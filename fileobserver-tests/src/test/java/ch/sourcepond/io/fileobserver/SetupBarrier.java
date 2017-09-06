package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedSet;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

class SetupBarrier implements PathChangeListener, ServiceListener {
    private static final int MAX_TRIALS = 20;
    private static final long TIMEOUT = 1000L;
    private final List<Path> expected;
    private final Set<ServiceReference<?>> registeredReferences = synchronizedSet(new HashSet());
    private final Set<ServiceRegistration<?>> registrations = synchronizedSet(new HashSet());
    private ServiceRegistration<PathChangeListener> selfRegistration;
    final PathChangeListenerTest test;
    private int trials;

    private SetupBarrier(final PathChangeListenerTest pTest) {
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

    private synchronized void await(final AwaitCondition pCondition,
                                    final String pMessage) {
        try {
            while (!pCondition.isDone() && MAX_TRIALS > trials++) {
                wait(TIMEOUT);
            }
            assertTrue(pMessage, pCondition.isDone());
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            trials = 0;
        }
    }

    public void awaitWatchedDirectoryRegistration() throws InterruptedException {
        await(() -> expected.isEmpty(), format("Not all expected files received: %s", expected));
    }

    public void awaitListenerInformedAboutExistingFiles() throws Exception {
        test.verifyForceInform(test.listener);
    }

    public <T> ServiceRegistration<T> registerService(final Class<T> pInterface, final T pService) throws Exception {
        final ServiceRegistration<T> reg = test.context.registerService(pInterface, pService, null);
        registrations.add(reg);
        await(() -> registeredReferences.contains(reg.getReference()), format("Service %s not registered!", pInterface.getName()));
        return reg;
    }

    @Override
    public synchronized void serviceChanged(final ServiceEvent serviceEvent) {
        if (REGISTERED == serviceEvent.getType()) {
            registeredReferences.add(serviceEvent.getServiceReference());
            notifyAll();
        } else if (UNREGISTERING == serviceEvent.getType()) {
            registeredReferences.remove(serviceEvent.getServiceReference());
            notifyAll();
        }
    }

    public void unregisterService(final ServiceRegistration<?> pRegistration) {
        if (pRegistration != null) {
            final ServiceReference<?> reference = pRegistration.getReference();
            try {
                pRegistration.unregister();
            } catch (final IllegalStateException e) {
                // Ignore
            }
            await(() -> !registeredReferences.contains(reference), format("Service not unregistered! %s", reference));
            registrations.remove(pRegistration);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void tearDown() throws Exception {
        unregisterService(selfRegistration);
        new ArrayList<>(registrations).forEach(this::unregisterService);
        test.context.removeServiceListener(this);
    }

    @FunctionalInterface
    private interface AwaitCondition {

        boolean isDone();
    }

    static SetupBarrier create(final PathChangeListenerTest pTest) throws Exception {
        final SetupBarrier barrier = new SetupBarrier(pTest);
        pTest.context.addServiceListener(barrier);
        barrier.selfRegistration = barrier.registerService(PathChangeListener.class, barrier);
        return barrier;
    }
}
