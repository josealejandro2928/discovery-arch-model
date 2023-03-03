package org.server.app.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.morphia.Datastore;
import org.discover.arch.model.ArchModelConverter;
import org.discover.arch.model.Config;
import org.discover.arch.model.ResourcesProviderAnalyzer;
import org.discover.arch.model.SearchFileTraversal;
import org.eclipse.emf.ecore.resource.Resource;
import org.process.models.xmi.EcoreModelHandler;
import org.process.models.xmi.EolRunner;
import org.process.models.xmi.EcoreStandAlone;
import org.process.models.xmi.JavaQueryAADLModelInst;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.MongoDbConnection;
import org.server.app.data.UserModel;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.ServerError;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class ModelRepositoryController {
    private static final CustomMapMapper objectMapper = CustomMapMapper.getInstance();

    public HttpHandler listModelsHandler = exchange -> {
        String method = exchange.getRequestMethod();
        if (method.equals("GET")) {
            listModelRepo(exchange);
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }
    };
    public HttpHandler discoverModelHandler = exchange -> {
        String method = exchange.getRequestMethod();
        if (method.equals("GET")) {
            try {
                this.discoverAndConvertModels(exchange);
            } catch (Exception e) {
                throw new ServerError(500, e.getMessage());
            }
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }
    };
    public HttpHandler analyseModelsHandler = exchange -> {
        String method = exchange.getRequestMethod();
        if (method.equals("GET")) {
            try {
                this.analyseModels(exchange);
            } catch (Exception e) {
                throw new ServerError(500, e.getMessage());
            }
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }
    };

    void listModelRepo(HttpExchange exchange) throws IOException {
        MongoDbConnection mongoDbConnection = MongoDbConnection.getInstance();
        Datastore datastore = mongoDbConnection.datastore;
        UserModel loggedUser = (UserModel) exchange.getAttribute("loggedUser");
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        if (configUser == null) throw new ServerError(400, "You dont have any space here");
        File fileLocalModels = new File(Paths.get(configUser.getRootPath(), "local_models").toAbsolutePath().toString());
        File fileGitHubModels = new File(Paths.get(configUser.getRootPath(), "github").toAbsolutePath().toString());


        Map<String, Object> dataOutput = new HashMap<>();
        var localModels = Arrays.stream(Objects.requireNonNull(fileLocalModels.listFiles())).map((File f) -> {
            Map<String, Object> data = new HashMap<>();
            data.put("name", f.getName());
            data.put("path", f.getAbsolutePath());
            data.put("isDir", f.isDirectory());
            if (f.isDirectory())
                data.put("items", Objects.requireNonNull(f.listFiles()).length);
            return data;
        }).toList();

        var githubModels = Arrays.stream(Objects.requireNonNull(fileGitHubModels.listFiles())).map((File f) -> {
            Map<String, Object> data = new HashMap<>();
            data.put("name", f.getName());
            data.put("path", f.getAbsolutePath());
            data.put("isDir", f.isDirectory());
            if (f.isDirectory())
                data.put("items", Objects.requireNonNull(f.listFiles()).length);
            return data;
        }).toList();

        dataOutput.put("message", "Ok");
        dataOutput.put("localModelsRepo", localModels);
        dataOutput.put("githubModelsRepo", githubModels);


        File fileAadlExtractedModels = new File(Paths.get(configUser.getRootPath(), configUser.getOutputFolderName(), "aadl")
                .toAbsolutePath().toString());
        File fileXmiConvertedModels = new File(Paths.get(configUser.getRootPath(), configUser.getOutputFolderName(), "xmi")
                .toAbsolutePath().toString());

        if (fileAadlExtractedModels.exists() && fileXmiConvertedModels.exists()) {
            var aadlExtractedModels = Arrays.stream(Objects.requireNonNull(fileAadlExtractedModels.listFiles())).map((File f) -> {
                Map<String, Object> data = new HashMap<>();
                data.put("name", f.getName());
                data.put("path", f.getAbsolutePath());
                data.put("isDir", f.isDirectory());
                return data;
            }).toList();

            var xmiConvertedModels = Arrays.stream(Objects.requireNonNull(fileXmiConvertedModels.listFiles())).map((File f) -> {
                Map<String, Object> data = new HashMap<>();
                data.put("name", f.getName());
                data.put("path", f.getAbsolutePath());
                data.put("isDir", f.isDirectory());
                return data;
            }).toList();
            dataOutput.put("aadlExtractedModels", aadlExtractedModels);
            dataOutput.put("xmiConvertedModels", xmiConvertedModels);
        }


        String response = objectMapper.writeValueAsString(dataOutput);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    void discoverAndConvertModels(HttpExchange exchange) throws Exception {
        Datastore datastore = MongoDbConnection.getInstance().datastore;
        UserModel loggedUser = (UserModel) exchange.getAttribute("loggedUser");
        Map<String, Object> dataOutput = new HashMap<>();
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        String configJsonPath = configUser.getPathToConfigJson();
        /////////Listening System.out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream newPrintStream = new PrintStream(baos);
        PrintStream oldPrintStream = System.out;
        System.setOut(newPrintStream);
        /////////Listening System.err
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        PrintStream newPrintStreamError = new PrintStream(baosErr);
        PrintStream oldPrintStreamError = System.err;
        System.setErr(newPrintStreamError);

        Config config = null;
        try {
            config = new Config(configJsonPath);
        } catch (Exception e) {
            throw new ServerError(500, "ERROR LOADING THE CONFIG FILE: ");
        }
        System.out.println("CREATING THE OUTPUT STRUCTURE FOLDER");
        config.createFolderOutput();
        System.out.println("*********************STAGE 1********************");
        System.out.println("ANALYZING THE RESOURCES PATHS");
        ResourcesProviderAnalyzer resourcesProviderAnalyzer = new ResourcesProviderAnalyzer(config);
        List<String> rootPathToAnalyze = resourcesProviderAnalyzer.getFileResourcePaths();
        SearchFileTraversal fileDiscover = new SearchFileTraversal(config).setSearchPaths(rootPathToAnalyze);
        ArchModelConverter archModelConverter = new ArchModelConverter(config);
        fileDiscover.analyseModels(archModelConverter);

        dataOutput.put("message", "Ok");
        String loggedData = this.filterLogs(baos.toString());
        String loggedErrorsData = this.filterLogs(baosErr.toString());
        dataOutput.put("dataLogs", loggedData.split("\n"));
        dataOutput.put("errorLogs", loggedErrorsData.split("\n"));

        /// Restoring the prev System print Stream ///
        System.out.flush();
        System.setOut(oldPrintStream);
        System.err.flush();
        System.setErr(oldPrintStreamError);

        String response = objectMapper.writeValueAsString(dataOutput);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

    void analyseModels(HttpExchange exchange) throws Exception {
        Datastore datastore = MongoDbConnection.getInstance().datastore;
        UserModel loggedUser = (UserModel) exchange.getAttribute("loggedUser");
        Map<String, Object> dataOutput = new HashMap<>();
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        String configJsonPath = configUser.getPathToConfigJson();

        /////////Listening System.out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream newPrintStream = new PrintStream(baos);
        PrintStream oldPrintStream = System.out;
        System.setOut(newPrintStream);
        /////////Listening System.err
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        PrintStream newPrintStreamError = new PrintStream(baosErr);
        PrintStream oldPrintStreamError = System.err;
        System.setErr(newPrintStreamError);

        Config config = null;
        try {
            config = new Config(configJsonPath);
        } catch (Exception e) {
            throw new ServerError(500, "ERROR LOADING THE CONFIG FILE: ");
        }

        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().clear();
        EcoreModelHandler ecoreModelHandler = new EcoreModelHandler(config);
        EcoreStandAlone ecoreStandAlone = EcoreStandAlone.getInstance();
        EolRunner eolRunner = EolRunner.getInstance();
        JavaQueryAADLModelInst javaQueryAADLModelInst = JavaQueryAADLModelInst.getInstance();
        ecoreStandAlone.init();
        ecoreModelHandler.discoverModelFromPath();
        config.loadJSONFilesGeneratedByDiscoveringPhase();
        ecoreModelHandler.processModels(eolRunner, javaQueryAADLModelInst);
        ecoreModelHandler.generateCSVFileFromProcessedModels("results");

        dataOutput.put("message", "Ok");
        String loggedData = this.filterLogs(baos.toString());
        String loggedErrorsData = this.filterLogs(baosErr.toString());
        dataOutput.put("dataLogs", loggedData.split("\n"));
        dataOutput.put("errorLogs", loggedErrorsData.split("\n"));

        //// Restoring the prev System print Stream ///
        System.out.flush();
        System.setOut(oldPrintStream);
        System.err.flush();
        System.setErr(oldPrintStreamError);

        String response = objectMapper.writeValueAsString(dataOutput);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

    String filterLogs(String data) {
        String loggedData = data.replaceAll("\\033\\[0;33m", "");
        loggedData = loggedData.replaceAll("\\u001B\\[0m", "");
        loggedData = loggedData.replaceAll("\\u001B\\[0m", "");
        loggedData = loggedData.replaceAll("\\u001B\\[0;32m", "");
        return loggedData;
    }
}
