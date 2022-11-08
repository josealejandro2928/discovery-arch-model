package org.osate.aadl2.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.URIHandlerImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLParserPoolImpl;

public class Aadl2ResourceFactoryImpl extends ResourceFactoryImpl {

	public Aadl2ResourceFactoryImpl() {
		super();
	}

	public Resource createResource(URI uri) {
		XMLResource result = new Aadl2ResourceImpl(uri);

		result.getDefaultLoadOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
		result.getDefaultLoadOptions().put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);
		result.getDefaultLoadOptions().put(XMLResource.OPTION_USE_LEXICAL_HANDLER, Boolean.TRUE);

		result.getDefaultSaveOptions().put(XMLResource.OPTION_CONFIGURATION_CACHE, Boolean.TRUE);
		result.getDefaultSaveOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
		result.getDefaultSaveOptions().put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
		result.getDefaultSaveOptions().put(XMLResource.OPTION_URI_HANDLER, new URIHandlerImpl.PlatformSchemeAware());
		result.getDefaultSaveOptions().put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.FALSE);
		result.getDefaultSaveOptions().put(XMLResource.OPTION_USE_FILE_BUFFER, Boolean.TRUE);

		return result;
	}

	static final XMLParserPoolImpl parserPool = new XMLParserPoolImpl();

}