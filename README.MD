# Discovery Architectural Models
This is a maven project for searching for architectural models given a set of directories.
In the future the project will be also available for searching for a given httpURI for discovering models on the web.
The main files that we are looking for the moment are: **.aadl, .xmi and .ecore**

The main objective of this project is to convert all the previous models found into XMI format
in order to use MDE techniques for the future preprocessing.

#### SearchFileTraversal Class part
![image](https://user-images.githubusercontent.com/37028825/198272771-b501f55b-7297-493d-a127-1f54fe6c1d6f.png)

#### The BFS algorithms for search in the file system
![image](https://user-images.githubusercontent.com/37028825/198348777-0ddfffc5-78a8-457e-8473-c0b9d427a7e9.png)

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
