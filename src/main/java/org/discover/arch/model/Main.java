package org.discover.arch.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static String rootPath = "/mnt/DATA/00-GSSI/00-WORK";
    static String[] archivesForSearching = {"test-repo"};
    static String[] extentionsForSearching = {"aadl"};

    public static void main(String[] args) {
        System.out.println("Hello  Pepe");
        String[] pathForSearching = new String[archivesForSearching.length];
        //////////Creating the SearchFolders///////////////////
        for (int i = 0; i < archivesForSearching.length; i++) {
            pathForSearching[i] = Paths.get(rootPath, archivesForSearching[i]).toString();
        }
        //////////////////////////////////////////////////////
        try {
            SearchFileTraversal fileDiscover = new SearchFileTraversal(rootPath, pathForSearching, extentionsForSearching);
            List<String> filesFound = fileDiscover.searchForFiles(true, false);
            System.out.println("Scanning result: " + fileDiscover.scanningResult);
            if (filesFound.size() > 200)
                System.out.println("Data found: " + filesFound.toString().substring(0, 200) + "...");
            else
                System.out.println("Data found: " + filesFound);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}