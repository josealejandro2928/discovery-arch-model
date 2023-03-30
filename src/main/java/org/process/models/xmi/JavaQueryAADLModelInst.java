package org.process.models.xmi;

import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.instance.*;
import org.osate.standalone.model.LoadXMIModel;

import java.util.*;
import java.util.function.Function;

public class JavaQueryAADLModelInst implements QueryModel {
    private static JavaQueryAADLModelInst INSTANCE = null;
    public LoadXMIModel loadXMIModel = LoadXMIModel.getInstance();

    final String COUPLING = "coupling";
    final String COHESION = "cohesion";
    final String COMPLEXITY = "complexity";
    final String GRAPH_DENSITY = "graph_density";
    final String GRAPH_STR_REPRESENTATION = "graph_str_rep";
    final String AVG_SHORTEST_PATH = "avg_shortest_path";
    final String AVG_CLUSTERING_COEFFICIENT = "avg_clust_coeff";
    final String AVG_DEGREE_CENTRALITY = "avg_deg_cent";

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

        GraphMetricsCalculator graphModelMetricsCalculator = new GraphMetricsCalculator(resourceModel);
        computeStructuralMetrics(resourceModel, dataOutput);
//        dataOutput.put(this.GRAPH_STR_REPRESENTATION, graphModelMetricsCalculator.getGraphTokens());
        dataOutput.put(this.AVG_SHORTEST_PATH, graphModelMetricsCalculator.getAvgShortestPath());
//        dataOutput.put(this.AVG_CLUSTERING_COEFFICIENT, graphModelMetricsCalculator.getAvgClusteringCoefficient(100));
        dataOutput.put(this.AVG_DEGREE_CENTRALITY, graphModelMetricsCalculator.getDegreeCentrality());

        return dataOutput;
    }

    /**
     * @param resourceModel The models load in memory
     *                      The computation of coupling is made as: in_feature / in_feature + out_feature
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
        double n = (float) componentInstances.size();
        double e = (float) connectionInstances.size();
        double totalE = (n * (n - 1)) / 2;
        double cohesion = 0;
        if (totalE > 0.0001)
            cohesion = e / totalE;

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
