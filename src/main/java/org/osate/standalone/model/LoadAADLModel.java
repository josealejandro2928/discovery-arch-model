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
import java.util.*;

public class LoadAADLModel implements RawModelLoader {
    private static LoadAADLModel INSTANCE = null;
    Injector injector;

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

    public Object loadModel(String pathAADLFile, String pathXMLFile, String id, boolean verbose) throws Exception {
        XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
        OutputLoadedModelSchema outputSchema = new OutputLoadedModelSchema();
        Map<String, Object> crossReferenceResolverOut = CrossReferenceResolver.resolve(pathAADLFile, null);
        List<String> pathToModelsFiles = (List<String>) crossReferenceResolverOut.get("foundFiles");
        File fileAadl = new File(pathAADLFile);
        File fileXML = new File(pathXMLFile);
        List<EObject> contents;
        AadlPackage aadlPackage;
        List<SystemImplementation> systemImplementations;

        if (!fileAadl.exists()) {
            throw new Exception("The addl file: " + pathAADLFile + " does not exits");
        }
        if (!fileXML.exists()) {
            throw new Exception("The file for storing the XMI files: " + pathXMLFile + "does not exits");
        }

//        System.out.println("pathToModelsFiles: " + pathToModelsFiles);
        try {
            for (String modelPaths : pathToModelsFiles) {
                rs.getResource(URI.createURI(modelPaths), true);
            }
            for (Resource resource : rs.getResources()) {
                resource.load(null);
            }
            Resource resourceRoot = rs.getResources().get(0);
            EcoreUtil.resolveAll(resourceRoot);
            ////////////// VALIDATE THE MODEL ///////////////////////////
            outputSchema.pathAADLFile = pathAADLFile;
            outputSchema.pathXMLFile = pathXMLFile;
            outputSchema.errors = validateModel(new Resource[]{resourceRoot});
            ////////////////////////////////////////////////////////////
            contents = resourceRoot.getContents();
            if (contents.size() == 0) {
                throw new Exception("This model: " + pathAADLFile + " cannot be loaded, it must be corrupted");
            }

            if (!(contents.get(0) instanceof AadlPackage))
                return null;

            aadlPackage = (AadlPackage) contents.get(0);
            outputSchema.modelName = aadlPackage.getFullName();
            systemImplementations = new ArrayList<>();
            if (verbose) {
                System.out.println("Looking for only system implementation models...");
            }
            for (Classifier classifier : aadlPackage.getPublicSection().getOwnedClassifiers()) {
                if (classifier instanceof SystemImplementationImpl) {
                    systemImplementations.add((SystemImplementation) classifier);
                }
            }

            List<OutputLoadedModelSchema> resultOutput = new ArrayList<>();
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
                } finally {
                    output = null;
                    systemInstance = null;
                }
            }
            if (resultOutput.isEmpty()) {
                resultOutput.add(outputSchema);
            }

            return resultOutput;
        } catch (final Exception e) {
            outputSchema.errors.add(e.getMessage());
            outputSchema.isParsingSucceeded = false;
            System.out.println("\033[0;31m" + "Error: " + e.getMessage() + "\033[0m");
            if (verbose)
                System.out.print(outputSchema);
            return outputSchema;
        } finally {
            rs = null;
            contents = null;
            aadlPackage = null;
            systemImplementations = null;
            crossReferenceResolverOut = null;
        }
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

}
