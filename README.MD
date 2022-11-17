# Discovery and Process Architectural Models 
This is a maven project whose main objective is to search for arch models from a set of file paths 
or web paths to get them, catalog them, get the XMI representation and perform metrics. 
Right now only support the parsing for aadl files, and the search is only done over a file system.
In the future, the project will also be available for searching for a given HTTP URI for 
discovering models on the web.

The main objective of this project is to **convert all the previous models found into XMI format**
to use **MDE techniques for future preprocessing**.

The project contains three packages:
1. discover.arch.model
2. osate.standalone.model
3. process.model.xmi

### discover.arch.model: ### 
It contains the classes in charge of processing the path for looking 
for aadl files and the conversion to XMI format. The first step is the search; here, 
we perform a BFS to find the raw models. When the discovery is made, all the paths to 
models are stored in memory and in a .txt file named "files-found.txt".
```text
./models/raw-models/SecuritySystem/syst.aadl
./models/raw-models/SimpleWebServer/ClientServer.aadl
./models/raw-models/UAS/UAS.aadl
./models/raw-models/UAS/UAS_Buses.aadl
./models/raw-models/UAV/Drivers.aadl
./models/raw-models/UAV/FC.aadl
./models/raw-models/UAV/MC.aadl
./models/raw-models/UAV/SW.aadl
./models/raw-models/UAV/UAV.aadl
```
Once you get the list of model paths, the parsing, and conversion into XML starts. 
The class ArchModelConverter takes the list and gets the RawModelLoader Instance that 
map to the extension for the conversion. The subsequent new converted model is stored 
in the disk in the path configured by the user. Bellow some examples
```java
   ArchModelConverter(String rootPath) throws Exception {
    File file = new File(rootPath);
    if (!file.exists())
        throw new Exception("The rootPath: " + rootPath + "does not exists");
    this.rootPath = rootPath;
    converterModelClassMap.put("aadl", LoadAADLModel.getInstance());
}
```
```java
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

private void convertModelsUsingClass(String pathFile, String outPathXMI, String id, 
        boolean verbose) throws Exception {
       String extension = SearchFileTraversal.getExtension(pathFile);
       RawModelLoader modelLoader = (RawModelLoader) this.converterModelClassMap.get(extension);
       RawModelLoader.OutputLoadedModelSchema data = modelLoader.loadModel(pathFile, outPathXMI, id, false);
       Map<String, Object> dataOutMap = data.toMap();
       if (verbose)
           System.out.println(dataOutMap);
       dataOutMap.put("extension", extension);
       this.logsOutput.put(new JSONObject(dataOutMap));
}
```

The BFS algorithms for search in the file system
<img alt="image" height="500" src="https://user-images.githubusercontent.com/37028825/198348777-0ddfffc5-78a8-457e-8473-c0b9d427a7e9.png" title="BFS algorithm for searching models"/>

### osate.standalone.model: ###
This package contains the classes in charge of parsing and loading .addl and XMI 
files. This has all the dependencies from OSATE and eclipse EMF. 
The result of parsing these classes is to get the ECORE representation of this model

The classes are:

| Classes                | Description                                                                              |
|------------------------|------------------------------------------------------------------------------------------|
| LoadXMIModel           | Load the .aaxl2 file and return the EObject representation.                              |
| LoadAADLModel          | Load and convert arch models .aadl into XMI instance model .aaxl2. It uses the OSATE api |
| CrossReferenceResolver | Resolve the cross reference models in .aadl files.                                       |





#### Part of the converter class
![image](https://user-images.githubusercontent.com/37028825/198349018-8883a696-76d7-4b65-85f3-daca0e0deba3.png)


### In order to run correctly the Main class a path to a config.json has to be created
You must create the config.json following this schema, and pass to the static variable the paths
```json
{
  "rootPath":"/mnt/DATA/00-GSSI/00-WORK",
  "archivesForSearching": ["test-repo"],
  "extensionsForSearching": ["aadl","pdf"],
  "outputFolderName": "output-discover"
}
```
```java
public class Main {
    static String configPath = "/mnt/DATA/00-GSSI/00-WORK/discovery-arch-model/config.json";
    static String rootPath;
    ...
```

|Name |Description|
|-----|--------|
|rootPath| THe rootPath or the global uri in the filesystem where we want to store, all the folder of models and all the logs files|
|archivesForSearching  |An array of folders where we have stored the files we want to process|
|extensionsForSearching  |An array of extension files that we aim to look for and we aim to convert(The models, ej: addl,ecore, emf)|
|outputFolderName  |The name of the folder to put the results |
