package org.process.models.xmi;

import java.util.Map;

public interface QueryModel {
    Map<String, Object> run(String script, String modelPath) throws Exception;

    Map<String, Object> run(String modelPath) throws Exception;

    Map<String,Object> run(String modelPath, Map<String, Object> data) throws Exception;
}
