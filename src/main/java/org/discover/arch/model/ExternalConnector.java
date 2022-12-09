package org.discover.arch.model;

import java.io.IOException;

public interface ExternalConnector {

    MetaData extractMetaData(String externalRepoURL) throws InvalidURLConnectorException;

    String loadResource(String externalRepoURL, String directoryPath) throws Exception;

    void cleaningAfterDownloadFinished(String clonedDirectoryRepo) throws IOException;

    boolean isValidPath(String externalRepoURL);


}

class MetaData {
    String name = "";
    String downloadablePath = "";
}

class InvalidURLConnectorException extends Exception {
    InvalidURLConnectorException(String msg) {
        super(msg);
    }

    InvalidURLConnectorException() {
        super("The URI provided might be corrupted");
    }
}


