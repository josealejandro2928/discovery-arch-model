package org.osate.standalone.model;

import com.google.inject.Injector;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.impl.SystemImplementationImpl;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.util.Aadl2ResourceFactoryImpl;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;
import org.discover.arch.model.OutputLoadedModelSchema;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public class LoadAADLModel implements RawModelLoader {
    private static LoadAADLModel INSTANCE = null;
    Injector injector;
    private final String PREDECLARED_PROPERTY_SET = Paths.get("aadl").toAbsolutePath().toString();

    private LoadAADLModel() {
        this.injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("aaxl2", new Aadl2ResourceFactoryImpl());
        InstancePackage.eINSTANCE.eClass();
    }

    public static LoadAADLModel getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        INSTANCE = new LoadAADLModel();
        return INSTANCE;
    }

    public Map<String, Object> loadModel(String pathAADLFileOrDirectory, String pathXMLFile, String id, boolean verbose) throws Exception {
        XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
        Map<String, Object> crossReferenceResolverOut = CrossReferenceResolver.resolveDown(pathAADLFileOrDirectory);
        List<String> pathToModelsFiles = (List<String>) crossReferenceResolverOut.get(CrossReferenceResolver.FOUND_FILES);
        List<String> pathToDocFiles = (List<String>) crossReferenceResolverOut.get(CrossReferenceResolver.DOC_FILES);
        File fileAadl = new File(pathAADLFileOrDirectory);
        File fileXML = new File(pathXMLFile);
        List<OutputLoadedModelSchema> resultOutput = new ArrayList<>();
        Set<Resource> resourceSet;
        Set<String> resourcesInstantiated = new HashSet<>();

        if (!fileAadl.exists()) {
            throw new Exception("The aadl file: " + pathAADLFileOrDirectory + " does not exits");
        }
        if (!fileXML.exists()) {
            throw new Exception("The file for storing the XMI files: " + pathXMLFile + "does not exits");
        }

//        System.out.println("pathToModelsFiles: " + pathToModelsFiles);

        for (String modelPaths : pathToModelsFiles) {
            try {
                rs.getResource(URI.createURI(modelPaths), true);
            } catch (Exception e) {
                System.err.println("\tError resolving path of models: " + e.getMessage());

            }
        }
        ////////LOADING PREDECLARED AADL DEFINITIONS///////////
        Set<String> predeclaredFilesModelAADL = (HashSet<String>) this.loadPredeclaredPropertySetsAADL(rs);
        /////////////////////////////////////////////////
        for (Resource resource : rs.getResources()) {
            try {
                resource.load(null);
            } catch (Exception e) {
                System.err.println("\tError loading resources: " + e.getMessage());

            }
        }

        resourceSet = new HashSet<>(rs.getResources());
        for (Resource resourceModel : resourceSet) {
            String resourceModelPath = resourceModel.getURI().toString();
            System.out.println("\t-model: " + resourceModelPath);
            if (predeclaredFilesModelAADL.contains(resourceModelPath))
                continue;
            OutputLoadedModelSchema outputSchema = new OutputLoadedModelSchema();
            try {
                EcoreUtil.resolveAll(resourceModel);
                outputSchema.pathAADLFile = resourceModelPath;
                outputSchema.pathXMLFile = pathXMLFile;
                outputSchema.errors = validateModel(new Resource[]{resourceModel});
                List<EObject> contents = resourceModel.getContents();
                if (contents.size() == 0) {
                    throw new Exception("This model: " + outputSchema.pathAADLFile + " cannot be loaded, it must be corrupted");
                }
                if (!(contents.get(0) instanceof AadlPackage))
                    throw new Exception("This model: " + outputSchema.pathAADLFile + " is not an AadlPackage");
                AadlPackage aadlPackage = (AadlPackage) contents.get(0);
                outputSchema.modelName = aadlPackage.getFullName();

                List<SystemImplementation> systemImplementations = aadlPackage.getPublicSection().getOwnedClassifiers().stream()
                        .filter((Classifier classifier) -> classifier instanceof SystemImplementationImpl)
                        .map((Classifier el) -> (SystemImplementation) el).toList();

                for (SystemImplementation systemImpl : systemImplementations) {
                    OutputLoadedModelSchema output = new OutputLoadedModelSchema(outputSchema);
                    SystemInstance systemInstance;
                    try {
                        systemInstance = InstantiateModel.instantiate(systemImpl);
                        output.isSavedTheModel = true;
                        output.pathXMLFile = saveModelToXMI(systemInstance, rs, pathXMLFile, output.modelName, id, resourcesInstantiated);
                        resultOutput.add(output);
                    } catch (final Exception e) {
                        output.errors.add(e.getMessage());
                        output.isSavedTheModel = false;
                        output.isParsingSucceeded = false;
                        resultOutput.add(output);
                        System.err.println("\tError instantiating the model: " + output.modelName +
                                " which system instance is: " + systemImpl.getName() + ": " + e);
//                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                outputSchema.errors.add(e.getMessage());
                outputSchema.isParsingSucceeded = false;
                System.err.println("\tError: " + e.getMessage());
                resultOutput.add(outputSchema);
            }
        }
        rs = null;
        resourceSet = null;
        resourcesInstantiated = null;
        Map<String, Object> dataOutput = new HashMap<>();
        dataOutput.put(this.MODEL_FILES_FOUND, pathToModelsFiles);
        dataOutput.put(this.CONVERTING_OUTPUT, resultOutput);
        dataOutput.put(this.DOC_FILES, pathToDocFiles);
        return dataOutput;
    }


    public List<Object> validateModel(Resource[] resources) {
        List<Issue> issues = new ArrayList<>();
        for (final Resource resource : resources) {
            IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
                    .getResourceValidator();
            try {
                issues = validator.validate(resource, CheckMode.NORMAL_AND_FAST, CancelIndicator.NullImpl);
            } catch (Exception e) {
                System.err.println("****************************** " + e);
            }
        }
        List<Object> error = new ArrayList<>(issues);
        return error.subList(0, Math.min(10, error.size()));
    }

    private String saveModelToXMI(SystemInstance systemInstance,
                                  XtextResourceSet rs,
                                  String pathXMLFile,
                                  String parentName,
                                  String id,
                                  Set<String> resourcesInstantiated)

            throws Exception {
        String instanceName = pathXMLFile;
        if (id != null) {
            instanceName += id + "_";
        }
        if (parentName != null) {
            instanceName += parentName + "_";
        }

        String simple_name = systemInstance.getName().replaceAll("_Instance", "");
        int indexName = 1;
        instanceName += simple_name + "_" + indexName;

        while (resourcesInstantiated.contains(instanceName)) {
            List<String> name_split = new ArrayList<>(List.of(instanceName.split("_")));
            name_split.remove(name_split.size() - 1);
            instanceName = String.join("_", name_split) + "_" + indexName;
            indexName++;
        }
        resourcesInstantiated.add(instanceName);
        instanceName += ".aaxl2";
        Resource xmiResource = rs.createResource(URI.createURI(instanceName));
        xmiResource.getContents().add(systemInstance);
        xmiResource.save(null);
        xmiResource = null;
        return instanceName;
    }

    private Iterable<String> loadPredeclaredPropertySetsAADL(XtextResourceSet rs) {
        Set<String> predeclaredFilesModelAADL = new HashSet<>();
        File file = new File(this.PREDECLARED_PROPERTY_SET);
        for (File fileChild : Objects.requireNonNull(file.listFiles())) {
            String pathTo_AADL_Resource = fileChild.getAbsolutePath();
            Resource resource = rs.getResource(URI.createURI(pathTo_AADL_Resource), true);
            predeclaredFilesModelAADL.add(resource.getURI().toString());
        }
        return predeclaredFilesModelAADL;
    }

}
