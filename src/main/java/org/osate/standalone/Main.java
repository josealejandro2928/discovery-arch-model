package org.osate.standalone;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.impl.SystemImplementationImpl;
import org.osate.aadl2.instance.impl.ComponentInstanceImpl;

public class Main {
    static String URI_XMI_MODEL = "";

    public static void main(String[] args) {
        LoadXMIModel loadXMIModel = LoadXMIModel.getInstance();
        try {
            if (args.length == 0)
                throw new Exception("The global path to the xmi file is needed");
            URI_XMI_MODEL = args[0];
            Resource resource = loadXMIModel.getResourceObjectFromXMIModel(URI_XMI_MODEL);
            TreeIterator<EObject> tree = loadXMIModel.getContentFromResource(resource);

            while (tree.hasNext()) {
                EObject el = tree.next();
                System.out.println(el);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }


    }
}
