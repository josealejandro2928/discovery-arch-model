package org.osate.standalone.model;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface RawModelLoader {
    Object loadModel(String pathAADLFile, String pathXMLFile, String id, boolean verbose) throws Exception;
    List<Object> validateModel(Resource[] resources);

}
