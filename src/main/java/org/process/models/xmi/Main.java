package org.process.models.xmi;

import org.discover.arch.model.Config;

import java.nio.file.Paths;

public class Main {
    static String configPath = "./config.json";

    public static void main(String[] args) {
        System.out.println("Running ecore processing");
        if (args.length > 0) {
            configPath = args[0];
        }
        try {
            Config config = Config.getInstance(configPath);
            if (config == null)
                throw new Exception("The config object cannot be null");
            EcoreStandAlone ecoreStandAlone = EcoreStandAlone.getInstance();
            EcoreModelHandler ecoreModelHandler = EcoreModelHandler.getInstance();
            EolRunner eolRunner = EolRunner.getInstance();
            ecoreStandAlone.init();
            ecoreModelHandler.discoverModelFromPath();
            config.loadJSONFilesGeneratedByDiscoveringPhase();
            ecoreModelHandler.processModels(eolRunner);
            ecoreModelHandler.generateCSVFileFromProcessedModels("results");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
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
