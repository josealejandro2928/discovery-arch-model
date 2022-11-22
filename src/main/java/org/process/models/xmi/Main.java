package org.process.models.xmi;

import java.nio.file.Paths;

public class Main {
    static String configPath = "./config.json";

    public static void main(String[] args) {
        System.out.println("Running ecore processing");
        try {
            Config config = Config.getInstance(configPath);
            EcoreStandAlone ecoreStandAlone = EcoreStandAlone.getInstance();
            EcoreModelHandler ecoreModelHandler = EcoreModelHandler.getInstance();
            EolRunner eolRunner = EolRunner.getInstance();
            if (config == null)
                throw new Exception("The config object cannot be null");
            ecoreStandAlone.init(config.ecoreRequiredFilesFolder);
            ecoreModelHandler.setRootPathFolder(Paths.get(config.rootPath, config.outputFolderName, "xmi").toString());
            ecoreModelHandler.discoverModelFromPath();
            ecoreModelHandler.processModels(ecoreStandAlone, eolRunner);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
//        try {
//            EolRunner eolRunner = EolRunner.getInstance();
//            System.out.println(eolRunner.runExample());
//        } catch (Exception e) {
//            System.out.println("Error: " + e.getMessage());
//            e.printStackTrace();
//        }
    }
}
