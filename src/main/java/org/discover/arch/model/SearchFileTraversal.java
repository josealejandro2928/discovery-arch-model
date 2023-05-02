package org.discover.arch.model;

import java.io.File;
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
    Set<String> dataFilesFound = new HashSet<>();
    private Config configObj = null;

    SearchFileTraversal(String rootPath, String[] searchPaths, String[] exts, String folderOutputName) {
        this.rootPath = rootPath;
        this.searchPaths = Arrays.asList(searchPaths);
        this.extensions = Arrays.asList(exts);
        this.folderOutputName = folderOutputName;
    }

    public SearchFileTraversal(Config configObj) {
        this.configObj = configObj;
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
        if (rootPathToScan.size() == 0 && this.searchPaths.size() > 0) {
            System.out.println("\033[0;33m" + "ALL THE FILES TO ANALYSE WERE ANALYZE BEFORE " +
                    "AND THE TIME INVALIDATION CACHE HAS NOT BEEN PASSED\n" +
                    "THE CURRENT TIME INVALIDATION CACHE IS: " + delayCache + "s"
                    + "\033[0m");
        }
        for (String pathToFolderModel : rootPathToScan) {
            File file = new File(pathToFolderModel);
            if (!file.isDirectory() || avoidFileNames.contains(file.getName())) continue;
            int totalFiles = Objects.requireNonNull(file.listFiles()).length;
            int indexFile = 0;
            for (File childFile : Objects.requireNonNull(file.listFiles())) {
                try {
                    System.out.println("\033[0;33m" + indexFile + "/" + totalFiles + " ANALYZING PATH: " + childFile + "\033[0m");
                    archModelConverter.analyzeFileAndConvert(childFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Error analysing the file: " + childFile);
                    e.printStackTrace();
                }
                indexFile++;
                System.out.println("\033[0;32m" + "MANUAL GARBAGE COLLECTION EXECUTED" + "\033[0m");
                System.gc();
            }
        }
        long endTime = System.nanoTime();
        double elapsedTime = (double) (endTime - startTime) / 1000000000;
        System.out.println("\033[0;32m" + "ELAPSED TIME: " + new DecimalFormat("0.000").format(elapsedTime) + "s" + "\033[0m");
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

    @Override
    public String toString() {
        return "rootPath: " + this.rootPath + "; " + "searchPaths: " +
                this.searchPaths + "; " + "ext: " + this.extensions;

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

    public void clear(){
        this.searchPaths = null;
        this.dataFilesFound = new HashSet<>();
    }
}
