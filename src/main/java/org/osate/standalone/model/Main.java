package org.osate.standalone.model;

import org.discover.arch.model.Config;
import org.discover.arch.model.OutputLoadedModelSchema;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.nio.file.Paths;

public class Main {
    static String URI_XMI_MODEL = "";
    static String URI_AADL_MODEL = "";

    public static void main(String[] args) {
        LoadXMIModel loadXMIModel = LoadXMIModel.getInstance();
        LoadAADLModel loadAADLModel = LoadAADLModel.getInstance();
        try {
            Config config = new Config(Paths.get("../EXAMPLE_ROOT_DIRECTORY_MODELS/config.json").toAbsolutePath().toString());
            if (args.length == 0)
                throw new Exception("The global path to the xmi file is needed");
            boolean isAadl = args[0].contains("aadl");
            boolean isXMI = args[0].contains("aaxl2");
            URI_XMI_MODEL = args[0];
            URI_AADL_MODEL = args[0];
            if (isXMI) {
                Resource resource = loadXMIModel.getResourceObjectFromXMIModel(URI_XMI_MODEL);
                TreeIterator<EObject> tree = loadXMIModel.getContentFromResource(resource);

                while (tree.hasNext()) {
                    EObject el = tree.next();
                    System.out.println(el);
                }
            }
            if (isAadl) {
                System.out.println(URI_AADL_MODEL);
                Object outputSchema = loadAADLModel.loadModel(URI_AADL_MODEL,
                        "src/main/java/org/osate/standalone/model/example_models/", "5", config);
                if (outputSchema instanceof Iterable<?>) {
                    System.out.println(outputSchema);
                } else {
                    System.out.println((outputSchema));
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
