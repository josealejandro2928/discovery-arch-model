package org.process.models.xmi;

import com.opencsv.CSVWriter;
import org.discover.arch.model.Config;
import org.discover.arch.model.SearchFileTraversal;
import org.eclipse.emf.ecore.resource.Resource;


import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class EcoreModelHandler {
    static private EcoreModelHandler INSTANCE = null;
    private List<String> uriModels;
    private String rootPathFolder;
    private final List<String> modelExtension;
    private Map<String, Map<String, Object>> processedDataFromModel;

    private EcoreModelHandler() {
        this.uriModels = new ArrayList<>();
        this.modelExtension = Arrays.asList("xml", "xmi", "ecore", "aaxl2");
        this.processedDataFromModel = new HashMap<>();

    }

    static EcoreModelHandler getInstance() {
        if (INSTANCE != null)
            return INSTANCE;
        else
            INSTANCE = new EcoreModelHandler();
        return INSTANCE;
    }


    public List<String> getUriModels() {
        return uriModels;
    }

    public void setRootPathFolder(String rootPathFolder) {
        this.rootPathFolder = rootPathFolder;
    }

    void discoverModelFromPath() throws Exception {
        if (rootPathFolder == null)
            throw new Exception("There is not root path for reading the XMI models");
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

    void processModels(EcoreStandAlone ecoreStandAlone, EolRunner eolRunner) throws Exception {
        System.out.println("PARSING AND GETTING THE ECORE OBJECT FROM MODELS XMI");
        for (String modelUri : this.uriModels) {
            try {
                try {
                    System.out.println("------------------------------------------------------------------");
                    System.out.println("URI: " + modelUri);
                    Map<String, Object> data = (Map<String, Object>) eolRunner.run("main", modelUri);
                    data.put("uri", modelUri);
                    this.processedDataFromModel.put(modelUri, data);
                    System.out.println("------------------------------------------------------------------");
                } catch (Exception e) {
                    System.out.println("Error running eol: " + e.getMessage());
                }
            } catch (Exception e) {
                System.out.println("Error getting the models from URI: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Map<String, Map<String, Object>> getProcessedDataFromModel() {
        return processedDataFromModel;
    }

    void generateCSVFileFromProcessedModels(String name, Config configObj) {
        try {
            File file = Paths.get(configObj.getRootPath(), name + ".csv").toFile();
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            List<Map<String, Object>> dataSource = this.createDataSource(configObj);
            String[] header = {"id", "model_name", "src_path", "conv_path",
                    "src_ext", "is_parsed", "is_sys_design", "num_errors",
                    "sys_name", "num_comp", "num_conn", "size", "udy",
                    "no_hardware_comp", "no_software_comp", "no_data_comp"};
            writer.writeNext(header);
            for (Map elementData : dataSource) {
                String[] row = new String[header.length];
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

    List<Map<String, Object>> createDataSource(Config configObj) {
        /*
        { id:String
          model_name:String
          src_path:String
          conv_path:String
          src_ext:String
          is_parsed:boolean
          is_sys_design:boolean
          num_errors:int
          sys_name:String
          num_comp:int
          num_conn:int
          size:int
          no_hardware_comp:int
          no_software_comp:int
          no_data_comp:int
        }
        * */
        List<Map<String, Object>> dataSource;
        List<Map<String, Object>> conversionLogs = configObj.getConversionLogs();
        dataSource = conversionLogs.stream().map(x -> {
            String uriToConvertedModel = (String) x.get("pathXMLFile");
            Map<String, Object> preData = new HashMap<>();
            preData.put("id", x.get("id"));
            preData.put("model_name", x.get("modelName"));
            preData.put("src_path", x.get("pathAADLFile"));
            preData.put("conv_path", uriToConvertedModel);
            preData.put("src_ext", x.get("extension"));
            preData.put("is_parsed", x.get("isParsingSucceeded"));
            List<String> errors = (List<String>) (x.get("errors"));
            preData.put("is_sys_design", x.get("isSavedTheModel"));
            preData.put("num_errors", errors.size());
            preData.put("sys_name", null);
            preData.put("num_comp", 0);
            preData.put("num_conn", 0);
            preData.put("size", 0);
            preData.put("udy", 0);
            preData.put("no_hardware_comp", 0);
            preData.put("no_software_comp", 0);
            preData.put("no_data_comp", 0);
            if (this.processedDataFromModel.containsKey(uriToConvertedModel)) {
                Map<String, Object> processedData = this.processedDataFromModel.get(uriToConvertedModel);
                preData.put("sys_name", processedData.get("systemName"));
                preData.put("num_comp", processedData.get("components"));
                preData.put("num_conn", processedData.get("connectors"));
                preData.put("size", processedData.get("size"));
                preData.put("udy", processedData.get("udy"));
                preData.put("no_hardware_comp", processedData.get("no_hardware"));
                preData.put("no_software_comp", processedData.get("no_software"));
                preData.put("no_data_comp", processedData.get("no_data_storage"));
            }
            return preData;
        }).collect(Collectors.toList());
        return dataSource;
    }

    void generateLegends(Config configObj) {
        try {
            File file = Paths.get(configObj.getRootPath(), "legends.csv").toFile();
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            Map<String, String> dataSource = new HashMap<>();
            dataSource.put("id", "The index in the source was found in the file system");
            dataSource.put("model_name", "The name of the model resulting of parsing the file .aadl");
            dataSource.put("src_path", "The global path where the model[source] was found in the file system");
            dataSource.put("conv_path", "The global path of the instantiated model converted to XML");
            dataSource.put("src_ext", "The extension of the model[source] ex: .aadl ");
            dataSource.put("is_parsed", "A boolean value that show if the model could be parsed or not, if it wasn't, model could be broken");
            dataSource.put("is_sys_design", "A boolean value that show if in the model[source] there was found a SystemInstance model");
            dataSource.put("num_errors", "A maximum of 10 error resulting of the validation provided by OSATE api over and aadl model");
            dataSource.put("sys_name", "The name of the SystemInstance model found");
            dataSource.put("num_comp", "The number of component of the model: ComponentInstance found");
            dataSource.put("num_conn", "The number of connections of the model: ConnectionInstance found");
            dataSource.put("size", "The sum of components and connection");
            dataSource.put("udy", "The Understandability: The number of connections between components divided by N^2-N, " +
                    "where N is the number of components");
            dataSource.put("no_hardware_comp", "The number of component which belong to " +
                    "the category of [\"device\",\"memory\",\"bus\",\"processor\"]");
            dataSource.put("no_software_comp", "The number of component which belong to the " +
                    "category of [\"process\",\"thread\",\"subprogram\",\"threadGroup\",\"subprogramGroup\"]");
            dataSource.put("no_data_comp", "The number of component which belong to the category of [\"data\"]");

            String[] header = {"column", "description"};
            writer.writeNext(header);
            for (String col : dataSource.keySet().stream().sorted().toList()) {
                String[] row = new String[]{col, dataSource.get(col)};
                writer.writeNext(row);
            }
            writer.close();
            System.out.println("GENERATED LEGEND SUCCESSFULLY: " + file.getAbsolutePath());
        } catch (Exception error) {
            System.out.println("Error creating a csv file");
            error.printStackTrace();
        }

    }
}


