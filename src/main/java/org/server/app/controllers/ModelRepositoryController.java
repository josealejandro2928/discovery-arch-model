package org.server.app.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.morphia.Datastore;
import org.discover.arch.model.ArchModelConverter;
import org.discover.arch.model.Config;
import org.discover.arch.model.ResourcesProviderAnalyzer;
import org.discover.arch.model.SearchFileTraversal;
import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.util.Aadl2ResourceFactoryImpl;
import org.process.models.xmi.EcoreModelHandler;
import org.process.models.xmi.EolRunner;
import org.process.models.xmi.EcoreStandAlone;
import org.process.models.xmi.JavaQueryAADLModelInst;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.MongoDbConnection;
import org.server.app.data.UserModel;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.FormDataHandler;
import org.server.app.utils.ServerError;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModelRepositoryController {
    private static final CustomMapMapper objectMapper = CustomMapMapper.getInstance();

    public HttpHandler modelsHandler = exchange -> {
        String method = exchange.getRequestMethod();
        if (method.equals("GET")) {
            listModelRepo(exchange);
        } else if (method.equals("POST")) {
            uploadModelToRepo(exchange);
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
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("aaxl2", new Aadl2ResourceFactoryImpl());
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
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("aaxl2");

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

    void uploadModelToRepo(HttpExchange exchange) throws IOException {
        UserModel loggedUser = (UserModel) exchange.getAttribute("loggedUser");
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        ArrayList<FormDataHandler.MultiPart> files = (ArrayList<FormDataHandler.MultiPart>) exchange.getAttribute("files");
//        System.out.println(files);
        if (files == null || files.size() == 0) throw new ServerError(400, "You have to upload at least one file");
        if (files.size() > 3) throw new ServerError(405, "Too many models at the same time");
        List<String> errors = new ArrayList<>();
        for (FormDataHandler.MultiPart file : files) {
            if (!file.contentType.equals("application/zip")) {
                errors.add(String.format("File with name: %s is not a zip file", file.name));
            }
        }
        if (errors.size() > 0)
            throw new ServerError(405, "There is an error on the request", Collections.singletonList(errors));

        Map<String, Object> dataOutput = new HashMap<>();
        var savedFiles = saveNewLocalModels(configUser, files);
        dataOutput.put("message", "Ok");
        dataOutput.put("savedFiles", savedFiles);
        String response = objectMapper.writeValueAsString(dataOutput);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();


    }

    static List<Path> saveNewLocalModels(ConfigUserModel configUser, List<FormDataHandler.MultiPart> files) throws IOException {
        Path localModelsPath = Paths.get(configUser.getRootPath(), "local_models");
        List<Path> filesSaved = new ArrayList<>();

        for (FormDataHandler.MultiPart file : files) {
            // save the zip file
            Path filePath = localModelsPath.resolve(file.name);
            Files.write(Path.of(filePath + ".zip"), file.bytes);
            filesSaved.add(filePath);
            unzip(filePath + ".zip", filePath.toString());
        }
        filesSaved.forEach((Path file) -> {
            try {
                Config.deleteDirectory(Path.of(file + ".zip"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return filesSaved;
    }

    private static void unzip(String zipFilePath, String destDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(new File(destDir), zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
