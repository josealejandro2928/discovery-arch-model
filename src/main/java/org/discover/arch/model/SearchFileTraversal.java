package org.discover.arch.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SearchFileTraversal {
    String rootPath;
    String folderOutputName;
    List<String> extensions;
    List<String> searchPaths;
    String logfilePath;
    Set<String> dataFilesFound = new HashSet<>();
    List<String> dataFilesError = new ArrayList<>();
    HashMap<String, String> scanningResult = new HashMap<>();
    private Config configObj = Config.getInstance(null);

    SearchFileTraversal(String rootPath, String[] searchPaths, String[] exts, String folderOutputName) {
        this.rootPath = rootPath;
        this.searchPaths = Arrays.asList(searchPaths);
        this.extensions = Arrays.asList(exts);
        this.folderOutputName = folderOutputName;
    }

    SearchFileTraversal() {
        this.rootPath = this.configObj.getRootPath();
        this.searchPaths = this.configObj.getArchivesForSearching();
        this.extensions = this.configObj.getExtensionsForSearching();
        this.folderOutputName = this.configObj.getOutputFolderName();
        try {
            Path previousFoundFiles = Paths.get(this.configObj.getRootPath(), ".files-found.txt").toAbsolutePath();
            this.dataFilesFound.addAll(Files.readAllLines(previousFoundFiles));
            this.dataFilesFound = this.dataFilesFound.stream().filter(x -> Paths.get(x).toFile().exists()).collect(Collectors.toSet());
            if (this.dataFilesFound.size() > 0)
                System.out.println("PATHS previously LOADED");
        } catch (Exception e) {
            System.out.println("NOT LOADED FILES DISCOVERED IN PREVIOUS SEARCHING");

        }
    }

    public SearchFileTraversal setFolderOutPutName(String outFolder) {
        this.folderOutputName = outFolder;
        return this;
    }

    public void analyseModels(ArchModelConverter archModelConverter) {
        System.out.println("ANALYSING THE MODELS");
        System.out.println("*********************STAGE 1********************");
        long startTime = System.nanoTime();
        List<String> avoidFileNames = this.configObj.getAvoidFileNames();
        int delayCache = this.configObj.timeCacheForDiscoveringSearchOverFilesInSeconds;
        List<String> rootPathToScan = this.searchPaths.stream().filter((x) -> !this.configObj.isInCache(x, delayCache)).toList();
        for (String pathToFolderModel : rootPathToScan) {
            File file = new File(pathToFolderModel);
            if (!file.isDirectory() || avoidFileNames.contains(file.getName())) continue;
            for (File childFile : Objects.requireNonNull(file.listFiles())) {
                try {
                    System.out.println("Analyzing path: " + childFile);
                    archModelConverter.analyzeFileAndConvert(childFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Error analysing the file: " + childFile);
                    e.printStackTrace();
                }
            }
        }
        long endTime = System.nanoTime();
        double elapsedTime = (double) (endTime - startTime) / 1000000000;
        this.scanningResult.put("filesMatched", archModelConverter.dataModelFiles.size() + "");
        this.scanningResult.put("elapsedTime", new DecimalFormat("0.000").format(elapsedTime) + "s");
        System.out.println("SCANNING RESULTS: " + this.scanningResult);
        System.out.println("ANALYSING THE MODELS COMPLETED");
        /////////UPDATING THE CACHE WITH THE ROOTS PATH OF MODELS////////
        rootPathToScan.forEach(this.configObj::putInCache);
        this.configObj.persistCacheInDisk();
        ////////////////////////////////////////////////////////////////
        System.out.println("*********************STAGE 2********************");
        System.out.println("COPYING THE AADL MODELS TO THE FOLDERS THE FILES...");
        try {
            archModelConverter.copyFoundedFiles();
        } catch (Exception e) {
            System.err.println("Error coping the files: " + e.getMessage());
        }
        System.out.println("COPYING TO FOLDERS THE FILES COMPLETED");

        System.out.println("*********************STAGE 3********************");
        System.out.println("LOGGING THE CONVERSION FILE .json...");
        try {
            archModelConverter.loggingConvertingResult();
        } catch (Exception e) {
            System.err.println("Error logging the result of conversion into .json" + e.getMessage());
        }
        System.out.println("LOGGING THE CONVERSION FILE .json COMPLETED");
        System.out.println("CREATING CSV OF ERRORS...");
        archModelConverter.createCSVOfError();
        System.out.println("CREATING CSV OF ERRORS... COMPLETED");

    }


    public List<String> searchForFiles(boolean storeOnRootPath, boolean verbose) {
        /// Run a BFS for searching ///
        int totalFiles = 0;
        long startTime = System.nanoTime();
        int delayCache = this.configObj.timeCacheForDiscoveringSearchOverFilesInSeconds;
        List<String> rootPathToScan = this.searchPaths.stream().filter((x) -> !this.configObj.isInCache(x, delayCache)).toList();
        Queue<String> queue = new LinkedList<>(rootPathToScan);
        System.out.println("SCANNING FILES ...");
        System.out.println(rootPathToScan);

        while (queue.size() > 0) {
            String pathFileOrArchive = queue.poll();
            totalFiles++;
            if (verbose) {
                System.out.println("Analysing file: " + pathFileOrArchive + " ...");
            }
            File file = new File(pathFileOrArchive);
            if (!this.configObj.getAvoidFileNames().contains(file.getName())) {
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

        }

        /////////UPDATING THE CACHE WITH THE ROOTS PATH OF MODELS////////
        for (String x : rootPathToScan) {
            this.configObj.putInCache(x);
        }
        this.configObj.persistCacheInDisk();
        ////////////////////////////////////////////////////////////////

        long endTime = System.nanoTime();
        System.out.println("SCANNING COMPLETED");
        if (storeOnRootPath) {
            try {
                this.logfilePath = Paths.get(this.rootPath, ".files-found.txt").toString();
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
        return this.dataFilesFound.stream().toList();
    }

    @Override
    public String toString() {
        return "rootPath: " + this.rootPath + "; " + "searchPaths: " +
                this.searchPaths + "; " + "exts: " + this.extensions;

    }

    static public String getExtension(String path) {
        if (!path.contains(".")) return "txt";
        String[] chunksFileString = path.split("\\.");
        return chunksFileString[chunksFileString.length - 1];
    }

    public SearchFileTraversal setSearchPaths(List<String> searchPaths) {
        this.searchPaths = searchPaths;
        return this;
    }
}
