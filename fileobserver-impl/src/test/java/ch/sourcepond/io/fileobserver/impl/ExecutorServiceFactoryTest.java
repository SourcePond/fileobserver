package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.commons.smartswitch.testing.SmartSwitchRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;

/**
 * Created by rolandhauser on 13.02.17.
 */
public class ExecutorServiceFactoryTest {
    private ExecutorService observerExecutor;
    private ExecutorService directoryWalkerExecutor;
    @Rule
    public SmartSwitchRule rule = new SmartSwitchRule();
    private ExecutorServiceFactory factory;

    @Before
    public void setup() {
        observerExecutor = rule.useOsgiService(ExecutorService.class, "(sourcepond.io.fileobserver.observerexecutor=*)");
        directoryWalkerExecutor = rule.useOsgiService(ExecutorService.class, "(sourcepond.io.fileobserver.directorywalkerexecutor=*)");
        factory = new ExecutorServiceFactory(rule.getTestFactory());
    }

    @Test
    public void getDirectoryWalkerExecutor() {
        assertSame(directoryWalkerExecutor, factory.getDirectoryWalkerExecutor());
    }

    @Test
    public void getObserverExecutor() {
        assertSame(observerExecutor, factory.getObserverExecutor());
    }
}
