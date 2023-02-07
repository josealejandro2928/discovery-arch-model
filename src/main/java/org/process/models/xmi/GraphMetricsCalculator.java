package org.process.models.xmi;

import org.eclipse.emf.ecore.resource.Resource;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionReference;
import org.osate.aadl2.instance.SystemInstance;

import java.util.*;
import java.util.function.Function;

public class GraphMetricsCalculator {
    private Map<ComponentInstance, List<ComponentInstance>> graph = new HashMap<>();
    Resource resourceModel;

    GraphMetricsCalculator(Resource resourceModel) {
        this.resourceModel = resourceModel;
        this.init();
    }

    private void init() {
        this.graph = new HashMap<>();
        SystemInstance rootSysInstance = (SystemInstance) resourceModel.getContents().get(0);
        List<ConnectionInstance> connectionInstances = rootSysInstance.getAllConnectionInstances();
        List<ComponentInstance> componentInstances = rootSysInstance.getAllComponentInstances().stream().
                filter((ComponentInstance x) -> x != rootSysInstance).toList();
        componentInstances.forEach((c) -> this.graph.put((c), new ArrayList<>()));


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
        List<ComponentInstance> components = this.graph.keySet().stream().toList();
        var n = components.size();
        var total = 0;
        for (ComponentInstance c : components) {
            var distanceMap = this.shortestPath(c);
            total += distanceMap.values().stream().reduce(0, (acc, curr) -> {
                if (curr > -1) acc += curr;
                return acc;
            });
        }
        return (double) total / ((double) n * (n - 1));
    }

    public String getGraphTokens() {
        List<ComponentInstance> components = new ArrayList<>(this.graph.keySet().stream().toList());
        components.sort(Comparator.comparing(NamedElement::getName));
        String componentsParts = String.join("; ", components.stream()
                .map((ComponentInstance c) -> c.getName() + ":" + c.getCategory().getName()).toList());

        String connectionParts = "";
        for (ComponentInstance component : components) {
            List<ComponentInstance> neighbors = this.graph.get(component);
            if (neighbors == null) continue;
            for (ComponentInstance neigh : neighbors) {
                connectionParts += "[" + component.getName() + ":" + component.getCategory().getName() + " -> " + neigh.getName() + ":" + neigh.getCategory().getName() + "]; ";
            }
        }
        connectionParts = connectionParts.substring(0, connectionParts.length() - 2);
        return componentsParts + "\n" + connectionParts;
    }

    private Map<ComponentInstance, Integer> shortestPath(ComponentInstance src) {
        Queue<ComponentInstance> queue = new LinkedList<>();
        Map<ComponentInstance, Integer> dist = new HashMap<>();
        this.graph.keySet().forEach((c) -> dist.put(c, -1));
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
