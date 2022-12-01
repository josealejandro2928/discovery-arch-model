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
import java.util.Objects;

public class Main {
    static String configPath = "./config.json";

    public static void main(String[] args) {
        System.out.println("........LOADING CONFIG.....");
        Config config = null;
        try {
            config = Config.getInstance(configPath);
        } catch (Exception e) {
            System.err.println("ERROR LOADING THE CONFIG FILE: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        try {
            System.out.println("*********************STAGE 1********************");
            System.out.println("ANALYZING THE RESOURCES PATHS");
            ResourcesProviderAnalyzer resourcesProviderAnalyzer = new ResourcesProviderAnalyzer(Objects.requireNonNull(config));
            SearchFileTraversal fileDiscover = new SearchFileTraversal(config).setSearchPaths(resourcesProviderAnalyzer.getFileResourcePaths());
            ArchModelConverter archModelConverter = new ArchModelConverter(config);
            System.out.println("CREATING THE OUTPUT STRUCTURE FOLDER");
            config.createFolderOutput();

            List<String> filesFound = fileDiscover.searchForFiles(true, false);
            System.out.println("SCANNING RESULTS: " + fileDiscover.scanningResult);
            archModelConverter.setDataModelFiles(filesFound);
            archModelConverter.initProcessing();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

}