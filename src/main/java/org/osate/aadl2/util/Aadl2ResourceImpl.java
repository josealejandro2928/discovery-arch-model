
package org.osate.aadl2.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLParserPoolImpl;

public class Aadl2ResourceImpl extends XMIResourceImpl {
    static final XMLParserPoolImpl parserPool = new XMLParserPoolImpl();

    public Aadl2ResourceImpl(URI uri) {
        super(uri);
    }

}
