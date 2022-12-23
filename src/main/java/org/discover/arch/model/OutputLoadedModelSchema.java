package org.discover.arch.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputLoadedModelSchema {
    public List<Object> errors;
    public String pathXMLFile;
    public String pathAADLFile;
    public String modelName;
    public boolean isParsingSucceeded;
    public boolean isSavedTheModel;

    public OutputLoadedModelSchema() {
        this.errors = new ArrayList<>();
        this.pathAADLFile = "";
        this.pathXMLFile = "";
        this.modelName = "";
        this.isParsingSucceeded = true;
        this.isSavedTheModel = false;
    }

    public OutputLoadedModelSchema(OutputLoadedModelSchema o) {
        this.errors = o.errors;
        this.pathAADLFile = o.pathAADLFile;
        this.modelName = o.modelName;
        this.pathXMLFile = "";
        this.isParsingSucceeded = true;
        this.isSavedTheModel = false;
    }

    public List<Object> getErrors(boolean filtered) {
        return this.errors.stream().filter((Object x) -> {
            if (!filtered)
                return true;
            String msg = x.toString();
            return !msg.contains("Error executing EValidator");
        }).toList();
    }

    @Override
    public String toString() {
        return "modelName: " + this.modelName + "\n" +
                "isParsingSucceeded: " + this.isParsingSucceeded + "\n" +
                "isSavedTheModel: " + this.isSavedTheModel + "\n" +
                "pathAADLFile: " + this.pathAADLFile + "\n" +
                "pathXMLFile: " + this.pathXMLFile + "\n" +
                "errors: " + this.errors + "\n";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        List<String> x = this.getErrors(true).stream().map(Object::toString).toList();
        data.put("modelName", this.modelName);
        data.put("isParsingSucceeded", this.isParsingSucceeded);
        data.put("isSavedTheModel", this.isSavedTheModel);
        data.put("pathAADLFile", this.pathAADLFile);
        data.put("pathXMLFile", this.pathXMLFile);
        data.put("errors", x);
        return data;
    }
}
