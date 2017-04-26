package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.DispatchEvent;
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static ch.sourcepond.io.fileobserver.DirectorySetup.*;
import static org.junit.Assert.*;

/**
 * Created by rolandhauser on 09.03.17.
 */
final class InitialCheckusmCalculationBarrier implements PathChangeListener {
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
    public synchronized void modified(final DispatchEvent pEvent) throws IOException {
        expectedFiles.remove(pEvent.getFile());
        notifyAll();
    }

    @Override
    public void discard(final DispatchKey pKey) {
        // noop
    }

    public synchronized void waitUntilChecksumsCalculated() throws InterruptedException {
        while (runs-- >= 0 && !expectedFiles.isEmpty()) {
            wait(5000);
        }
        assertTrue("Not all files have been calculated within expected time! " + expectedFiles, expectedFiles.isEmpty());
    }
}
