package org.discover.arch.model;

import com.google.inject.Inject;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public class ResourcesProviderAnalyzer {
    private List<String> fileResourcePaths;
    private List<String> externalResourcePaths;
    Config configObj = Config.getInstance(null);
    Map<String, ExternalConnector> externalConnectorMap =  new HashMap<>();


    ResourcesProviderAnalyzer() throws Exception {
        this.externalConnectorMap.put("github", new GithubConnector());
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
        if (validFilesPaths.size() == 0)
            return false;
        this.fileResourcePaths = validFilesPaths;
        return true;
    }

    private boolean validateExternalPaths() throws Exception {
        System.out.println("***********************************************************************************************");
        System.out.println("ANALYZING THE EXTERNAL PATHS THIS MAY TAKE A BIT LONGER, DEPENDS OF THE CACHE TIME CONFIGURATION");
        int delayCache = this.configObj.timeCacheForPollingFromExternalResources;

        for (String p : this.externalResourcePaths) {
            Map.Entry<String, ExternalConnector> entrySet = this.externalConnectorMap.entrySet()
                    .stream().filter((Map.Entry<String, ExternalConnector> entry) -> {
                        return entry.getValue().isValidPath(p);
                    }).findAny().orElse(null);
            if (entrySet == null)
                System.out.println("Not found connector to external result : " + p);
            else {
                ExternalConnector connector = entrySet.getValue();
                if (this.configObj.isInCache(p, delayCache))
                    continue;
                String directoryPath = Paths.get(this.configObj.getRootPath(), entrySet.getKey()).toAbsolutePath().toString();
                try {
                    connector.loadResource(p, directoryPath);
                } catch (Exception e) {
                    System.out.println("Error analysing the external resource: " + p);
                    e.printStackTrace();
                }
            }

        }
        System.out.println("EXTERNAL PATHS SUCCESSFULLY SYNCHRONIZED TO THE PROJECT");
        System.out.println("***********************************************************************************************");
        return true;
    }

    public List<String> getExternalResourcePaths() {
        return externalResourcePaths;
    }

    public List<String> getFileResourcePaths() {
        return fileResourcePaths;
    }
}
