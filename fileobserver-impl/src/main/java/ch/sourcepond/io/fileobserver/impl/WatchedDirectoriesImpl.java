package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.WatchedDirectories;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by rolandhauser on 19.01.17.
 */
public class WatchedDirectoriesImpl implements WatchedDirectories {
    @Override
    public void enable(final Enum<?> pKey, final Path pDirectory) throws IOException {

    }

    @Override
    public void disable(final Enum<?> pKey) {

    }
}
