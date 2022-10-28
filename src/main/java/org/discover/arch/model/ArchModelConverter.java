package org.discover.arch.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class ArchModelConverter {
    String rootPath;
    List<String> dataModelFiles = new ArrayList<>();
    List<String> extensions = new ArrayList<>();
    String folderOutputName;
    HashMap<String, String> converterModelJarPlugginsMap = new HashMap<>();
    JarExecutor jarExe = new JarExecutor();
    JSONArray logsOutput = new JSONArray();

    ArchModelConverter(String rootPath) throws Exception {
        File file = new File(rootPath);
        if (!file.exists())
            throw new Exception("The rootPath: " + rootPath + "does not exists");
        this.rootPath = rootPath;
        converterModelJarPlugginsMap.put("aadl", "ConvertAADLToXMI.jar");
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
        if (this.dataModelFiles.size() == 0)
            throw new Exception("There is not filesModelPath for processing");
        System.out.println("COPYING TO FOLDERS THE FILES...");
        this.copyFoundedFiles();
        System.out.println("COPYING TO FOLDERS THE FILES COMPLETED");
        System.out.println("*********************STAGE 2********************");
        System.out.println("CONVERTING THE FOUND MODELS TO XMI...");
        this.convertModels();
        System.out.println("CONVERTING THE FOUND MODELS TO XMI COMPLETED");
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

    public void createFolderOutput() throws Exception {
        String pathOutput = Paths.get(this.rootPath, this.folderOutputName).toString();
        File file = new File(pathOutput);
        file.mkdir();
        for (File childFile : file.listFiles()) {
            deleteDirectory(childFile.toPath());
        }
        for (String ext : this.extensions) {
            new File(Paths.get(file.getPath(), ext).toString()).mkdir();
        }
        new File(Paths.get(file.getPath(), "xmi").toString()).mkdir();
    }

    static void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private void convertModels() throws Exception {
        int id = 0;
        String outPathXMI = Paths.get(this.rootPath, this.folderOutputName, "xmi") + "/";
        for (String pathFile : this.dataModelFiles) {
            String extension = SearchFileTraversal.getExtension(pathFile);
            if (this.converterModelJarPlugginsMap.containsKey(extension)) {
                Path currentWorkingDir = Paths.get("").toAbsolutePath();
                Path pathTOJarFile = Paths.get(currentWorkingDir.toString(), "jar", converterModelJarPlugginsMap.get(extension));
                List<String> args = List.of(new String[]{pathFile, outPathXMI, id + ""});
                jarExe.executeJar(pathTOJarFile.toString(), args);
                processingOutput();
            } else {
                System.out.println("This converter does not support the mapping between models of " + extension + " to " + " xmi ");
            }
            id++;
        }
    }

    private void processingOutput() throws Exception {
        List<String> outputData = this.jarExe.getOutput().lines().toList();
        // We are expecting the same out schema from the Converters.jar
        // 1- First the delimiter should be the string "OUTPUT:"
        // 2- After that we expect a line with the schema property: value
        int index = outputData.indexOf("OUTPUT:");
        if (index == -1)
            throw new Exception("The schema output of the .jar does not conformance with the parserFunction");

        JSONObject data = new JSONObject();
        for (int i = index + 1; i < outputData.size(); i++) {
            String[] temp = outputData.get(i).split(": ");
            String prop = temp[0].trim();
            String val = temp[temp.length - 1].trim();
            if (!val.startsWith("[")) {
                if (val.equals("true") || val.equals("false"))
                    data.put(prop, Boolean.parseBoolean(val));
                else {
                    if (temp.length == 1) {
                        data.put(prop, "");
                    } else
                        data.put(prop, val);
                }
            } else {
                JSONArray arrayData = new JSONArray(val);
                data.put(prop, arrayData);
            }
        }
        System.out.println(data);
        this.logsOutput.put(data);
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

    }
}
