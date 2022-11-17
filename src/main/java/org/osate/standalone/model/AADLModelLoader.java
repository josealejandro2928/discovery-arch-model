package org.osate.standalone.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AADLModelLoader {
    public OutputLoadedModelSchema loadAddlModel(String pathAADLFile, String pathXMLFile, String id, boolean verbose) throws Exception;

    public class OutputLoadedModelSchema {
        List<String> errors;
        String pathXMLFile;
        String pathAADLFile;
        String modelName;
        boolean isParsingSucceeded;
        boolean isSavedTheModel;

        OutputLoadedModelSchema() {
            this.errors = new ArrayList<String>();
            this.pathAADLFile = "";
            this.pathXMLFile = "";
            this.modelName = "";
            this.isParsingSucceeded = true;
            this.isSavedTheModel = false;
        }

        @Override
        public String toString() {
            String src = "modelName: " + this.modelName + "\n" +
                    "isParsingSucceeded: " + this.isParsingSucceeded + "\n" +
                    "isSavedTheModel: " + this.isSavedTheModel + "\n" +
                    "pathAADLFile: " + this.pathAADLFile + "\n" +
                    "pathXMLFile: " + this.pathXMLFile + "\n" +
                    "errors: " + this.errors + "\n";
            return src;
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
