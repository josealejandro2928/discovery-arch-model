package org.osate.standalone.model;

import org.eclipse.xtext.xbase.lib.ObjectExtensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface RawModelLoader {
    public Object loadModel(String pathAADLFile, String pathXMLFile, String id, boolean verbose) throws Exception;

    public class OutputLoadedModelSchema {
        List<String> errors;
        String pathXMLFile;
        String pathAADLFile;
        String modelName;
        boolean isParsingSucceeded;
        boolean isSavedTheModel;

        OutputLoadedModelSchema() {
            this.errors = new ArrayList<>();
            this.pathAADLFile = "";
            this.pathXMLFile = "";
            this.modelName = "";
            this.isParsingSucceeded = true;
            this.isSavedTheModel = false;
        }
        OutputLoadedModelSchema(OutputLoadedModelSchema o) {
            this.errors = o.errors;
            this.pathAADLFile = o.pathAADLFile;
            this.modelName = o.modelName;
            this.pathXMLFile = "";
            this.isParsingSucceeded = true;
            this.isSavedTheModel = false;
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
            data.put("modelName", this.modelName);
            data.put("isParsingSucceeded", this.isParsingSucceeded);
            data.put("isSavedTheModel", this.isSavedTheModel);
            data.put("pathAADLFile", this.pathAADLFile);
            data.put("pathXMLFile", this.pathXMLFile);
            data.put("errors", this.errors);
            return data;
        }
    }
}
