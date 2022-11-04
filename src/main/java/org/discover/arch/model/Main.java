package org.discover.arch.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {
    static String configPath = "/mnt/DATA/00-GSSI/00-WORK/DISCOVERY-ARCH-MODELS/config.json";
    static String rootPath;
    static String[] archivesForSearching;
    static String[] extensionsForSearching;
    static String outputFolderName;

    public static void main(String[] args) {
        System.out.println("Loading config .....");
        try {
            JSONObject configObject = loadConfig();
            System.out.println("Config parameters: " + configObject);
        } catch (Exception e) {
            System.err.println("ERROR LOADING THE CONFIG FILE: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        String[] pathForSearching = new String[archivesForSearching.length];
        //////////Creating the SearchFolders///////////////////
        for (int i = 0; i < archivesForSearching.length; i++) {
            pathForSearching[i] = Paths.get(rootPath, archivesForSearching[i]).toString();
        }
        //////////////////////////////////////////////////////
        try {
            SearchFileTraversal fileDiscover = new SearchFileTraversal(rootPath, pathForSearching, extensionsForSearching)
                    .setFolderOutPutName(outputFolderName);
            ArchModelConverter archModelConverter = new ArchModelConverter(rootPath)
                    .setExtensions(Arrays.asList(extensionsForSearching))
                    .setFolderOutputName(outputFolderName);
            archModelConverter.createFolderOutput();

            System.out.println("*********************STAGE 1********************");
            List<String> filesFound = fileDiscover.searchForFiles(true, false);
            System.out.println("SCANNING RESULTS: " + fileDiscover.scanningResult);
            archModelConverter.setDataModelFiles(fileDiscover.dataFilesFound);
            archModelConverter.initProcessing();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static JSONObject loadConfig() throws Exception {
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
            data.put("outputFolderName", "output-discover");
        if (!data.has("extensionsForSearching"))
            data.put("extensionsForSearching", new String[]{"aadl"});

        rootPath = (String) data.get("rootPath");
        JSONArray tempData = (JSONArray) data.get("archivesForSearching");
        archivesForSearching = new String[tempData.length()];
        for (int i = 0; i < tempData.length(); i++) {
            archivesForSearching[i] = (String) tempData.get(i);
        }
        tempData = (JSONArray) data.get("extensionsForSearching");
        extensionsForSearching = new String[tempData.length()];
        for (int i = 0; i < tempData.length(); i++) {
            extensionsForSearching[i] = (String) tempData.get(i);
        }
        outputFolderName = (String) data.get("outputFolderName");
        return data;
    }
}