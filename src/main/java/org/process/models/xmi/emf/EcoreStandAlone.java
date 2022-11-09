package org.process.models.xmi.emf;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.BasicExtendedMetaData;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.osate.aadl2.util.Aadl2ResourceFactoryImpl;
import org.osate.aadl2.instance.InstancePackage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EcoreStandAlone {
    static private EcoreStandAlone INSTANCE = null;
    private ResourceSet resourceSet;
    private EcorePackage ecorePackage;

    private EcoreStandAlone() {

    }

    static EcoreStandAlone getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        else
            INSTANCE = new EcoreStandAlone();
        return INSTANCE;
    }


    void init(String requiredEcoreDirectoryPath) throws Exception {
        System.out.println("Init ecore standalone");
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("aaxl2", new Aadl2ResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
//        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        InstancePackage.eINSTANCE.eClass();
        EcorePackage.eINSTANCE.eClass();

        this.resourceSet = new ResourceSetImpl();
        System.out.println("Loading the required .ecore modules (aadl2.ecore,aadl2_instance.ecore) and registering them");
        List<Resource> resources = new ArrayList<>();
        resources.add(resourceSet.getResource(URI.createFileURI(Paths.get(requiredEcoreDirectoryPath, "aadl2.ecore").toString()), true));
        for (Resource resource : resources) {
            EObject eObject = resource.getContents().get(0);
            if (eObject instanceof EPackage ep) {
                this.resourceSet.getPackageRegistry().put(ep.getNsURI(), ep);
            }
        }
    }

    private EObject getEObjectFromResource(Resource resource) throws Exception {
        if (resource.getContents().size() == 0)
            throw new Exception("There is not content in the resource loaded");
        return resource.getContents().get(0);
    }


    /**
     * @param modelUri The url path to the model to get the EObject instance ej: '/file/xxx/xx/model.xml'
     * @return EObject instance
     */
    EObject getModelByURI(String modelUri) throws Exception {
        Resource resource = this.resourceSet.getResource(URI.createFileURI(modelUri), true);
        resource.load(null);
        return getEObjectFromResource(resource);
    }

    ResourceSet getResourceSet() {
        return this.resourceSet;
    }
}
