package org.process.models.xmi;

import org.discover.arch.model.Config;

public class Main {
    static String configPath = "./config.json";

    public static void main(String[] args) {
        System.out.println("Running ecore processing");
        if (args.length > 0) {
            configPath = args[0];
        }
        System.out.println(configPath);
        try {
            Config config = Config.getInstance(configPath);
            if (config == null)
                throw new Exception("The config object cannot be null");
//            EcoreStandAlone ecoreStandAlone = EcoreStandAlone.getInstance();
            EcoreModelHandler ecoreModelHandler = EcoreModelHandler.getInstance();
            EolRunner eolRunner = EolRunner.getInstance();
            JavaQueryAADLModelInst javaQueryAADLModelInst = JavaQueryAADLModelInst.getInstance();
//            ecoreStandAlone.init();
            ecoreModelHandler.discoverModelFromPath();
            config.loadJSONFilesGeneratedByDiscoveringPhase();
            ecoreModelHandler.processModels(eolRunner,javaQueryAADLModelInst);
//            ecoreModelHandler.processModels(javaQueryAADLModelInst);
            ecoreModelHandler.generateCSVFileFromProcessedModels("results");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
