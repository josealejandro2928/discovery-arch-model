package org.server.app.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.morphia.Datastore;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.MongoDbConnection;
import org.server.app.data.UserModel;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.ServerError;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ReportsHandler {
    private final CustomMapMapper objectMapper = new CustomMapMapper();

    public HttpHandler reportHandler = exchange -> {
        String method = exchange.getRequestMethod();
        if (method.equals("GET")) {
            try {
                listModelRepo(exchange);
            } catch (CsvException e) {
                throw new ServerError(500, e.getMessage());
            }
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }
    };

    void listModelRepo(HttpExchange exchange) throws IOException, CsvException {
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        if (configUser == null) throw new ServerError(400, "You dont have any space here");

        Map<String, Object> dataOutput = new HashMap<>();
        dataOutput.put("message", "Ok");
        dataOutput.put("result", this.getResultCSV(configUser));
        dataOutput.put("conv-logs", this.getConvLogs(configUser));
        dataOutput.put("legends", this.getLegendCSV(configUser));

        String response = objectMapper.writeValueAsString(dataOutput);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    List<Map<String, Object>> getResultCSV(ConfigUserModel configUser) throws IOException, CsvException {
        List<Map<String, Object>> result = new ArrayList<>();
        File fileReport = new File(Paths.get(configUser.getRootPath(), "results.csv").toAbsolutePath().toString());
        if (!fileReport.exists()) return result;
        FileReader fileReader = new FileReader(fileReport);
        CSVReader csvReader = new CSVReader(fileReader);
        String[] headerColumns = new String[]{};
        List<String[]> dataCSV = csvReader.readAll();
        for (var i = 0; i < dataCSV.size(); i++) {
            if (i == 0) {
                headerColumns = dataCSV.get(i);
            } else {
                Map<String, Object> rowMap = new HashMap<>();
                for (var j = 0; j < headerColumns.length; j++) {
                    rowMap.put(headerColumns[j], dataCSV.get(i)[j]);
                }
                result.add(rowMap);
            }
        }
        return result;
    }

    List<Map<String, Object>> getLegendCSV(ConfigUserModel configUser) throws IOException, CsvException {
        List<Map<String, Object>> result = new ArrayList<>();
        File fileReport = new File(Paths.get(configUser.getRootPath(), "legends.csv").toAbsolutePath().toString());
        if (!fileReport.exists()) return result;
        FileReader fileReader = new FileReader(fileReport);
        CSVReader csvReader = new CSVReader(fileReader);
        String[] headerColumns = new String[]{};
        List<String[]> dataCSV = csvReader.readAll();
        for (var i = 0; i < dataCSV.size(); i++) {
            if (i == 0) {
                headerColumns = dataCSV.get(i);
            } else {
                Map<String, Object> rowMap = new HashMap<>();
                for (var j = 0; j < headerColumns.length; j++) {
                    rowMap.put(headerColumns[j], dataCSV.get(i)[j]);
                }
                result.add(rowMap);
            }
        }
        return result;
    }


    List<Map<String, Object>> getConvLogs(ConfigUserModel configUser) throws IOException, CsvException {
        List<Map<String, Object>> result = new ArrayList<>();
        File convLogsFile = new File(Paths.get(configUser.getRootPath(), configUser.getOutputFolderName(),
                "conversion-logs.json").toAbsolutePath().toString());
        if (!convLogsFile.exists()) return result;
        String read = String.join("\n", Files.readAllLines(convLogsFile.toPath()));
        result = this.objectMapper.readValue(read, new TypeReference<>() {
        });
        return result;
    }
}
