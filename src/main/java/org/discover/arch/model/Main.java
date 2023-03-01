package org.discover.arch.model;

import java.util.List;

public class Main {
    static String configPath = "./config.json";

    public static void main(String[] args) {
        System.out.println("........LOADING CONFIG.....");
        if (args.length > 0) {
            configPath = args[0];
        }
        Config config = null;
        try {
            config = new Config(configPath);
        } catch (Exception e) {
            System.err.println("ERROR LOADING THE CONFIG FILE: " + e.getMessage());
            return;
        }
        try {
            System.out.println("CREATING THE OUTPUT STRUCTURE FOLDER");
            config.createFolderOutput();

            System.out.println("*********************STAGE 1********************");
            System.out.println("ANALYZING THE RESOURCES PATHS");
            ResourcesProviderAnalyzer resourcesProviderAnalyzer = new ResourcesProviderAnalyzer(config);
            List<String> rootPathToAnalyze = resourcesProviderAnalyzer.getFileResourcePaths();
            SearchFileTraversal fileDiscover = new SearchFileTraversal(config).setSearchPaths(rootPathToAnalyze);
            ArchModelConverter archModelConverter = new ArchModelConverter(config);
            fileDiscover.analyseModels(archModelConverter);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
