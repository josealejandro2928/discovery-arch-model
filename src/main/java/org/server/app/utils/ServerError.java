package org.server.app.utils;

import java.io.IOException;
import java.util.*;

public class ServerError extends IOException {
    int code;
    List<Object> meta;

    public ServerError(int code, String message, List<Object> meta) {
        super(message);
        this.code = code;
        this.meta = meta == null ? new ArrayList<>() : meta;
    }

    ServerError(Exception e) {
        super(e.getMessage());
        int code = 500;
        List<Object> meta = new ArrayList<>();
        if (e instanceof ServerError) {
            code = ((ServerError) e).getCode();
            meta = ((ServerError) e).getMeta();
        }
        this.meta = meta;
        this.code = code;
    }

    ServerError() {
        super("Error in the server");
        this.code = 500;
        this.meta = new ArrayList<>();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<Object> getMeta() {
        return meta;
    }

    public void setMeta(List<Object> meta) {
        this.meta = meta;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", this.getCode());
        data.put("message", this.getMessage());
        data.put("meta", this.getMeta());
        return data;
    }
}
