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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ArchModelConverter {
    String rootPath;
    Set<String> dataModelFiles = new HashSet<>();
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
        this.dataModelFiles = new HashSet<>(data);
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
        return "ArchModelConverter:: rootPath: " + this.rootPath + "; " + "ext: " + this.extensions;
    }


    void copyFoundedFiles() throws Exception {
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

//    private void convertModelsInParallel(boolean verbose) throws Exception {
//        int NUM_THREADS = Math.min(4, Runtime.getRuntime().availableProcessors());
//        int chunksSize = this.dataModelFiles.size() / NUM_THREADS;
//        List<Thread> poolThreads = new ArrayList<>();
//        for (int i = 0; i < dataModelFiles.size(); i += chunksSize) {
//            final int start = i;
//            final int end = Math.min(start + chunksSize, this.dataModelFiles.size());
//            poolThreads.add(new Thread(() -> {
//                try {
//                    convertSliceOfModels(start, end, verbose);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    throw new RuntimeException(e);
//                }
//            }));
//        }
//        poolThreads.forEach(Thread::start);
//        for (Thread t : poolThreads) {
//            t.join();
//        }
//    }

    private Map<String, Object> convertModelsUsingClass(String pathFile, String outPathXMI, String id) throws Exception {
        // String extension = SearchFileTraversal.getExtension(pathFile);
        String extension = "aadl";
        RawModelLoader modelLoader = (RawModelLoader) this.converterModelClassMap.get(extension);
        return modelLoader.loadModel(pathFile, outPathXMI, id, false);
    }

    void loggingConvertingResult() throws Exception {
        String jsonStr = this.logsOutput.toString(2); //TODO: This is impacting the heap memory
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
                    if (error == null)
                        continue;
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

    public void analyzeFileAndConvert(String pathToModelOrFolder) throws Exception {
        String outPathXMI = Paths.get(this.rootPath, this.folderOutputName, "xmi") + "/";
        File fileSrc = new File(pathToModelOrFolder);
        Map<String, Object> dataOutput = convertModelsUsingClass(pathToModelOrFolder, outPathXMI, fileSrc.getName());
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            List<String> aadlFiles = (List<String>) dataOutput.get(RawModelLoader.MODEL_FILES_FOUND);
            List<String> docFiles = (List<String>) dataOutput.get(RawModelLoader.DOC_FILES); // TODO: This is making a big impact in the generation of the json
            this.dataModelFiles.addAll(aadlFiles);
            List<OutputLoadedModelSchema> dataOutputConversion = (List<OutputLoadedModelSchema>) dataOutput.get(RawModelLoader.CONVERTING_OUTPUT);

            dataOutputConversion.forEach((OutputLoadedModelSchema out) -> {
                Map<String, Object> dataOutMap = out.toMap();
                dataOutMap.put("extension", "aadl");
                dataOutMap.put("docFiles", docFiles); // TODO: See the way to storage this information wihout impacting the heap memory
                this.conversionOutput.add(out);
                this.logsOutput.put(new JSONObject(dataOutMap));
            });
            aadlFiles.forEach(this.configObj::putInCache);
        } finally {
            lock.unlock();
        }
    }
}
