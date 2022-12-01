package org.discover.arch.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Config {
    static private Config INSTANCE = null;
    private String rootPath;
    private String outputFolderName;
    private String ecoreRequiredFilesFolder;
    private List<String> archivesForSearching;
    private List<String> extensionsForSearching;
    private List<String> externalResources;
    private String configPath;
    public Map<String, Object> configObj;
    private List<Map<String, Object>> conversionLogs;
    private List<String> filesFound;
    private Map<String, Object> reports;

    private Config(Map<String, Object> data) throws Exception {
        if (data == null)
            throw new Exception("In order to load a config a Map should be passed into the constructor");
        this.rootPath = (String) data.get("rootPath");
        this.outputFolderName = (String) data.get("outputFolderName");
        this.archivesForSearching = (List<String>) data.get("archivesForSearching");
        this.extensionsForSearching = (List<String>) data.get("extensionsForSearching");
        this.externalResources = (List<String>) data.get("externalResources");
        this.ecoreRequiredFilesFolder = (String) data.get("ecoreRequiredFilesFolder");
        this.conversionLogs = new ArrayList<>();
        this.filesFound = new ArrayList<>();
        this.reports = new HashMap<>();
        validate();
        this.configObj = data;
    }

    private void validate() throws Exception {
        File file = new File(rootPath);
        if (!file.exists())
            throw new Exception("The rootPath: " + rootPath + " does not exists");
    }

    public static Config getInstance(String configPath) {
        if (INSTANCE == null) {
            System.out.println("Loading config .....");
            try {
                JSONObject data = loadConfig(configPath);
                INSTANCE = new Config(data.toMap());
                INSTANCE.configPath = configPath;
                System.out.println("Config parameters: " + INSTANCE.configObj);
                return INSTANCE;
            } catch (Exception e) {
                System.err.println("ERROR LOADING THE CONFIG FILE: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {
            return INSTANCE;
        }
    }

    static private JSONObject loadConfig(String configPath) throws Exception {
        File file = new File(configPath);
        if (!file.exists())
            throw new Exception("A config.json file path must be provided");
        String read = String.join("\n", Files.readAllLines(file.toPath()));
        JSONObject data = new JSONObject(read);
        if (!data.has("rootPath"))
            throw new Exception("The config.json file should have key 'rootPath'");
        if (!data.has("archivesForSearching"))
            throw new Exception("The config.json file should have key 'archivesForSearching'");
        if (!data.has("outputFolderName"))
            data.put("outputFolderName", "output-processing");
        if (!data.has("extensionsForSearching"))
            data.put("extensionsForSearching", new String[]{"aadl"});

        if (!data.has("ecoreRequiredFilesFolder"))
            throw new Exception("The config.json file should have key 'ecoreRequiredFilesFolder'");

        if (!data.has("externalResources"))
            data.put("externalResources", new String[]{});
        return data;
    }

    public String getEcoreRequiredFilesFolder() {
        return ecoreRequiredFilesFolder;
    }

    public String getOutputFolderName() {
        return outputFolderName;
    }

    public List<String> getArchivesForSearching() {
        return archivesForSearching;
    }

    public List<String> getExtensionsForSearching() {
        return extensionsForSearching;
    }

    public List<String> getExternalResources() {
        return externalResources;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void addMoreArchivesForSearching(Object data) {
        if (data instanceof Iterable) {
            for (String el : (Iterable<String>) data) {
                this.archivesForSearching.add(el);
            }
        } else {
            this.archivesForSearching.add((String) data);
        }
        this.saveConfig();
    }

    public boolean saveConfig() {
        return true;
    }

    public void createFolderOutput() throws Exception {
        File file = Paths.get(this.rootPath, this.outputFolderName).toFile();
        file.mkdir();
        for (File childFile : Objects.requireNonNull(file.listFiles())) {
            deleteDirectory(childFile.toPath());
        }
        for (String ext : this.extensionsForSearching) {
            new File(Paths.get(file.getPath(), ext).toString()).mkdir();
        }
        new File(Paths.get(file.getPath(), "xmi").toString()).mkdir();
    }

    static void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public List<Map<String, Object>> getConversionLogs() {
        return conversionLogs;
    }

    public Map<String, Object> getReports() {
        return reports;
    }

    public List<String> getFilesFound() {
        return filesFound;
    }

    public void loadJSONFilesGeneratedByDiscoveringPhase() {
        Path outputPathFolder = Paths.get(this.rootPath, this.outputFolderName).toAbsolutePath();
        File conversionLogsFile = outputPathFolder.resolve("conversion-logs.json").toFile();
        File filesFoundFile = outputPathFolder.resolve("files-found.txt").toFile();
        File reportsLogsFile = outputPathFolder.resolve("reports-logs.json").toFile();
        this.conversionLogs = new ArrayList<>();
        this.filesFound = new ArrayList<>();
        this.reports = new HashMap<>();
        try {
            if (conversionLogsFile.exists()) {
                String read = String.join("\n", Files.readAllLines(conversionLogsFile.toPath()));
                for (Object object : new JSONArray(read)) {
                    JSONObject jsonObject = (JSONObject) object;
                    this.conversionLogs.add(jsonObject.toMap());
                }
            }
        } catch (Exception e) {
            System.out.println("Error in loading the .json files generated by the discovering phase: -> loading \"conversion-logs.json\"");
        }
        try {
            if (reportsLogsFile.exists()) {
                String read = String.join("\n", Files.readAllLines(reportsLogsFile.toPath()));
                this.reports = new JSONObject(read).toMap();
            }
        } catch (Exception e) {
            System.out.println("Error in loading the .json files generated by the discovering phase: -> loading \"reports-logs.json\"");
        }
        try {
            if (filesFoundFile.exists()) {
                this.filesFound = Files.readAllLines(filesFoundFile.toPath());
            }
        } catch (Exception e) {
            System.out.println("Error in loading the .json files generated by the discovering phase: -> loading \"files-found.txt\"");
        }

    }
}