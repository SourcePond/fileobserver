package ch.sourcepond.io.fileobserver.impl;

import java.nio.file.WatchService;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class FsDirectoriesFactory {

    FsDirectories newDirectories(final WatchService pWatchService) {
        return new FsDirectories(pWatchService);
    }
}
