package ch.sourcepond.io.fileobserver.impl;

import java.util.concurrent.ExecutorService;

/**
 * Created by rolandhauser on 21.02.17.
 */
public class ExecutorServices {

    // Injected by Felix DM
    private volatile ExecutorService observerExecutor;

    // Injected by Felix DM
    private volatile ExecutorService directoryWalkerExecutor;

    public ExecutorService getObserverExecutor() {
        return observerExecutor;
    }

    public ExecutorService getDirectoryWalkerExecutor() {
        return directoryWalkerExecutor;
    }
}
