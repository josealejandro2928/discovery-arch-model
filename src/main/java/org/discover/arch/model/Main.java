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
            config = Config.getInstance(configPath);
            if (config == null)
                throw new Exception("THE provided path to the config.json file is incorrect or corrupted");
        } catch (Exception e) {
            System.err.println("ERROR LOADING THE CONFIG FILE: " + e.getMessage());
            return;
        }
        try {
            System.out.println("CREATING THE OUTPUT STRUCTURE FOLDER");
            config.createFolderOutput();

            System.out.println("*********************STAGE 1********************");
            System.out.println("ANALYZING THE RESOURCES PATHS");
            ResourcesProviderAnalyzer resourcesProviderAnalyzer = new ResourcesProviderAnalyzer();
            SearchFileTraversal fileDiscover = new SearchFileTraversal().setSearchPaths(resourcesProviderAnalyzer.getFileResourcePaths());
            ArchModelConverter archModelConverter = new ArchModelConverter();

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
