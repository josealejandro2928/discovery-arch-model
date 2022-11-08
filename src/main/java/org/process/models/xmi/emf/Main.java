package org.process.models.xmi.emf;

import java.nio.file.Paths;

public class Main {
    static String configPath = "/mnt/DATA/00-GSSI/00-WORK/DISCOVERY-ARCH-MODELS/config.json";

    public static void main(String[] args) {
        System.out.println("Running ecore processing");
        try {
            Config config = Config.getInstance(configPath);
            EcoreStandAlone ecoreStandAlone = EcoreStandAlone.getInstance();
            EcoreModelHandler ecoreModelHandler = EcoreModelHandler.getInstance();
            if (config == null)
                throw new Exception("The config object cannot be null");
            ecoreStandAlone.init((String) config.configObj.get("ecoreRequiredFilesFolder"));
            ecoreModelHandler.setRootPathFolder(Paths.get((String) config.configObj.get("rootPath"),
                    (String) config.configObj.get("outputFolderName"), "xmi").toString());
            ecoreModelHandler.discoverModelFromPath();
            ecoreModelHandler.processModels(ecoreStandAlone);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
