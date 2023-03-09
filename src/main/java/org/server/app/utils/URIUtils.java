package org.server.app.utils;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class URIUtils {
    public static Map<String, String> getQuery(URI url) throws IOException {
        String query = url.getQuery();
        Map <String,String> queryMap = new HashMap<>();
        if(query == null) return queryMap;
        String [] parts = query.split("&");
        for(String part:parts){
            String[] op = part.split("=");
            if(op.length > 1){
                queryMap.put(op[0].trim(),op[1].trim());
            }
        }
        return queryMap;
    }
}
