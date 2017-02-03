package ch.sourcepond.io.fileobserver.impl;

import java.nio.file.Path;

/**
 * Created by rolandhauser on 03.02.17.
 */
@FunctionalInterface
interface ModificationConsumer {

    void consume(String pId, Path pFile);
}
