package org.discover.arch.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDateTime;

public class Config {
    static private Config INSTANCE = null;
    static String configPath;
    private String rootPath;
    private String outputFolderName;
    private String ecoreRequiredFilesFolder;
    private List<String> archivesForSearching;
    private List<String> extensionsForSearching;
    private List<String> externalResources;
    private List<String> avoidFileNames;

    public Map<String, Object> configObj;
    private List<Map<String, Object>> conversionLogs;
    private List<String> filesFound;
    private Map<String, Object> reports;
    private Map<String, Date> cache;
    int timeCacheForDiscoveringSearchOverFilesInSeconds;
    int timeCacheForPollingFromExternalResources;


    private Config(Map<String, Object> data) throws Exception {
        if (data == null)
            throw new Exception("In order to load a config a Map should be passed into the constructor");
        this.rootPath = (String) data.get("rootPath");
        this.outputFolderName = (String) data.get("outputFolderName");
        this.archivesForSearching = (List<String>) data.get("archivesForSearching");
        this.extensionsForSearching = (List<String>) data.get("extensionsForSearching");
        this.externalResources = (List<String>) data.get("externalResources");
        this.avoidFileNames = (List<String>) data.get("avoidFileNames");
        this.ecoreRequiredFilesFolder = (String) data.get("ecoreRequiredFilesFolder");
        this.timeCacheForDiscoveringSearchOverFilesInSeconds = (Integer) data.get("timeCacheForDiscoveringSearchOverFilesInSeconds");
        this.timeCacheForPollingFromExternalResources = (Integer) data.get("timeCacheForPollingFromExternalResources");
        this.conversionLogs = new ArrayList<>();
        this.filesFound = new ArrayList<>();
        this.reports = new HashMap<>();
        validate();
        this.loadCache();
        this.configObj = data;
    }

    private void validate() throws Exception {
        File file = new File(rootPath);
        if (!file.exists())
            throw new Exception("The rootPath: " + rootPath + " does not exists");
    }

    public static Config getInstance(String configPath) {
        if (INSTANCE == null) {
            System.out.println("Loading config once .....");
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

    static private JSONObject loadConfig(String configJsonPath) throws Exception {
        if (configJsonPath != null)
            configPath = configJsonPath;
        File file = new File(configPath);
        if (!file.exists())
            throw new Exception("A config.json file path must be provided");
        if (!file.isFile())
            throw new Exception("A config.json must be a file");

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

        if (!data.has("avoidFileNames"))
            data.put("avoidFileNames", new String[]{});

        if (!data.has("timeCacheForDiscoveringSearchOverFilesInSeconds"))
            data.put("timeCacheForDiscoveringSearchOverFilesInSeconds", 60);

        if (!data.has("timeCacheForPollingFromExternalResources"))
            data.put("timeCacheForPollingFromExternalResources", 60 * 5);
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
                if (!this.archivesForSearching.contains(el))
                    this.archivesForSearching.add(el);
            }
        } else {
            if (!this.archivesForSearching.contains((String) data))
                this.archivesForSearching.add((String) data);

        }
        this.configObj.put("archivesForSearching", this.archivesForSearching);
        this.saveConfig();
    }

    public boolean saveConfig() {
        try {
            String jsonStr = new JSONObject(this.configObj).toString(2);
            FileWriter fw = new FileWriter(Paths.get(configPath).toString());
            fw.write(jsonStr);
            fw.close();
            return true;
        } catch (Exception e) {
            System.out.println("ERROR SAVING THE CONFIG JSON");
            e.printStackTrace();
            return false;
        }
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
        return this.conversionLogs;
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

    ///////////////////////CACHE STUFF////////////////////////////////////////////////////////////////////////////////
    public void loadCache() {
        Path cachePath = Paths.get(this.rootPath, ".cache.txt").toAbsolutePath();
        File cacheFile = cachePath.toFile();
        this.cache = new HashMap<>();
        SimpleDateFormat formatterDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        System.out.println("LOADING CACHE FROM DISK");
        if (cacheFile.exists()) {
            try {
                for (String line : Files.readAllLines(cachePath)) {
                    String[] data = line.split(",");
                    this.cache.put(data[0].trim(), formatterDate.parse(data[1].trim()));
                }
            } catch (Exception error) {
                System.out.println("ERROR READING THE CACHE");
                error.printStackTrace();
            }
        } else {
            try {
                Files.createFile(cachePath);
            } catch (Exception error) {
                System.out.println("ERROR CREATING THE CACHE");
                error.printStackTrace();
            }

        }
    }

    public void putInCache(String x) {
        this.cache.put(x, new Date());
    }

    public boolean isInCache(String x, int delay) {
        boolean isStoredInCache = this.cache.containsKey(x);
        if (!isStoredInCache) return false;
        int secondsToInvalidate = delay;
        Date dateOfEntry = this.cache.get(x);
        Date now = new Date();
        long diff = now.getTime() - dateOfEntry.getTime();
        long diffSeconds = diff / 1000;
        if (diffSeconds > secondsToInvalidate) {
            this.cache.remove(x);
            return false;
        }
        return true;
    }

    public Map<String, Date> getCache() {
        return cache;
    }

    public void persistCacheInDisk() {
        Path cachePath = Paths.get(this.rootPath, ".cache.txt").toAbsolutePath();
        File cacheFile = cachePath.toFile();
        SimpleDateFormat formatterDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        if (!cacheFile.exists()) {
            try {
                Files.createFile(cachePath);
            } catch (Exception error) {
                System.out.println("ERROR CREATING THE CACHE");
                error.printStackTrace();
            }
        }
        List<String> data = new ArrayList<>();
        for (Map.Entry<String, Date> entry : this.cache.entrySet()) {
            String key = entry.getKey();
            Date date = entry.getValue();
            data.add(key + "," + formatterDate.format(date));
        }
        try {
            Files.write(cachePath, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<String> getAvoidFileNames() {
        return avoidFileNames;
    }
}