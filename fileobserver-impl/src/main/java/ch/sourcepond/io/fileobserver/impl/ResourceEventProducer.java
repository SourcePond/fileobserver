package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.ResourceObserver;

import java.nio.file.Path;

/**
 * This class manages {@link ResourceObserver} instances and delivers file modifications/deletions to any interested
 * observer.
 */
class ResourceEventProducer {
    private Directories directories;


    void fileModify(String pId, Path pFile) {

    }

    void fileDelete(String pId) {

    }
}
