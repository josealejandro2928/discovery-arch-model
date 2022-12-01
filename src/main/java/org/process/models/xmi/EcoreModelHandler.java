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
    private List<Map<String, Object>> processedDataFromModel;

    private EcoreModelHandler() {
        this.uriModels = new ArrayList<>();
        this.modelExtension = Arrays.asList("xml", "xmi", "ecore", "aaxl2");
        this.processedDataFromModel = new ArrayList<>();

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
                Resource resource = ecoreStandAlone.getModelByURI(modelUri);
                try {
                    System.out.println("------------------------------------------------------------------");
                    System.out.println("URI: " + modelUri);
                    Map<String, Object> data = (Map<String, Object>) eolRunner.run("main", modelUri);
                    data.put("uri", modelUri);
                    this.processedDataFromModel.add(data);
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

    public List<Map<String, Object>> getProcessedDataFromModel() {
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
                    "sys_name", "num_comp", "num_conn"};
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
        } catch (Exception error) {
            System.out.println("Error creating a csv file");
            error.printStackTrace();
        }

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
        }
        * */
        List<Map<String, Object>> dataSource;
        List<Map<String, Object>> conversionLogs = configObj.getConversionLogs();
        dataSource = conversionLogs.stream().map(x -> {
            Map<String, Object> preData = new HashMap<>();
            preData.put("id", x.get("id"));
            preData.put("model_name", x.get("modelName"));
            preData.put("src_path", x.get("pathAADLFile"));
            preData.put("conv_path", x.get("pathXMLFile"));
            preData.put("src_ext", x.get("extension"));
            preData.put("is_parsed", x.get("isParsingSucceeded"));
            List<String> errors = (List<String>) (x.get("errors"));
            preData.put("is_sys_design", x.get("isSavedTheModel"));
            preData.put("num_errors", errors.size());
            preData.put("sys_name", "");
            preData.put("num_comp", 0);
            preData.put("num_conn", 0);
            return preData;
        }).collect(Collectors.toList());
        return dataSource;
    }
}


