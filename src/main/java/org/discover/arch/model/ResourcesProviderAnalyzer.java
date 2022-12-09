package org.discover.arch.model;

import com.google.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResourcesProviderAnalyzer {
    private List<String> fileResourcePaths;
    private List<String> externalResourcePaths;
    Config configObj = Config.getInstance(null);

    ResourcesProviderAnalyzer(List<String> filePaths, List<String> externalPaths) throws Exception {
        this.fileResourcePaths = filePaths;
        this.externalResourcePaths = externalPaths;
        boolean validationFiles = this.validateFilePaths();
        if (!validationFiles)
            throw new Exception("There is not valid path in the list provided, please verify your configuration");
    }

    ResourcesProviderAnalyzer() throws Exception {
        this.fileResourcePaths = this.configObj.getArchivesForSearching();
        this.externalResourcePaths = this.configObj.getExternalResources();
        boolean validationFiles = this.validateFilePaths();
        if (!validationFiles)
            throw new Exception("THERE IS NOT VALID PATH IN THE LIST PROVIDED, PLEASE VERIFY YOUR CONFIGURATION");
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
        return true;
    }

    public List<String> getExternalResourcePaths() {
        return externalResourcePaths;
    }

    public List<String> getFileResourcePaths() {
        return fileResourcePaths;
    }
}
