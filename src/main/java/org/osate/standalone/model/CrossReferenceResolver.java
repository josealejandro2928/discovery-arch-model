package org.osate.standalone.model;

import org.discover.arch.model.Config;

import java.io.File;
import java.util.*;

public class CrossReferenceResolver {
    static final String FOUND_FILES = "FOUND_FILES";
    static final String DOC_FILES = "DOC_FILES";
    static final List<String> docFilesExtensions = new ArrayList<>(Arrays.asList("md", "txt", "MD", "TXT"));

    static Map<String, Object> resolveDown(String path, Config configObj) {
        Map<String, Object> dataOutput = new HashMap<>();
        List<String> foundFiles = new ArrayList<>();
        List<String> docFiles = new ArrayList<>();
        List<String> extensionsToAnalyze = configObj.getExtensionsForSearching();
        Queue<String> queue = new LinkedList<>(Collections.singletonList(path));
        List<String> avoidFileNames = configObj.getAvoidFileNames();
        int delayCache = configObj.timeCacheForDiscoveringSearchOverFilesInSeconds;

        while (queue.size() > 0) {
            String pathFileOrArchive = queue.poll();
            File file = new File(pathFileOrArchive);
            if (configObj.isInCache(file.getPath(), delayCache) || avoidFileNames.contains(file.getName()))
                continue;
            if (file.isDirectory()) {
                try {
                    for (File childFile : Objects.requireNonNull(file.listFiles())) {
                        queue.add(childFile.getPath());
                    }
                } catch (Exception e) {
                    System.err.println("ERROR CrossReferenceResolver:: reading the files of the directory: " + file);
                }
            } else {
                String filePath = file.getPath();
                String ext = getExtension(filePath);
                if (extensionsToAnalyze.contains(ext)) {
                    foundFiles.add(filePath);
                }
                if (docFilesExtensions.contains(ext)) {
                    docFiles.add(filePath);
                }
            }
        }
        /////////// OUTPUT //////////////////
        dataOutput.put(FOUND_FILES, foundFiles);
        dataOutput.put(DOC_FILES, docFiles);
        return dataOutput;
    }

    static String getExtension(String path) {
        if (!path.contains(".")) {
            return "txt";
        }
        String[] chunksFileString = path.split("\\.");
        return chunksFileString[chunksFileString.length - 1];
    }
}
