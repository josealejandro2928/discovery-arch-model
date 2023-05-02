package org.process.models.xmi;

import com.opencsv.CSVWriter;
import org.apache.commons.collections.map.LinkedMap;
import org.discover.arch.model.Config;
import org.discover.arch.model.SearchFileTraversal;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class EcoreModelHandler {
    private List<String> uriModels;
    private String rootPathFolder;
    private final List<String> modelExtension;
    private Map<String, Map<String, Object>> processedDataFromModel;
    private List<String> qualityMetricsComputed;
    private Config configObj = null;

    public EcoreModelHandler(Config configObj) {
        this.configObj = configObj;
        this.uriModels = new ArrayList<>();
        this.modelExtension = Arrays.asList("xml", "xmi", "ecore", "aaxl2");
        this.processedDataFromModel = new HashMap<>();
        this.rootPathFolder = Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolderName(), "xmi").toString();
        this.qualityMetricsComputed = new ArrayList<>();
    }


    public List<String> getUriModels() {
        return uriModels;
    }

    public void setRootPathFolder(String rootPathFolder) {
        this.rootPathFolder = rootPathFolder;
    }

    public void discoverModelFromPath() throws Exception {
        if (rootPathFolder == null)
            throw new Exception("There is not root path for reading the XMI models");
        File rootPathFolderFile = Paths.get(rootPathFolder).toFile();
        if (!rootPathFolderFile.exists())
            throw new Exception("The path to get the xmi converted files does not exist: " + rootPathFolderFile);
        if (!rootPathFolderFile.isDirectory())
            throw new Exception("The file to process the xmi converted models must be a directory");
        this.uriModels = new ArrayList<>();
        Files.walk(Path.of(rootPathFolder)).sorted().map(Path::toFile).forEach(
                (File file) -> {
                    if (file.isFile()) {
                        String uriModel = file.getPath();
                        String ext = SearchFileTraversal.getExtension(uriModel);
                        if (this.modelExtension.contains(ext))
                            this.uriModels.add(file.getPath());
                    }
                });
    }

    public void processModels(QueryModel eolBasedModelQuery, QueryModel javaBasedModelQuery) {
        System.out.println("PARSING AND GETTING THE ECORE OBJECT FROM MODELS XMI");
        int indexFile = 1;
        long startTime = System.nanoTime();
        for (String modelUri : this.uriModels) {
            try {
                System.out.println("------------------------------------------------------------------");

                System.out.println("\033[0;33m" + indexFile + "/" + this.uriModels.size() + " URI: " + modelUri + "\033[0m");
                Map<String, Object> data = new LinkedHashMap<>(eolBasedModelQuery.run(modelUri));
                data = javaBasedModelQuery.run(modelUri, data);
                System.out.println("Data: " + data);
//                data.put("uri", modelUri);
                this.processedDataFromModel.put(modelUri, data);
                System.out.println("------------------------------------------------------------------");
            } catch (Exception e) {
                System.err.println("Error performing query over the model: " + e.getMessage());
                e.printStackTrace();
            }
            indexFile++;
        }
        long endTime = System.nanoTime();
        double elapsedTime = (double) (endTime - startTime) / 1000000000;
        System.out.println("\033[0;32m" + "ELAPSED TIME: " + new DecimalFormat("0.000").format(elapsedTime) + "s" + "\033[0m");
        if (this.processedDataFromModel.size() == 0) return;
        Map<String, Object> itemMetricsObject = this.processedDataFromModel.values().stream().toList().get(0);
        this.qualityMetricsComputed = new ArrayList<>(itemMetricsObject.keySet());
//        Collections.sort(this.qualityMetricsComputed);
        System.out.println("GETTING METRICS DYNAMICALLY: " + this.qualityMetricsComputed);

    }

    public Map<String, Map<String, Object>> getProcessedDataFromModel() {
        return processedDataFromModel;
    }

    public void generateCSVFileFromProcessedModels(String name) {
        try {
            File file = Paths.get(this.configObj.getRootPath(), name + ".csv").toFile();
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            ////////// INITIALIZING THE HEADER OF THE CSV ////////////////////////////////////
            List<String> header = new ArrayList<>(Arrays.asList("model_name", "src_path", "conv_path", "src_ext", "is_parsed", "is_sys_design", "sys_name"));
            header.addAll(this.qualityMetricsComputed.stream().filter((String x)->!header.contains(x)).toList());
            header.add("doc_files");
            //////////////////////////////////////////////////////////////////////////////////
            List<Map<String, Object>> dataSource = this.createDataSource();
            writer.writeNext(header.toArray(new String[]{}));
            for (Map<String, Object> elementData : dataSource) {
                String[] row = new String[header.size()];
                int index = 0;
                for (String key : header) {
                    row[index++] = elementData.get(key) + "";
                }
                writer.writeNext(row);
            }
            writer.close();
            System.out.println("GENERATED CSV SUCCESSFULLY: " + file.getAbsolutePath());
        } catch (Exception error) {
            System.out.println("Error creating a csv file");
            error.printStackTrace();
        }

        this.generateLegends(configObj);

    }

    List<Map<String, Object>> createDataSource() {
        List<Map<String, Object>> conversionLogs = this.configObj.getConversionLogs();
        return conversionLogs.stream().map(conversionLogModel -> {
            String uriToConvertedModel = (String) conversionLogModel.get("pathXMLFile");
            Map<String, Object> preData = new HashMap<>();
            preData.put("model_name", conversionLogModel.get("modelName"));
            preData.put("src_path", conversionLogModel.get("pathAADLFile"));
            preData.put("conv_path", uriToConvertedModel);
            preData.put("src_ext", conversionLogModel.get("extension"));
            preData.put("is_parsed", conversionLogModel.get("isParsingSucceeded"));
            preData.put("is_sys_design", conversionLogModel.get("isSavedTheModel"));

            if (this.processedDataFromModel.containsKey(uriToConvertedModel)) {
                Map<String, Object> processedData = this.processedDataFromModel.get(uriToConvertedModel);
                for (String key : processedData.keySet()) {
                    preData.put(key, processedData.getOrDefault(key, null));
                }
            }
            preData.put("doc_files", String.join(", ", (Collection<String>)conversionLogModel.getOrDefault("docFiles",null)));
            return preData;
        }).collect(Collectors.toList());
    }

    void generateLegends(Config configObj) {
        try {
            File file = Paths.get(configObj.getRootPath(), "legends.csv").toFile();
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            List<String[]> dataSource = new ArrayList<>();
            dataSource.add(new String[]{"model_name", "The name of the model resulting of parsing the file .aadl"});
            dataSource.add(new String[]{"src_path", "The global path where the model[source] was found in the file system"});
            dataSource.add(new String[]{"conv_path", "The global path of the instantiated model converted to XML"});
            dataSource.add(new String[]{"src_ext", "The extension of the model[source] ex: .aadl "});
            dataSource.add(new String[]{"is_parsed", "A boolean value that show if the model could be parsed or not, if it wasn't, model could be broken"});
            dataSource.add(new String[]{"is_sys_design", "A boolean value that show if in the model[source] there was found a SystemInstance model"});
            dataSource.add(new String[]{"sys_name", "The name of the SystemInstance model found"});
            dataSource.add(new String[]{"num_comp", "The number of component of the model: ComponentInstance found"});
            dataSource.add(new String[]{"num_conn", "The number of connections of the model: ConnectionInstance found"});
            dataSource.add(new String[]{"size", "The sum of components and connection"});
            dataSource.add(new String[]{"udy", "The Understandability: The number of connections between components divided by N^2-N, " +
                    "where N is the number of components"});
            dataSource.add(new String[]{"no_hardware_comp", "The number of component which belong to " +
                    "the category of [\"device\",\"memory\",\"bus\",\"processor\"]"});
            dataSource.add(new String[]{"no_software_comp", "The number of component which belong to the " +
                    "category of [\"process\",\"thread\",\"subprogram\",\"threadGroup\",\"subprogramGroup\"]"});
            dataSource.add(new String[]{"coupling", "Sum for every component of the number of in_features divided by the (out_features + in_features) : Features are connection to other components"});
            dataSource.add(new String[]{"cohesion", "The computation of cohesion its returned as: e / (n(n-1))/2 where e and n are connections and components respectively"});
            dataSource.add(new String[]{"complexity", "The sum for every component of in_features + out_features"});
            dataSource.add(new String[]{"graph_density", "The graph density is the ratio e / n, where e and n are connections and nodes respectively"});
            dataSource.add(new String[]{"graph_str_rep", "The graph string representation of the models"});
            dataSource.add(new String[]{"avg_shortest_path", "The distance measure of a source to all other reachable destinations of a complex network used to model the software architecture"});
            dataSource.add(new String[]{"avg_deg_cent", "The degree centrality of the graph based in components and connections"});

            String[] header = {"column", "description"};
            writer.writeNext(header);
            for (String[] row : dataSource) {
                writer.writeNext(row);
            }
            writer.close();
            dataSource = null;
            System.out.println("GENERATED LEGEND SUCCESSFULLY: " + file.getAbsolutePath());
        } catch (Exception error) {
            System.out.println("Error creating a csv file");
            error.printStackTrace();
        }

    }

    public void clear() {
        this.processedDataFromModel = null;
        this.configObj = null;
        this.uriModels = null;
    }
}


