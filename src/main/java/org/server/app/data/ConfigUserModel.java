package org.server.app.data;

import com.mongodb.lang.NonNull;
import dev.morphia.annotations.*;
import org.bson.types.ObjectId;
import org.discover.arch.model.Config;
import org.server.app.utils.ConfigServer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Entity("configs")
public class ConfigUserModel {
    @Id
    private ObjectId id;
    @NonNull
    private List<String> archivesForSearching;
    @NonNull
    private int timeCacheForDiscoveringSearchOverFilesInSeconds;
    @NonNull
    private List<String> avoidFileNames;
    @NonNull
    private List<String> extensionsForSearching;
    @NonNull
    private String outputFolderName;

    @NonNull
    private int timeCacheForPollingFromExternalResources;
    @NonNull
    private String ecoreRequiredFilesFolder;
    @NonNull
    private String rootPath;

    @NonNull
    private List<String> externalResources;
    @NonNull
    private String pathToConfigJson;

    @Reference
    private UserModel user;

    @NonNull
    public String getPathToConfigJson() {
        return pathToConfigJson;
    }

    public void setPathToConfigJson(@NonNull String pathToConfigJson) {
        this.pathToConfigJson = pathToConfigJson;
    }

    public ConfigUserModel() {

    }

    static public ConfigUserModel buildConfig(UserModel user) {
        ConfigServer configServer = ConfigServer.getInstance();
        ConfigUserModel configUser = new ConfigUserModel();
        Path directoryPath = Paths.get(configServer.dotenv.get("ROOT_STORAGE"), user.getEmail()).toAbsolutePath();
        configUser.archivesForSearching = Arrays.asList(Paths.get(directoryPath.toString(), "local_models").toString());
        configUser.timeCacheForDiscoveringSearchOverFilesInSeconds = 300;
        configUser.timeCacheForPollingFromExternalResources = 300;
        configUser.avoidFileNames = Arrays.asList(".git", ".gitignore", ".project", ".aadlbin-gen");
        configUser.extensionsForSearching = Arrays.asList("aadl");
        configUser.outputFolderName = "output-processing";
        configUser.ecoreRequiredFilesFolder = "./ecore";
        configUser.rootPath = directoryPath.toString();
        configUser.externalResources = new ArrayList<>();
        configUser.pathToConfigJson = Paths.get(directoryPath.toString(), "config.json").toString();
        configUser.user = user;
        return configUser;
    }

    @NonNull
    public UserModel getUser() {
        return user;
    }

    public void setUser(@NonNull UserModel user) {
        this.user = user;
    }

    public String getId() {
        return this.id.toString();
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @NonNull
    public List<String> getArchivesForSearching() {
        return archivesForSearching;
    }

    public void setArchivesForSearching(@NonNull List<String> archivesForSearching) {
        this.archivesForSearching = archivesForSearching;
    }

    public int getTimeCacheForDiscoveringSearchOverFilesInSeconds() {
        return timeCacheForDiscoveringSearchOverFilesInSeconds;
    }

    public void setTimeCacheForDiscoveringSearchOverFilesInSeconds(int timeCacheForDiscoveringSearchOverFilesInSeconds) {
        this.timeCacheForDiscoveringSearchOverFilesInSeconds = timeCacheForDiscoveringSearchOverFilesInSeconds;
    }

    @NonNull
    public List<String> getAvoidFileNames() {
        return avoidFileNames;
    }

    public void setAvoidFileNames(@NonNull List<String> avoidFileNames) {
        this.avoidFileNames = avoidFileNames;
    }

    @NonNull
    public List<String> getExtensionsForSearching() {
        return extensionsForSearching;
    }

    public void setExtensionsForSearching(@NonNull List<String> extensionsForSearching) {
        this.extensionsForSearching = extensionsForSearching;
    }

    @NonNull
    public String getOutputFolderName() {
        return outputFolderName;
    }

    public void setOutputFolderName(@NonNull String outputFolderName) {
        this.outputFolderName = outputFolderName;
    }

    public int getTimeCacheForPollingFromExternalResources() {
        return timeCacheForPollingFromExternalResources;
    }

    public void setTimeCacheForPollingFromExternalResources(int timeCacheForPollingFromExternalResources) {
        this.timeCacheForPollingFromExternalResources = timeCacheForPollingFromExternalResources;
    }

    @NonNull
    public String getEcoreRequiredFilesFolder() {
        return ecoreRequiredFilesFolder;
    }

    public void setEcoreRequiredFilesFolder(@NonNull String ecoreRequiredFilesFolder) {
        this.ecoreRequiredFilesFolder = ecoreRequiredFilesFolder;
    }

    @NonNull
    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(@NonNull String rootPath) {
        this.rootPath = rootPath;
    }

    @NonNull
    public List<String> getExternalResources() {
        return externalResources;
    }

    public void setExternalResources(@NonNull List<String> externalResources) {
        this.externalResources = externalResources;
    }

    public ConfigUserModel syncFromConfig(Config config){
        this.setArchivesForSearching(config.getArchivesForSearching());
        this.setAvoidFileNames(config.getAvoidFileNames());
        this.setExternalResources(config.getExternalResources());
        this.setExtensionsForSearching(config.getExtensionsForSearching());
        this.setTimeCacheForDiscoveringSearchOverFilesInSeconds(config.timeCacheForDiscoveringSearchOverFilesInSeconds);
        this.setTimeCacheForPollingFromExternalResources(config.timeCacheForPollingFromExternalResources);
        return this;
    }
}
