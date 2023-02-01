package org.process.models.xmi;

import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.FeatureInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.standalone.model.LoadXMIModel;

import java.util.*;

public class JavaQueryAADLModelInst implements QueryModel {
    private static JavaQueryAADLModelInst INSTANCE = null;
    public LoadXMIModel loadXMIModel = LoadXMIModel.getInstance();

    final String COUPLING = "coupling";
    final String COHESION = "cohesion";
    final String COMPLEXITY = "complexity";
    final String GRAPH_DENSITY = "graph_density";

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

        computeStructuralMetrics(resourceModel, dataOutput);

        return dataOutput;
    }

    /**
     * @param resourceModel The models load in memory
     *                      The computation of coupling is made as: 1 - (1 / no_of_comp)
     *                      The computation of cohesion its returned as: e / (n(n-1))/2
     *                      The computation of complexity, for every component Sum over all components c(i) = c_input(i) * c_output(i)
     */
    public void computeStructuralMetrics(Resource resourceModel, Map<String, Object> data) {
        SystemInstance sys = (SystemInstance) resourceModel.getContents().get(0);
        List<ComponentInstance> componentInstances = sys.getAllComponentInstances().stream().filter((ComponentInstance x) -> x != sys).toList();
        List<ConnectionInstance> connectionInstances = sys.getAllConnectionInstances();

        ///////////////////// Coupling //////////////////////////////
        double coupling = 0;
        for (ComponentInstance c : componentInstances) {
            List<FeatureInstance> portFeaturePerComponents = c.getAllFeatureInstances();
            long in_features = portFeaturePerComponents.stream().filter((FeatureInstance fe) -> fe.getDirection().incoming()).count();
            long out_features = portFeaturePerComponents.stream().filter((FeatureInstance fe) -> fe.getDirection().outgoing()).count();
            if (in_features + out_features > 0) {
                coupling += (float) in_features / ((float) out_features + (float) in_features);
            }
        }
        data.put(this.COUPLING, coupling);

        ///////////////////// Cohesion //////////////////////////////
        double n = componentInstances.size();
        double e = connectionInstances.size();
        double totalE = (n * (n - 1)) / 2;
        double cohesion = e / totalE;
        data.put(this.COHESION, cohesion);

        ///////////////////// Computing Complexity //////////////////////////////
        int complexity = 0;
        for (ComponentInstance c : componentInstances) {
            List<FeatureInstance> portFeaturePerComponents = c.getAllFeatureInstances();
            long in_features = portFeaturePerComponents.stream().filter((FeatureInstance fe) -> fe.getDirection().incoming()).count();
            long out_features = portFeaturePerComponents.stream().filter((FeatureInstance fe) -> fe.getDirection().outgoing()).count();
            complexity += in_features * out_features;
        }
        data.put(this.COMPLEXITY, complexity);

        ///////////////////// Computing Graph Density //////////////////////////////
        double graph_density = 0;
        if (n > 0) {
            graph_density = (float) e / (float) n;
        }
        data.put(this.GRAPH_DENSITY, graph_density);

    }
}
