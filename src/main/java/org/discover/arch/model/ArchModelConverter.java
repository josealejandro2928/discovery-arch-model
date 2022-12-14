package org.discover.arch.model;

import com.opencsv.CSVWriter;
import org.eclipse.xtext.validation.Issue;
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
import java.util.stream.Stream;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class ArchModelConverter {
    String rootPath;
    List<String> dataModelFiles = new ArrayList<>();
    List<String> extensions;
    String folderOutputName;
    HashMap<String, Object> converterModelClassMap = new HashMap<>();
    JSONArray logsOutput = new JSONArray();
    List<OutputLoadedModelSchema> conversionOutput = new ArrayList<>();
    Config configObj = Config.getInstance(null);

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
        System.out.println("CREATING CSV OF ERRORS...");
        this.createCSVOfError();
        System.out.println("CREATING CSV OF ERRORS... COMPLETED");
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
        if (data == null)
            return;
        if (data instanceof Iterable) {
            ((List<OutputLoadedModelSchema>) data).forEach((OutputLoadedModelSchema x) -> {
                Map<String, Object> dataOutMap = x.toMap();
                if (verbose)
                    System.out.println(dataOutMap);
                dataOutMap.put("extension", extension);
                dataOutMap.put("id", id);
                conversionOutput.add(x);
                this.logsOutput.put(new JSONObject(dataOutMap));
            });
        } else {
            Map<String, Object> dataOutMap = ((OutputLoadedModelSchema) data).toMap();
            if (verbose) {
                System.out.println(dataOutMap);
            }
            dataOutMap.put("extension", extension);
            dataOutMap.put("id", id);
            conversionOutput.add((OutputLoadedModelSchema) data);
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

    void createCSVOfError() {
        try {
            File file = Paths.get(this.configObj.getRootPath(), this.configObj.getOutputFolderName(), "error-info.csv").toFile();
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            String[] header = {"model_name", "src_path", "is_parsed", "ref_resolving_error", "syntax_error", "error_codes"};
            writer.writeNext(header);
            for (OutputLoadedModelSchema element : this.conversionOutput) {
                String[] row = new String[header.length];
                row[0] = element.modelName;
                row[1] = element.pathAADLFile;
                row[2] = element.isParsingSucceeded + "";
                int ref_resolving_error = 0;
                int syntaxError = 0;
                Set<String> errorCodes = new HashSet<>();
                for (Object error : element.getErrors(true)) {
                    String errMessage = error.toString();
                    if (errMessage.contains("Couldn't resolve reference")) {
                        ref_resolving_error++;
                    }
                    if (error instanceof Issue) {
                        String code = ((Issue) error).getCode();
                        if (code != null)
                            errorCodes.add(code);
                        if (((Issue) error).isSyntaxError())
                            syntaxError++;
                    } else {
                        syntaxError++;
                    }
                }
                row[3] = ref_resolving_error + "";
                row[4] = syntaxError + "";
                row[5] = errorCodes.toString();
                writer.writeNext(row);
            }
            writer.close();
            System.out.println("GENERATED CSV OF ERROR REPORTS SUCCESSFULLY: " + file.getAbsolutePath());
        } catch (Exception error) {
            System.out.println("Error creating a csv file");
            error.printStackTrace();
        }
    }
}
