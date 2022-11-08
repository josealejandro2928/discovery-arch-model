package org.process.models.xmi.emf;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.util.Map;

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


    void init() {
        System.out.println("Init ecore standalone");
        this.resourceSet = new ResourceSetImpl();
        this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
                "ecore", new EcoreResourceFactoryImpl());
        this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
                "xmi", new XMIResourceFactoryImpl());
        this.ecorePackage = EcorePackage.eINSTANCE;
    }

    ResourceSet getResourceSet() {
        return this.resourceSet;
    }
}
