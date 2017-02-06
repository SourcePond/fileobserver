package ch.sourcepond.io.fileobserver.impl;

import java.nio.file.Path;

/**
 *
 */
interface ObserverHandler {

    void modified(String pId, Path pFile);

    void deleted(String pId);
}
