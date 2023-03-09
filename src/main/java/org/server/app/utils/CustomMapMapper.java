package org.server.app.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class CustomMapMapper extends ObjectMapper {
    static CustomMapMapper instance = null;

    private CustomMapMapper() {
        super();
    }

    public static CustomMapMapper getInstance() {
        if (instance != null) return instance;
        instance = new CustomMapMapper();
        return instance;
    }

    public Map<String, Object> getBodyAsMap(String content) {
        try {
            return super.readValue(content, new TypeReference<>() {
            });
        } catch (IOException ioe) {
            throw new CompletionException(ioe);
        }

    }
}
