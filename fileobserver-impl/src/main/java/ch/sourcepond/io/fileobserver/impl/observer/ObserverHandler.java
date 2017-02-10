package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.io.fileobserver.api.FileKey;

import java.nio.file.Path;

/**
 *
 */
public interface ObserverHandler {

    void modified(FileKey pKey, Path pFile);

    void deleted(FileKey pKey);
}
