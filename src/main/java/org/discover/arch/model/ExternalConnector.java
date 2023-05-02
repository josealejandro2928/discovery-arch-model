package org.discover.arch.model;

import java.io.IOException;

public interface ExternalConnector {

    MetaData extractMetaData(String externalRepoURL) throws InvalidURLConnectorException;

    void loadResource(String externalRepoURL, String directoryPath) throws Exception;

    boolean isValidPath(String externalRepoURL);

    boolean isReadyForDownload(String path);
    void deleteBeforeLoading(String clonedDirectoryRepo) throws Exception;

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


