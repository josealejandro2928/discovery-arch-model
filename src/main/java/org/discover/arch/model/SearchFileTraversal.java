package org.discover.arch.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

public class SearchFileTraversal {
    String rootPath;
    List<String> extensions;
    List<String> searchPaths;
    String logfilePath;
    List<String> dataFilesFound = new ArrayList<>();
    List<String> dataFilesError = new ArrayList<>();
    HashMap<String, String> scanningResult = new HashMap<>();

    SearchFileTraversal(String rootPath, String[] searchPaths, String[] exts) throws Exception {
        File file = new File(rootPath);
        if (!file.exists())
            throw new Exception("The rootPath: " + rootPath + " does not exists");
        this.rootPath = rootPath;
        for (String p : searchPaths) {
            file = new File(p);
            if (!file.exists()) {
                throw new Exception("The path: " + p + " does not exists");
            }
        }
        this.searchPaths = Arrays.asList(searchPaths);
        this.extensions = Arrays.asList(exts);
    }

    public List<String> searchForFiles(boolean storeOnRootPath, boolean verbose) {
        /// Run a BFS for searching ///
        int totalFiles = 0;
        long startTime = System.nanoTime();
        Queue<String> queue = new LinkedList<>(this.searchPaths);
        System.out.println("SCANNING FILES ...");
        while (queue.size() > 0) {
            String pathFileOrArchive = queue.poll();
            totalFiles++;
            if (verbose) {
                System.out.println("Analysing file: " + pathFileOrArchive + " ...");
            }
            File file = new File(pathFileOrArchive);
            if (file.isDirectory()) {
                try {
                    for (File childFile : Objects.requireNonNull(file.listFiles())) {
                        queue.add(childFile.getPath());
                    }
                } catch (Exception e) {
                    System.err.println("ERROR reading the files of the directory: " + file);
                    this.dataFilesError.add(file.getPath());
                }
            } else {
                String filePath = file.getPath();
                String ext = getExtension(filePath);
                if (this.extensions.contains(ext)) {
                    this.dataFilesFound.add(filePath);
                }
            }
        }

        long endTime = System.nanoTime();
        System.out.println("SCANNING COMPLETED");
        if (storeOnRootPath) {
            try {
                this.logfilePath = Paths.get(this.rootPath, "files-founded.txt").toString();
                FileWriter myWriter = new FileWriter(this.logfilePath);
                myWriter.write(String.join("\n", this.dataFilesFound));
                if (this.dataFilesError.size() > 0) {
                    myWriter = new FileWriter(Paths.get(this.rootPath, "files-error.txt").toString());
                    myWriter.write(String.join("\n", this.dataFilesError));
                }
                myWriter.close();
                System.out.println("Successfully wrote to the log of founded files.");
            } catch (IOException e) {
                System.err.println("Error saving the logs of files");
                e.printStackTrace();
            }
        }

        double elapsedTime = (double) (endTime - startTime) / 1000000000;
        this.scanningResult.put("totalFiles", totalFiles + "");
        this.scanningResult.put("filesMatched", this.dataFilesFound.size() + "");
        this.scanningResult.put("filesWithErrors", this.dataFilesError.size() + "");
        this.scanningResult.put("elapsedTime", new DecimalFormat("0.000").format(elapsedTime) + "s");
        return this.dataFilesFound;
    }

    @Override
    public String toString() {
        return "rootPath: " + this.rootPath + "; " + "searchPaths: " +
                this.searchPaths + "; " + "exts: " + this.extensions;

    }

    static String getExtension(String path) {
        if (!path.contains(".")) return "txt";
        String[] chunksFileString = path.split("\\.");
        return chunksFileString[chunksFileString.length - 1];
    }
}
