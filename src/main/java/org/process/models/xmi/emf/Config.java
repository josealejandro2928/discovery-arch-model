package org.process.models.xmi.emf;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class Config {
    static private Config INSTANCE = null;
    public Map configObj = null;

    private Config() {
    }

    static Config getInstance(String configPath) {
        if (INSTANCE == null) {
            System.out.println("Loading config .....");
            try {
                JSONObject data = loadConfig(configPath);
                INSTANCE = new Config();
                INSTANCE.configObj = data.toMap();
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

    static private JSONObject loadConfig(String configPath) throws Exception {
        File file = new File(configPath);
        if (!file.exists())
            throw new Exception("A config.json file path must be provided");

        String read = String.join("\n", Files.readAllLines(file.toPath()));
        JSONObject data = new JSONObject(read);
        if (!data.has("ecoreRequiredFilesFolder"))
            throw new Exception("The config.json file should have key 'ecoreRequiredFilesFolder'");

        return data;
    }
}
