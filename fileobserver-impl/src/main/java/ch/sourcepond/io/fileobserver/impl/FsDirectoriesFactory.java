package ch.sourcepond.io.fileobserver.impl;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class FsDirectoriesFactory {

    FsDirectories newDirectories(final Registrar pRegistrar) {
        return new FsDirectories(pRegistrar);
    }
}
