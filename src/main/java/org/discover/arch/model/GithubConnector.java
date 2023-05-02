package org.discover.arch.model;

import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

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
    public void loadResource(String externalRepoURL, String directoryPath) throws Exception {
        MetaData metaData = this.extractMetaData(externalRepoURL);
        File clonedDirectory = Paths.get(directoryPath, metaData.name).toFile();
        Git git = null;
        try {
            if (!this.isReadyForDownload(clonedDirectory.toString())) return;
            System.out.println("CLONING REPOSITORY: " + metaData.downloadablePath);
            this.deleteBeforeLoading(clonedDirectory.toString());
            git = Git.cloneRepository()
                    .setURI(metaData.downloadablePath)
                    .setDirectory(clonedDirectory)
                    .call();
            System.out.println("FINISH OF CLONING REPOSITORY: " + metaData.downloadablePath);
            configObj.putInCache(externalRepoURL);
            configObj.addMoreArchivesForSearching(directoryPath);
        } catch (Exception error) {
            System.out.println("Error cloning the repo from Github");
            error.printStackTrace();
        } finally {
            System.out.println("CLOSED THE GIT CONNECTION");
            if (git != null) git.close();
        }
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

    @Override
    public boolean isReadyForDownload(String repoPath) {
        System.out.println("\t-Analysing if there are new commits on: " + repoPath);
        File repoDir = new File(repoPath);
        if (!repoDir.exists()) {
            System.out.println("\t(New commits)");
            return true;
        }
        Git git = null;
        try {
            git = Git.open(repoDir);
            git.fetch().call();
            ObjectId localHead = git.getRepository().resolve("HEAD");
            Collection<Ref> refs = git.lsRemote().setHeads(true).setTags(false).call();

            // Get the hash of the latest commit on the default branch
            String defaultBranchRef = "refs/heads/" + git.getRepository().getBranch();
            ObjectId remoteHead = refs.stream()
                    .filter(s -> s.toString().contains(defaultBranchRef))
                    .map(Ref::getObjectId)
                    .findFirst()
                    .orElse(null);
            if (localHead.equals(remoteHead)) {
                System.out.println("\t(No new commits)");
                return false;
            } else {
                System.out.println("\t(New commits)");
                return true;
            }
        } catch (IOException | GitAPIException e) {
            System.out.println("\t(No new commits)");
            return false;
        } finally {
            if (git != null) git.close();
        }
    }

    @Override
    public void deleteBeforeLoading(String clonedDirectoryRepo) throws Exception {
        File file = new File(clonedDirectoryRepo);
        if (!file.exists()) return;
        Config.deleteDirectory(file.toPath());
    }
}
