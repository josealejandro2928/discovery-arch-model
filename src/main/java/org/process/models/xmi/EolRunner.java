package org.process.models.xmi;

import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.emc.emf.EmfModel;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EolRunner {
    private static EolRunner INSTANCE = null;

    private EolRunner() {
    }

    static public EolRunner getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        INSTANCE = new EolRunner();
        return INSTANCE;
    }

    public Object run(String eolScript, String modelPath) throws Exception {
        if (eolScript == null)
            eolScript = "main";
        EolModule module = new EolModule();
        Path rootPath = Paths.get("", "scripts", "eol").toAbsolutePath();
        String eolPath = rootPath.resolve(eolScript + ".eol").toString();
        String metaModelPath = Paths.get("ecore", "aadl2_instance.ecore").toAbsolutePath().toString();
        EmfModel model = createEmfModel("ModelImpl", modelPath, metaModelPath, true, false);
        module.parse(new File(eolPath));
        module.getContext().getModelRepository().addModel(model);
        return module.execute();
    }

    public Object runExample() throws Exception {
        EolModule module = new EolModule();
        Path rootPath = Paths.get("", "scripts", "eol");
        String eolPath = rootPath.resolve("test.eol").toString();
        String metaModelPath = Paths.get(rootPath.toString(), "example_models", "Tree.ecore").toString();
        String modelPath = Paths.get(rootPath.toString(), "example_models", "Tree.xmi").toString();
        EmfModel model = createEmfModel("TreeModel", modelPath, metaModelPath, true, false);
        module.parse(new File(eolPath));
        module.getContext().getModelRepository().addModel(model);
        return module.execute();
    }

    protected EmfModel createEmfModel(String name, String modelUri, String metaModelURI, boolean readOnLoad, boolean storeOnDisposal)
            throws EolModelLoadingException {
        EmfModel emfModel = new EmfModel();
        emfModel.setMetamodelFile(metaModelURI);
        emfModel.setModelFile(modelUri);
        emfModel.setReadOnLoad(readOnLoad);
        emfModel.setStoredOnDisposal(storeOnDisposal);
        emfModel.setName(name);
        emfModel.load();
        return emfModel;
    }
}
