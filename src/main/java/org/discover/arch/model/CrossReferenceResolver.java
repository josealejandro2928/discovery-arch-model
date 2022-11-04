package org.discover.arch.model;

import java.io.File;
import java.util.*;

public class CrossReferenceResolver {
    static int UP_LEVELS = 1;

    static Map<String, Object> resolve(String path, String extension) {
        List<String> foundFiles = new ArrayList<>();
        File file = new File(path);
        String parentDirectory = null;
        String parentName = null;
        int levelsUp = 0;
        Map<String, Object> dataOutput = new HashMap<>();

        while (levelsUp < UP_LEVELS && file.getParent() != null) {
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
                        System.err.println("ERROR CrossReferenceResolver:: reading the files of the directory: " + file);
                    }
                } else {
                    String filePath = file.getPath();
                    String ext = SearchFileTraversal.getExtension(filePath);
                    if (ext.equals(extension)) {
                        foundFiles.add(filePath);
                    }
                }

            }
        }
        /////////// OUTPUT //////////////////
        dataOutput.put("parentName", parentName);
        dataOutput.put("foundFiles", foundFiles);
        return dataOutput;
    }
}
