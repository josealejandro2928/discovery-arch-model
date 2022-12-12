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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<OutputLoadedModelSchema> resultOutput = new ArrayList<>();
        Map<String, Object> crossReferenceResolverOut = CrossReferenceResolver.resolve(pathAADLFile, null);
        List<String> pathToModelsFiles = (List<String>) crossReferenceResolverOut.get("foundFiles");
        String parentDirectoryName = (String) crossReferenceResolverOut.get("parentName");
        Resource[] resources = new Resource[pathToModelsFiles.size()];
        File fileAadl = new File(pathAADLFile);
        File fileXML = new File(pathXMLFile);

        if (!fileAadl.exists()) {
            throw new Exception("The addl file: " + pathAADLFile + " does not exits");
        }
        if (!fileXML.exists()) {
            throw new Exception("The file for storing the XMI files: " + pathXMLFile + "does not exits");
        }

        try {
            for (int i = 0; i < pathToModelsFiles.size(); i++) {
                resources[i] = rs.getResource(URI.createURI(pathToModelsFiles.get(i)), true);
            }
            for (Resource resource : resources) {
                resource.load(null);
            }
            Resource rsrc = resources[0];
            EcoreUtil.resolveAll(rsrc);
            ////////////// VALIDATE THE MODEL ///////////////////////////
            outputSchema.pathAADLFile = pathAADLFile;
            outputSchema.pathXMLFile = pathXMLFile;
            outputSchema.errors = validateModel(new Resource[]{rsrc});
            ////////////////////////////////////////////////////////////
            List<EObject> contents = rsrc.getContents();
            if (contents.size() == 0) {
                throw new Exception("This model cannot be loaded, it must be corrupted");
            }
            final AadlPackage aadlPackage = (AadlPackage) contents.get(0);
            outputSchema.modelName = aadlPackage.getFullName();
            List<SystemImplementation> systemImplementations = new ArrayList<>();
            if (verbose)
                System.out.println("Looking for only system implementation models...");
            for (final Classifier classifier : aadlPackage.getPublicSection().getOwnedClassifiers()) {
                if (classifier instanceof SystemImplementationImpl) {
                    systemImplementations.add((SystemImplementation) classifier);
                }
            }

            for (SystemImplementation systemImpl : systemImplementations) {
                OutputLoadedModelSchema output = new OutputLoadedModelSchema(outputSchema);
                try {
                    final SystemInstance systemInstance = InstantiateModel.instantiate(systemImpl);
                    output.isSavedTheModel = true;
                    output.pathXMLFile = saveModelToXMI(systemInstance, rs, pathXMLFile, parentDirectoryName, id);
                    resultOutput.add(output);
                } catch (final Exception e) {
                    output.isSavedTheModel = false;
                    output.isParsingSucceeded = false;
                    e.printStackTrace();
                    throw new Exception("Error during instantiation " + e.getMessage());
                }
            }
            return resultOutput;
        } catch (final Exception e) {
            outputSchema.errors.add(e.getMessage());
            outputSchema.isParsingSucceeded = false;
            if (verbose)
                System.out.print(outputSchema);
            return outputSchema;
        }
    }

    private List<String> validateModel(Resource[] resources) {
        List<Issue> issues = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (final Resource resource : resources) {
            IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
                    .getResourceValidator();
            try {
                issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
            } catch (Exception e) {
                System.err.println("****************************** " + e);
            }

            for (int i = 0; i < issues.size() && i < 10; i++) {
                Issue issue = issues.get(i);
                errors.add(issue.getMessage());
            }
        }
        return errors;
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
        return instanceName;
    }

}