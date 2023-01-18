package org.osate.standalone.model;

import org.discover.arch.model.Config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CrossReferenceResolver {
    static int UP_LEVELS = 2;
    static final String FOUND_FILES = "foundFiles";
    static final String PARENT_NAME = "parentName";

    private static final Config configObj = Config.getInstance("/mnt/DATA/00-GSSI/00-WORK/EXAMPLE_ROOT_DIRECTORY_MODELS/config.json");

    static Map<String, Object> resolve(String path) {
        List<String> foundFiles = new ArrayList<>(Arrays.asList(path));
        File file = new File(path);
        String parentDirectory = null;
        String parentName = null;
        Map<String, Object> dataOutput = new HashMap<>();
        String extension = getExtension(path);

        int levelsUp = 0;
        while (!isReachingTopPathOrigin(file.getParent()) && levelsUp < UP_LEVELS && file.getParent() != null) {
            file = new File(file.getParent());
            levelsUp++;
        }
        parentDirectory = file.getPath();
        parentName = file.getName();
        Set<String> visitedFiles = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(parentDirectory);
        visitedFiles.add(path);
        while (queue.size() > 0) {
            String pathFileOrArchive = queue.poll();
            file = new File(pathFileOrArchive);
            if (!visitedFiles.contains(pathFileOrArchive)) {
                if (file.isDirectory()) {
                    try {
                        for (File childFile : Objects.requireNonNull(file.listFiles())) {
                            queue.add(childFile.getPath());
                        }
                    } catch (Exception e) {
                        System.err
                                .println("ERROR CrossReferenceResolver:: reading the files of the directory: " + file);
                    }
                } else {
                    String filePath = file.getPath();
                    String ext = getExtension(filePath);
                    if (ext.equals(extension)) {
                        foundFiles.add(filePath);
                    }
                }

            }
        }
        /////////// OUTPUT //////////////////
        dataOutput.put(PARENT_NAME, parentName);
        dataOutput.put(FOUND_FILES, foundFiles);
        return dataOutput;
    }

    static Map<String, Object> resolveDown(String path) {
        Map<String, Object> dataOutput = new HashMap<>();
        List<String> foundFiles = new ArrayList<>();
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
            }
        }
        /////////// OUTPUT //////////////////
        dataOutput.put(FOUND_FILES, foundFiles);
        return dataOutput;
    }

    static String getExtension(String path) {
        if (!path.contains(".")) {
            return "txt";
        }
        String[] chunksFileString = path.split("\\.");
        return chunksFileString[chunksFileString.length - 1];
    }

    static boolean isReachingTopPathOrigin(String path) {
        List<Path> topPaths = configObj.getArchivesForSearching().stream().map((String x) -> Paths.get(x).toAbsolutePath()).toList();
        boolean res = topPaths.contains(Paths.get(path));
        return res;
    }
}
