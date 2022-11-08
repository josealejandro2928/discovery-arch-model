package org.process.models.xmi.emf;

public class Main {
    static String configPath = "/mnt/DATA/00-GSSI/00-WORK/DISCOVERY-ARCH-MODELS/config.json";

    public static void main(String[] args) {
        System.out.println("Running ecore processing");
        try {
            Config configObj = Config.getInstance(configPath);
            EcoreStandAlone ecoreStandAlone = EcoreStandAlone.getInstance();
            ecoreStandAlone.init();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
