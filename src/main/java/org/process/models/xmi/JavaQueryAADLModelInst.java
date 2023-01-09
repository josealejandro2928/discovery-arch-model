package org.process.models.xmi;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instance.impl.SystemInstanceImpl;
import org.osate.standalone.model.LoadXMIModel;

import java.util.*;

public class JavaQueryAADLModelInst implements QueryModel {
    private static JavaQueryAADLModelInst INSTANCE = null;
    public LoadXMIModel loadXMIModel = LoadXMIModel.getInstance();

    final String SYSTEM_NAME = "systemName";
    final String COMPONENTS = "components";
    final String CONNECTORS = "connectors";
    final String SIZE = "size";
    final String NO_HARDWARE = "no_hardware";
    final String NO_SOFTWARE = "no_software";
    final String NO_DATA_STORAGE = "no_data_storage";
    final String NO_SYS = "no_sys";
    final String UDY = "udy";

    private JavaQueryAADLModelInst() {
    }

    static public JavaQueryAADLModelInst getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        INSTANCE = new JavaQueryAADLModelInst();
        return INSTANCE;
    }

    @Override
    public Object run(String script, String modelPath) throws Exception {
        return this.run(modelPath);
    }

    @Override
    public Object run(String modelPath) throws Exception {
        Map<String, Object> dataOutput = new HashMap<>();
        dataOutput.put(SYSTEM_NAME, null);
        dataOutput.put(COMPONENTS, 0);
        dataOutput.put(CONNECTORS, 0);
        dataOutput.put(SIZE, 0);
        dataOutput.put(NO_HARDWARE, 0);
        dataOutput.put(NO_SOFTWARE, 0);
        dataOutput.put(NO_DATA_STORAGE, 0);
        dataOutput.put(NO_SYS, 0);
        dataOutput.put(UDY, 0);
        Resource resourceModel = this.loadXMIModel.getResourceObjectFromXMIModel(modelPath);
        if (resourceModel == null)
            throw new Exception("Error loading the resource for the given: " + modelPath);
        TreeIterator<EObject> treeEObjectIterator = resourceModel.getAllContents();
        final SystemInstanceImpl systemInstance = (SystemInstanceImpl) resourceModel.getContents().get(0);
        dataOutput.put(SYSTEM_NAME, systemInstance.getName());

        while (treeEObjectIterator.hasNext()) {
            EObject nodeElement = treeEObjectIterator.next();
            this.countComponentAndConnectionsPerModel(nodeElement, dataOutput);
        }
        int components = (int) dataOutput.get(COMPONENTS);
        int connectors = (int) dataOutput.get(CONNECTORS);
        dataOutput.put(SIZE, connectors + components);

        this.getMetrics(dataOutput);

        return dataOutput;
    }

    public void countComponentAndConnectionsPerModel(EObject nodeElement, Map<String, Object> dataOutput) {
        String[] hardwareCategoryLabels = new String[]{"device", "memory", "bus", "processor"};
        String[] softwareCategoryLabels = new String[]{"process", "thread", "subprogram", "thread group", "subprogram group", "virtual processor", "virtual bus"};

        if (nodeElement instanceof ComponentInstance) {
            dataOutput.put(COMPONENTS, (Integer) dataOutput.get(COMPONENTS) + 1);
        }
        if (nodeElement instanceof ConnectionInstance) {
            dataOutput.put(CONNECTORS, (Integer) dataOutput.get(CONNECTORS) + 1);
        }
        if (nodeElement instanceof ComponentInstance) {
            String categoryName = ((ComponentInstance) nodeElement).getCategory().getName();
            if (Arrays.asList(hardwareCategoryLabels).contains(categoryName)) {
                dataOutput.put(NO_HARDWARE, (Integer) dataOutput.get(NO_HARDWARE) + 1);
            }

            if (Arrays.asList(softwareCategoryLabels).contains(categoryName)) {
                dataOutput.put(NO_SOFTWARE, (Integer) dataOutput.get(NO_SOFTWARE) + 1);
            }

            if (categoryName.equals("data")) {
                dataOutput.put(NO_DATA_STORAGE, (Integer) dataOutput.get(NO_DATA_STORAGE) + 1);
            }

            if (categoryName.equals("system")) {
                dataOutput.put(NO_SYS, (Integer) dataOutput.get(NO_SYS) + 1);
            }
        }

    }

    public void getMetrics(Map<String, Object> dataOutput) {
        float n = (int) dataOutput.get(COMPONENTS);
        float c = (int) dataOutput.get(CONNECTORS);
        float udy = 0;
        if (n > 1) {
            udy = c / (n * (n - 1));
        }
        dataOutput.put(UDY, udy);
    }

    /////////////////////////////////////////////////// IMPORTANT FUNCTIONS FOR PERFORMING QUERIES /////////////////////////////////////////
    private List<EObject> getComponents(Resource resource) {
        List<EObject> result = new ArrayList<>();
        TreeIterator<EObject> treeEObjectIterator = resource.getAllContents();
        while (treeEObjectIterator.hasNext()) {
            EObject nodeElement = treeEObjectIterator.next();
            if (nodeElement instanceof ComponentInstance)
                result.add(nodeElement);
        }
        return result;
    }
    private List<EObject> getConnections(Resource resource) {
        List<EObject> result = new ArrayList<>();
        TreeIterator<EObject> treeEObjectIterator = resource.getAllContents();
        while (treeEObjectIterator.hasNext()) {
            EObject nodeElement = treeEObjectIterator.next();
            if (nodeElement instanceof ConnectionInstance)
                result.add(nodeElement);
        }
        return result;
    }
}
