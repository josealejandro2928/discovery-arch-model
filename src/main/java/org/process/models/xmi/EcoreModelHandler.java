package org.process.models.xmi;

import org.discover.arch.model.SearchFileTraversal;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.impl.SystemImplementationImpl;
import org.osate.aadl2.instance.impl.ComponentInstanceImpl;
import org.osate.aadl2.instance.impl.ConnectionInstanceImpl;
import org.osate.aadl2.instance.impl.SystemInstanceImpl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class EcoreModelHandler {
    static private EcoreModelHandler INSTANCE = null;
    private List<Resource> models;
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

    List<Resource> getModels() {
        return this.models;
    }

    public List<String> getUriModels() {
        return uriModels;
    }

    void addModel(Resource m) {
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
        System.out.println("PARSING AND GETTING THE ECORE OBJECT FROM MODELS XMI");
        for (String modelUri : this.uriModels) {
            try {
                Resource resource = obj.getModelByURI(modelUri);
                this.addModel(resource);
                DataRepresentationFeature data = getDataFromResource(resource);
                System.out.println("------------------------------------------------------------------");
                System.out.println("URI: " + modelUri);
                System.out.println(data);
                System.out.println("------------------------------------------------------------------");
            } catch (Exception e) {
                System.out.println("Error getting the models from URI: " + e.getMessage());
                e.printStackTrace();
            }

        }
    }

    DataRepresentationFeature getDataFromResource(Resource resource) {
        TreeIterator<EObject> treeIterator = resource.getAllContents();
        DataRepresentationFeature objFeature = new DataRepresentationFeature();

        while (treeIterator.hasNext()) {

            EObject node = treeIterator.next();
            if (node instanceof SystemInstanceImpl sysNode) {
                objFeature.setSystemName(sysNode.getName());
            }
            if (node instanceof ComponentInstanceImpl componentInstanceNode) {
                objFeature.getComponents().add(componentInstanceNode);
                objFeature.setComponentsNumber(objFeature.getComponentsNumber() + 1);
            }
            if (node instanceof ConnectionInstanceImpl connectionInstanceNode) {
                objFeature.getConnections().add(connectionInstanceNode);
                objFeature.setConnectionsNumber(objFeature.getConnectionsNumber() + 1);
            }
        }
        return objFeature;
    }


}


