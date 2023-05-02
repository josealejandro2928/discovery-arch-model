package org.discover.arch.model;

import java.io.File;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

public class ResourcesProviderAnalyzer {
    private List<String> fileResourcePaths;
    private final List<String> externalResourcePaths;
    Config configObj;
    Map<String, ExternalConnector> externalConnectorMap = new HashMap<>();


    public ResourcesProviderAnalyzer(Config configObj) throws Exception {
        this.configObj = configObj;
        this.externalConnectorMap.put("github", new GithubConnector(this.configObj));
        this.fileResourcePaths = this.configObj.getArchivesForSearching();
        this.externalResourcePaths = this.configObj.getExternalResources();
        this.validateExternalPaths();
        boolean validationFiles = this.validateFilePaths();
        if (!validationFiles)
            throw new Exception("THERE IS NOT VALID PATH IN THE LIST PROVIDED, PLEASE VERIFY YOUR CONFIGURATION");
        this.configObj.persistCacheInDisk();
        this.configObj.saveConfig();
    }

    private boolean validateFilePaths() {
        List<String> validFilesPaths = new ArrayList<>();
        for (String p : this.fileResourcePaths) {
            File file = new File(p);
            if (file.exists())
                validFilesPaths.add(p);
        }
        this.fileResourcePaths = validFilesPaths;
        return true;
    }

    private void validateExternalPaths() {
        System.out.println("***********************************************************************************************");
        System.out.println("ANALYZING THE EXTERNAL PATHS THIS MAY TAKE A BIT LONGER, DEPENDS OF THE CACHE TIME CONFIGURATION");
        int delayCache = this.configObj.timeCacheForPollingFromExternalResources;
        long startTime = System.nanoTime();

        for (String p : this.externalResourcePaths) {
            Map.Entry<String, ExternalConnector> entrySet = this.externalConnectorMap.entrySet()
                    .stream().filter((Map.Entry<String, ExternalConnector> entry) -> entry.getValue().isValidPath(p)).findAny().orElse(null);
            if (entrySet == null)
                System.out.println("Not found connector to external result : " + p);
            else {
                ExternalConnector connector = entrySet.getValue();
                String directoryPath = Paths.get(this.configObj.getRootPath(), entrySet.getKey()).toAbsolutePath().toString();
                // Layer of validation that checks for the  config expiration times
                if (this.configObj.isInCache(p, delayCache)) {
                    continue;
                }
                try {
                    connector.loadResource(p, directoryPath);
                } catch (Exception e) {
                    System.out.println("Error analysing the external resource: " + p);
                    e.printStackTrace();
                }
            }

        }
        long endTime = System.nanoTime();
        double elapsedTime = (double) (endTime - startTime) / 1000000000;
        System.out.println("\033[0;32m" + "ELAPSED TIME: " + new DecimalFormat("0.000").format(elapsedTime) + "s" + "\033[0m");
        System.out.println("EXTERNAL PATHS SUCCESSFULLY SYNCHRONIZED TO THE PROJECT");
        System.out.println("***********************************************************************************************");
    }

    public List<String> getExternalResourcePaths() {
        return externalResourcePaths;
    }

    public List<String> getFileResourcePaths() {
        return fileResourcePaths;
    }
}
