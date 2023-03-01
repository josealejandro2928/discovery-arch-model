package org.process.models.xmi;

import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionReference;
import org.osate.aadl2.instance.SystemInstance;

import java.util.*;

public class GraphMetricsCalculator {
    private Map<ComponentInstance, Set<ComponentInstance>> graph = new HashMap<>();
    Resource resourceModel;
    List<ComponentInstance> components;

    GraphMetricsCalculator(Resource resourceModel) {
        this.resourceModel = resourceModel;
        this.init();
    }

    private void init() {
        this.graph = new HashMap<>();
        SystemInstance rootSysInstance = (SystemInstance) resourceModel.getContents().get(0);
        List<ConnectionInstance> connectionInstances = rootSysInstance.getAllConnectionInstances();
        this.components = rootSysInstance.getAllComponentInstances();
        this.components.forEach((c) -> this.graph.put((c), new HashSet<>()));


        for (ConnectionInstance connectionInstance : connectionInstances) {
            for (ConnectionReference cr : connectionInstance.getConnectionReferences()) {
                ComponentInstance src = cr.getSource().getComponentInstance();
                ComponentInstance dst = cr.getDestination().getComponentInstance();
                if (src.equals(rootSysInstance)) continue;
                this.graph.get(src).add(dst);
            }
        }
    }

    double getAvgShortestPath() {
        var n = this.components.size();
        var total = 0;
        for (ComponentInstance c : this.components) {
            var distanceMap = this.shortestPath(c);
            total += distanceMap.values().stream().reduce(0, (acc, curr) -> {
                if (curr > -1) acc += curr;
                return acc;
            });
        }
        int den = (n * (n - 1));
        if (den > 0) {
            return (double) total / (double) den;
        }
        return 0;
    }

    double getAvgClusteringCoefficient(int trials) {
        Random random = new Random();
        double avgClustCoef = 0.0;
        for (int i = 0; i < trials; i++) {
            int index = random.nextInt(this.components.size());
            ComponentInstance pickedComponent = this.components.get(index);
            List<ComponentInstance> neighbors = this.graph.get(pickedComponent).stream().toList();
            if (neighbors.size() < 2) continue;
            var c1 = neighbors.get(random.nextInt(neighbors.size()));
            var c2 = neighbors.get(random.nextInt(neighbors.size()));
            while (c1.equals(c2)) {
                c2 = neighbors.get(random.nextInt(neighbors.size()));
            }
            if (graph.get(c1).contains(c2) || graph.get(c2).contains(c1)) {
                avgClustCoef++;
            }
        }
        return avgClustCoef / trials;
    }

    double getDegreeCentrality() {
        double n = this.components.size();
        double den = (double) n * (n - 1);
        double num = this.graph.keySet().stream().map((var c) -> this.graph.get(c).size()).reduce(0, Integer::sum);
        if (den > 0) {
            return num / den;
        }
        return 0;
    }


    public String getGraphTokens() {
        List<ComponentInstance> components = new ArrayList<>(this.components);
        components.sort(Comparator.comparing(NamedElement::getName));
        String componentsParts = String.join("; ", components.stream()
                .map((ComponentInstance c) -> c.getName() + ":" + c.getCategory().getName()).toList());

        String connectionParts = "";
        for (ComponentInstance component : components) {
            Set<ComponentInstance> neighbors = this.graph.get(component);
            if (neighbors == null) continue;
            for (ComponentInstance neigh : neighbors) {
                connectionParts += "[" + component.getName() + ":" + component.getCategory().getName() + " -> " + neigh.getName() + ":" + neigh.getCategory().getName() + "]; ";
            }
        }
        if (connectionParts.length() >= 2) {
            connectionParts = connectionParts.substring(0, connectionParts.length() - 2);
        }
        return componentsParts + "\n" + connectionParts;
    }

    private Map<ComponentInstance, Integer> shortestPath(ComponentInstance src) {
        Queue<ComponentInstance> queue = new LinkedList<>();
        Map<ComponentInstance, Integer> dist = new HashMap<>();
        this.components.forEach((c) -> dist.put(c, -1));
        queue.add(src);
        dist.put(src, 0);
        while (queue.size() > 0) {
            var n = queue.poll();
            var neighbors = this.graph.get(n);
            if (neighbors == null) continue;
            for (var w : neighbors) {
                if (dist.get(w).equals(-1)) {
                    queue.add(w);
                    dist.put(w, dist.get(n) + 1);
                }
            }
        }
        return dist;
    }


}
