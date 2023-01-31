package org.process.models.xmi;

import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.standalone.model.LoadXMIModel;

import java.util.*;

public class JavaQueryAADLModelInst implements QueryModel {
    private static JavaQueryAADLModelInst INSTANCE = null;
    public LoadXMIModel loadXMIModel = LoadXMIModel.getInstance();

    final String COUPLING = "coupling";
    final String COHESION = "cohesion";

    private JavaQueryAADLModelInst() {
    }

    static public JavaQueryAADLModelInst getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        INSTANCE = new JavaQueryAADLModelInst();
        return INSTANCE;
    }

    @Override
    public Map<String, Object> run(String script, String modelPath) throws Exception {
        return this.run(modelPath);
    }

    @Override
    public Map<String, Object> run(String modelPath) throws Exception {
        return null;
    }

    @Override
    public Map<String, Object> run(String modelPath, Map<String, Object> data) throws Exception {
        Map<String, Object> dataOutput = data;
        if (data == null) {
            dataOutput = new HashMap<>();
        }
        dataOutput.put(COUPLING, 0);

        Resource resourceModel = this.loadXMIModel.getResourceObjectFromXMIModel(modelPath);
        if (resourceModel == null) {
            throw new Exception("Error loading the resource for the given: " + modelPath);
        }

        computeCouplingAndCohesion(resourceModel, dataOutput);

        return dataOutput;
    }

    /**
     * @param resourceModel The models load in memory
     *                      The computation of coupling is made as: 1 - (1 / no_of_comp)
     *                      The computation of cohesion its returned as: e / (n(n-1))/2
     */
    public void computeCouplingAndCohesion(Resource resourceModel, Map<String, Object> data) {
        SystemInstance sys = (SystemInstance) resourceModel.getContents().get(0);
        List<ComponentInstance> componentInstances = sys.getAllComponentInstances().stream().filter((ComponentInstance x) -> x != sys).toList();
        List<ConnectionInstance> connectionInstances = sys.getAllConnectionInstances();
        double coupling = 1 - (1 / (double) componentInstances.size());
        double n = componentInstances.size();
        double e = connectionInstances.size();
        double totalE = (n * (n - 1)) / 2;
        double cohesion = e / totalE;

        data.put(this.COUPLING, coupling);
        data.put(this.COHESION, cohesion);
    }
}
