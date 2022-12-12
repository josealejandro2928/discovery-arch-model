package org.discover.arch.model;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osate.standalone.model.RawModelLoader;
import org.osate.standalone.model.LoadAADLModel;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ArchModelConverter {
    String rootPath;
    List<String> dataModelFiles = new ArrayList<>();
    List<String> extensions = new ArrayList<>();
    String folderOutputName;
    HashMap<String, String> converterModelJarPlugginsMap = new HashMap<>();
    HashMap<String, Object> converterModelClassMap = new HashMap<>();
    JarExecutor jarExe = new JarExecutor();
    JSONArray logsOutput = new JSONArray();
    Config configObj = Config.getInstance(null);

    ArchModelConverter(String rootPath) {
        this.rootPath = rootPath;
        converterModelClassMap.put("aadl", LoadAADLModel.getInstance());
    }

    ArchModelConverter(String rootPath, String[] extensions, String outputFolderName) {
        this.rootPath = rootPath;
        this.extensions = Arrays.asList(extensions);
        this.folderOutputName = outputFolderName;
        converterModelClassMap.put("aadl", LoadAADLModel.getInstance());
    }

    ArchModelConverter() {
        this.rootPath = this.configObj.getRootPath();
        this.extensions = this.configObj.getExtensionsForSearching();
        this.folderOutputName = this.configObj.getOutputFolderName();
        converterModelClassMap.put("aadl", LoadAADLModel.getInstance());
    }

    public ArchModelConverter setDataModelFiles(List<String> data) {
        this.dataModelFiles = data;
        return this;
    }

    public ArchModelConverter setDataModelFiles(String path) throws Exception {
        File file = new File(path);
        if (!file.exists())
            throw new Exception("The file: " + path + " for loading the pathModels does not exists");
        Scanner myReader = new Scanner(file);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            this.dataModelFiles.add(data);
        }
        return this;
    }

    public ArchModelConverter setFolderOutputName(String name) {
        this.folderOutputName = name;
        return this;
    }

    public ArchModelConverter setExtensions(List<String> ext) {
        this.extensions = ext;
        return this;
    }

    @Override
    public String toString() {
        return "ArchModelConverter:: rootPath: " + this.rootPath + "; " + "exts: " + this.extensions;
    }

    public void initProcessing() throws Exception {
//        if (this.dataModelFiles.size() == 0)
//            throw new Exception("There is not filesModelPath for processing");
        System.out.println("COPYING TO FOLDERS THE FILES...");
        this.copyFoundedFiles();
        System.out.println("COPYING TO FOLDERS THE FILES COMPLETED");
        System.out.println("*********************STAGE 2********************");
        System.out.println("CONVERTING THE FOUND MODELS TO XMI...");
        long startTime = System.nanoTime();
        this.convertModels(true);
        long endTime = System.nanoTime();
        double elapsedTime = (double) (endTime - startTime) / 1000000000;
        System.out.println("CONVERTING THE FOUND MODELS TO XMI COMPLETED: " + new DecimalFormat("0.000").format(elapsedTime) + "s");
        System.out.println("LOGGING THE CONVERSION FILE .json...");
        this.loggingConvertingResult();
        System.out.println("LOGGING THE CONVERSION FILE .json COMPLETED");
    }


    private void copyFoundedFiles() throws Exception {
        int id = 1;
        for (String pathFile : this.dataModelFiles) {
            String extension = SearchFileTraversal.getExtension(pathFile);
            File file = new File(pathFile);
            Path originalPath = Paths.get(pathFile);
            Path copied = Paths.get(Paths.get(this.rootPath, this.folderOutputName, extension, id + "_" + file.getName()).toString());
            Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
            id++;
        }
    }

    private void convertModels(boolean verbose) throws Exception {
        int id = 0;
        String outPathXMI = Paths.get(this.rootPath, this.folderOutputName, "xmi") + "/";
        for (String pathFile : this.dataModelFiles) {
            String extension = SearchFileTraversal.getExtension(pathFile);
            if (this.converterModelClassMap.containsKey(extension)) {
                this.convertModelsUsingClass(pathFile, outPathXMI, id + "", verbose);
            } else {
                System.out.println("This converter does not support the mapping between models of " + extension + " to " + " xmi ");
            }
            id++;
        }
    }

    private void convertModelsUsingClass(String pathFile, String outPathXMI, String id, boolean verbose) throws Exception {
        String extension = SearchFileTraversal.getExtension(pathFile);
        RawModelLoader modelLoader = (RawModelLoader) this.converterModelClassMap.get(extension);
        Object data = modelLoader.loadModel(pathFile, outPathXMI, id, false);
        if (data instanceof Iterable) {
            ((List<RawModelLoader.OutputLoadedModelSchema>) data).stream().forEach((RawModelLoader.OutputLoadedModelSchema x) -> {
                Map<String, Object> dataOutMap = ((RawModelLoader.OutputLoadedModelSchema) x).toMap();
                if (verbose)
                    System.out.println(dataOutMap);
                dataOutMap.put("extension", extension);
                dataOutMap.put("id", id);
                this.logsOutput.put(new JSONObject(dataOutMap));
            });
        } else {
            Map<String, Object> dataOutMap = ((RawModelLoader.OutputLoadedModelSchema) data).toMap();
            if (verbose)
                System.out.println(dataOutMap);
            dataOutMap.put("extension", extension);
            dataOutMap.put("id", id);
            this.logsOutput.put(new JSONObject(dataOutMap));
        }

    }

    private void loggingConvertingResult() throws Exception {
        String jsonStr = this.logsOutput.toString(2);
        try {
            FileWriter fw = new FileWriter(Paths.get(this.rootPath, this.folderOutputName, "conversion-logs.json").toString());
            fw.write(jsonStr);
            fw.close();
        } catch (Exception e) {
            throw new Exception("error generating the logs .json " + e.getMessage());
        }
        try {
            var reports = this.generateReports();
            var jsonObject = new JSONObject(reports);
            jsonStr = jsonObject.toString(2);
            System.out.println(jsonObject);
            FileWriter fw = new FileWriter(Paths.get(this.rootPath, this.folderOutputName, "reports-logs.json").toString());
            fw.write(jsonStr);
            fw.close();
        } catch (Exception e) {
            throw new Exception("error generating the reports .json" + e.getMessage());
        }

    }

    private JSONArray filterOutputResults(Iterable<Object> dataSource, Function<JSONObject, Boolean> fn) {
        JSONArray dataFiltered = new JSONArray();
        for (Object el : dataSource) {
            JSONObject item = (JSONObject) el;
            if (fn.apply(item))
                dataFiltered.put(item);
        }
        return dataFiltered;
    }

    private Map<String, HashMap<String, Object>> generateReports() {
        Map<String, HashMap<String, Object>> reports = new HashMap<>();
        for (String ext : this.extensions) {
            reports.put(ext, new HashMap<>());
            var filteredData = this.filterOutputResults(this.logsOutput, (JSONObject el) -> el.get("extension").equals(ext));
            reports.get(ext).put("totalFiles", filteredData.length());
            reports.get(ext).put("filesConverted", this.filterOutputResults(filteredData, (JSONObject el) -> el.get("isSavedTheModel").equals(true)).length());
            reports.get(ext).put("filesSuccessfullyParsed", this.filterOutputResults(filteredData, (JSONObject el) -> el.get("isParsingSucceeded").equals(true)).length());
            reports.get(ext).put("filesWithErrors", this.filterOutputResults(filteredData, (JSONObject el) -> el.get("isParsingSucceeded").equals(false)).length());
        }
        reports.put("general", new HashMap<>());
        reports.get("general").put("totalFilesDiscovered", this.logsOutput.length());
        reports.get("general").put("totalFilesSuccessfullyParsed", this.filterOutputResults(this.logsOutput,
                (JSONObject el) -> el.get("isParsingSucceeded").equals(true)).length());

        reports.get("general").put("totalFilesConverted", this.filterOutputResults(this.logsOutput,
                (JSONObject el) -> el.get("isSavedTheModel").equals(true)).length());

        reports.get("general").put("totalFilesWithErrors", this.filterOutputResults(this.logsOutput,
                (JSONObject el) -> el.get("isParsingSucceeded").equals(false)).length());
        return reports;
    }
}
