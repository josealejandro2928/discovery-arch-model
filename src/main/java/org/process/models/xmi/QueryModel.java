package org.process.models.xmi;

public interface QueryModel {
    public Object run(String script, String modelPath) throws Exception;

    public Object run(String modelPath) throws Exception;
}
