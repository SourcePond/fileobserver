package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static ch.sourcepond.io.fileobserver.DirectorySetup.*;
import static org.junit.Assert.*;

/**
 * Created by rolandhauser on 09.03.17.
 */
final class InitialCheckusmCalculationBarrier implements FileObserver {
    private final Set<Path> expectedFiles = new HashSet<>();
    private int runs = 10;

    InitialCheckusmCalculationBarrier() {
        expectedFiles.add(E11);
        expectedFiles.add(E12);
        expectedFiles.add(E2);
        expectedFiles.add(H11);
        expectedFiles.add(H12);
        expectedFiles.add(H2);
        expectedFiles.add(C);
    }

    @Override
    public synchronized void modified(final FileKey<?> pKey, final Path pFile) throws IOException {
        expectedFiles.remove(pFile);
        notifyAll();
    }

    @Override
    public void discard(final FileKey<?> pKey) {
        // noop
    }

    public synchronized void waitUntilChecksumsCalculated() throws InterruptedException {
        while (runs-- >= 0 && !expectedFiles.isEmpty()) {
            wait(5000);
        }
        assertTrue("Not all files have been calculated within expected time! " + expectedFiles, expectedFiles.isEmpty());
    }
}
