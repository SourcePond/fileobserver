package ch.sourcepond.io.fileobserver.impl;

import java.io.IOException;
import java.nio.file.WatchKey;

/**
 *
 */
@FunctionalInterface
interface WatchKeyProcessor {

    void processEvent(WatchKey pWatchKey) throws IOException;
}
