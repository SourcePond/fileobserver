package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.ServiceEvent.REGISTERED;

class SetupBarrier implements PathChangeListener, ServiceListener {
    private final List<Path> expected;
    final PathChangeListenerTest test;
    private int trials;
    private boolean keyDeliveryHookRegistered;

    SetupBarrier(final PathChangeListenerTest pTest) {
        test = pTest;
        expected = new ArrayList<>(asList(
                pTest.root_etc_network_networkConf,
                pTest.root_etc_network_dhcpConf,
                pTest.root_etc_manConf,
                pTest.root_home_jeff_documentTxt,
                pTest.root_home_jeff_letterXml,
                pTest.root_home_indexIdx,
                pTest.root_configProperties
        ));
    }

    @Override
    public synchronized void modified(PathChangeEvent pEvent) throws IOException {
        expected.remove(pEvent.getFile());
        notifyAll();
    }

    @Override
    public void discard(DispatchKey pKey) {
        // noop
    }

    public synchronized void awaitWatchedDirectoryRegistration() throws InterruptedException {
        while (!expected.isEmpty() && 10 > trials++) {
            wait(1000L);
        }
        assertTrue("Not all expected files received: " + expected, expected.isEmpty());
    }

    public void awaitListenerInformedAboutExistingFiles() throws Exception {
        test.verifyForceInform(test.listener);
    }

    @Override
    public void serviceChanged(final ServiceEvent serviceEvent) {
        if (REGISTERED == serviceEvent.getType()) {
            synchronized (this) {
                keyDeliveryHookRegistered = true;
                notifyAll();
            }
        }
    }

    public synchronized void awaitHookRegistrationCompleted() throws InterruptedException {
        trials = 0;
        while (!keyDeliveryHookRegistered && 10 > trials++) {
            wait(1000L);
        }
        assertTrue(format("Expected service %s has not been registered", KeyDeliveryHook.class.getName()), keyDeliveryHookRegistered);
    }
}
