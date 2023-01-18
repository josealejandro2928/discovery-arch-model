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
        List<String> pathToModelsFiles = (List<String>) crossReferenceResolverOut.get("foundFiles");
        File fileAadl = new File(pathAADLFileOrDirectory);
        File fileXML = new File(pathXMLFile);
        List<EObject> contents;
        AadlPackage aadlPackage;
        List<SystemImplementation> systemImplementations;
        List<OutputLoadedModelSchema> resultOutput = new ArrayList<>();
        Set<Resource> resourceSet;

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
                System.out.println("\t\033[0;31m" + "Error resolving path of models: " + e.getMessage() + "\033[0m");

            }
        }
        ////////LOADING PREDECLARED AADL DEFINITIONS///////////
        Set<String> predeclaredFilesModelAADL = (HashSet<String>) this.loadPredeclaredPropertySetsAADL(rs);
        /////////////////////////////////////////////////
        for (Resource resource : rs.getResources()) {
            try {
                resource.load(null);
            } catch (Exception e) {
                System.out.println("\t\033[0;31m" + "Error loading resources: " + e.getMessage() + "\033[0m");

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
                contents = resourceModel.getContents();
                if (contents.size() == 0) {
                    throw new Exception("This model: " + outputSchema.pathAADLFile + " cannot be loaded, it must be corrupted");
                }
                if (!(contents.get(0) instanceof AadlPackage))
                    throw new Exception("This model: " + outputSchema.pathAADLFile + " is not an AadlPackage");
                aadlPackage = (AadlPackage) contents.get(0);
                outputSchema.modelName = aadlPackage.getFullName();

                systemImplementations = aadlPackage.getPublicSection().getOwnedClassifiers().stream()
                        .filter((Classifier classifier) -> classifier instanceof SystemImplementationImpl)
                        .map((Classifier el) -> (SystemImplementation) el).toList();

                for (SystemImplementation systemImpl : systemImplementations) {
                    OutputLoadedModelSchema output = new OutputLoadedModelSchema(outputSchema);
                    SystemInstance systemInstance;
                    try {
                        systemInstance = InstantiateModel.instantiate(systemImpl);
                        output.isSavedTheModel = true;
                        output.pathXMLFile = saveModelToXMI(systemInstance, rs, pathXMLFile, output.modelName, id);
                        resultOutput.add(output);
                    } catch (final Exception e) {
                        output.errors.add(e.getMessage());
                        output.isSavedTheModel = false;
                        output.isParsingSucceeded = false;
                        resultOutput.add(output);
                        System.out.println("\033[0;31m" + "Error instantiating the model: " + output.modelName +
                                " which system instance is: " + systemImpl.getName() + "\033[0m");
                    }
                }

            } catch (Exception e) {
                outputSchema.errors.add(e.getMessage());
                outputSchema.isParsingSucceeded = false;
                System.out.println("\t\033[0;31m" + "Error: " + e.getMessage() + "\033[0m");
                resultOutput.add(outputSchema);
            }
        }
        rs = null;
        resourceSet = null;
        Map<String, Object> dataOutput = new HashMap<>();
        dataOutput.put(this.MODEL_FILES_FOUND, pathToModelsFiles);
        dataOutput.put(this.CONVERTING_OUTPUT, resultOutput);
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

    private String saveModelToXMI(SystemInstance systemInstance, XtextResourceSet rs, String pathXMLFile, String parentName, String id)
            throws Exception {
        String instanceName = pathXMLFile;
        if (id != null) {
            instanceName += id + "_";
        }
        if (parentName != null) {
            instanceName += parentName + "_";
        }

        String simple_name = systemInstance.getName().replaceAll("_Instance", "");
        instanceName += simple_name + ".aaxl2";
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
