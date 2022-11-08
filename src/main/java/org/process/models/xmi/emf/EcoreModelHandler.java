package org.process.models.xmi.emf;

import org.discover.arch.model.SearchFileTraversal;
import org.eclipse.emf.ecore.EObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class EcoreModelHandler {
    static private EcoreModelHandler INSTANCE = null;
    private List<EObject> models;
    private List<String> uriModels;
    private String rootPathFolder;
    private final List<String> modelExtension;

    private EcoreModelHandler() {
        this.uriModels = new ArrayList<>();
        this.models = new ArrayList<>();
        this.modelExtension = Arrays.asList("xml", "xmi", "ecore", "aaxl2");

    }

    static EcoreModelHandler getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        else
            INSTANCE = new EcoreModelHandler();
        return INSTANCE;
    }

    List<EObject> getModels() {
        return this.models;
    }

    public List<String> getUriModels() {
        return uriModels;
    }

    void addModel(EObject m) {
        if (!models.contains(m))
            models.add(m);
    }

    public void setRootPathFolder(String rootPathFolder) {
        this.rootPathFolder = rootPathFolder;
    }

    void discoverModelFromPath() throws Exception {
        if (rootPathFolder == null)
            throw new Exception("There is not root path for reading the XMI models");
        this.uriModels = new ArrayList<>();
        Files.walk(Path.of(rootPathFolder)).sorted().map(Path::toFile).forEach(
                (File file) -> {
                    if (file.isFile()) {
                        String uriModel = file.getPath();
                        String ext = SearchFileTraversal.getExtension(uriModel);
                        if (this.modelExtension.contains(ext))
                            this.uriModels.add(file.getPath());
                    }
                });
    }

    void processModels(EcoreStandAlone obj) throws Exception {
        for (String modelUri : this.uriModels.subList(0, 1)) {
            EObject item = obj.getModelByURI(modelUri);
            System.out.println(item);
            this.models.add(item);
        }
    }


}
