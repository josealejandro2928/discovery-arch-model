package org.process.models.xmi;

import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.instance.impl.ComponentInstanceImpl;
import org.osate.aadl2.instance.impl.ConnectionInstanceImpl;

import java.util.ArrayList;
import java.util.List;

public class DataRepresentationFeature {
    private String systemName;
    private int componentsNumber = 0;
    private int connectionsNumber = 0;
    private List<ComponentInstanceImpl> components = new ArrayList<>();
    private List<ConnectionInstanceImpl> connections = new ArrayList<>();

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setComponentsNumber(int componentsNumber) {
        this.componentsNumber = componentsNumber;
    }

    public int getComponentsNumber() {
        return componentsNumber;
    }

    public void setComponents(List<ComponentInstanceImpl> components) {
        this.components = components;
    }

    public List<ComponentInstanceImpl> getComponents() {
        return components;
    }

    public void setConnections(List<ConnectionInstanceImpl> connections) {
        this.connections = connections;
    }

    public List<ConnectionInstanceImpl> getConnections() {
        return connections;
    }

    public int getConnectionsNumber() {
        return connectionsNumber;
    }

    public void setConnectionsNumber(int connectionsNumber) {
        this.connectionsNumber = connectionsNumber;
    }

    @Override
    public String toString() {
        String result = "NAME: " + systemName + "; ";
        result += "#components: " + componentsNumber + "; ";
        result += "#connections: " + connectionsNumber + ";";
        result += "\nCOMPONENTS:\n";
        result += String.join("\n", this.components.stream().map(ComponentInstanceImpl::toString).toList());
        result += "\nCONNECTIONS:\n";
        result += String.join("\n", this.connections.stream().map(ConnectionInstanceImpl::toString).toList());
        return result;
    }
}
