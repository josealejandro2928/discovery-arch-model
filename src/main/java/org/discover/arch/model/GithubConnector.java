package org.discover.arch.model;

import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubConnector implements ExternalConnector {
    private Config configObj;

    GithubConnector(Config configObj) {
        this.configObj = configObj;
    }

    @Override
    public MetaData extractMetaData(String externalRepoURL) throws InvalidURLConnectorException {
        MetaData data = new MetaData();
        if (isValidPath(externalRepoURL)) {
            String[] parts = externalRepoURL.split("/");
            String name = parts[parts.length - 1];
            name = name.split("\\.")[0];
            data.name = name;
            if (!externalRepoURL.contains(".git")) {
                data.downloadablePath = externalRepoURL + ".git";
            }
        } else {
            throw new InvalidURLConnectorException();
        }
        return data;
    }

    @Override
    public String loadResource(String externalRepoURL, String directoryPath) throws Exception {
        MetaData metaData = this.extractMetaData(externalRepoURL);
        File clonedDirectory = Paths.get(directoryPath, metaData.name).toFile();
        try {
//            this.deleteBeforeLoading(clonedDirectory.getAbsolutePath());
            System.out.println("CLONING REPOSITORY: " + metaData.downloadablePath);
            Git.cloneRepository()
                    .setURI(metaData.downloadablePath)
                    .setDirectory(clonedDirectory)
                    .call();
//            cleaningAfterDownloadFinished(clonedDirectory.getAbsolutePath());
            System.out.println("FINISH OF CLONING REPOSITORY: " + metaData.downloadablePath);
            configObj.putInCache(externalRepoURL);
            configObj.addMoreArchivesForSearching(directoryPath);
            return clonedDirectory.getAbsolutePath();
        } catch (Exception error) {
            System.out.println("Error cloning the repo from Github");
            error.printStackTrace();
            return null;
        }
    }

    @Override
    public void cleaningAfterDownloadFinished(String clonedDirectoryRepo) throws IOException {
        List<String> filesToEliminate = Arrays.asList(".git");
        Queue<Path> queue = new LinkedList<Path>();
        queue.add(Paths.get(clonedDirectoryRepo));
        while (queue.size() > 0) {
            File file = queue.poll().toFile();
            if (filesToEliminate.contains(file.getName())) {
                Config.deleteDirectory(file.toPath());
                file.delete();
            } else {
                if (file.isDirectory()) {
                    try {
                        for (File childFile : Objects.requireNonNull(file.listFiles())) {
                            queue.add(childFile.toPath());
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR reading the files of the directory: " + file);
                    }
                }
            }
        }
    }

    public void deleteBeforeLoading(String clonedDirectoryRepo) throws Exception {
        File file = new File(clonedDirectoryRepo);
        if (!file.exists()) return;
        Files.walk(file.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach((File x) -> {
                    x.delete();
                });
    }

    @Override
    public boolean isValidPath(String externalRepoURL) {
        String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern patt = Pattern.compile(regex);
        Matcher matcher = patt.matcher(externalRepoURL);
        boolean isValidURL = matcher.matches();
        boolean isGithubURL = externalRepoURL.contains("github.com");
        return isValidURL && isGithubURL;
    }
}
