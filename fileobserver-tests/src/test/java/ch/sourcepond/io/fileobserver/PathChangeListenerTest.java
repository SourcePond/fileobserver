/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.DispatchEvent;
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.testing.BundleContextClassLoaderRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;

import static ch.sourcepond.io.fileobserver.DirectoryKey.ROOT;
import static ch.sourcepond.io.fileobserver.DirectorySetup.*;
import static ch.sourcepond.io.fileobserver.RecursiveDeletion.deleteDirectory;
import static ch.sourcepond.io.fileobserver.spi.WatchedDirectory.create;
import static ch.sourcepond.testing.OptionsHelper.karafContainer;
import static ch.sourcepond.testing.OptionsHelper.mockitoBundles;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

/**
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class PathChangeListenerTest {

    @Rule
    public BundleContextClassLoaderRule rule = new BundleContextClassLoaderRule(this);

    @Rule
    public DirectorySetup dirSetup = new DirectorySetup();

    @Configuration
    public Option[] config() {
        MavenUrlReference listenerRepo = maven()
                .groupId("ch.sourcepond.io")
                .artifactId("fileobserver-feature")
                .classifier("features")
                .type("xml")
                .versionAsInProject();

        return new Option[]{
                mavenBundle().groupId("ch.sourcepond.testing").artifactId("bundle-test-support").versionAsInProject(),
                mockitoBundles(),
                karafContainer(features(listenerRepo, "fileobserver-feature"))
        };
    }

    private static boolean isKeyEqual(final DispatchKey dispatchKey, final Object pDirectoryKey, final Path pRelativePath) {
        return pDirectoryKey.equals(dispatchKey.getDirectoryKey()) && dispatchKey.getRelativePath().equals(pRelativePath);
    }

    private static DispatchKey key(final DirectoryKey pKey, final Path pRelativePath) {
        return argThat(new ArgumentMatcher<DispatchKey>() {
            @Override
            public boolean matches(final DispatchKey dispatchKey) {
                return isKeyEqual(dispatchKey, pKey, pRelativePath);
            }

            @Override
            public String toString() {
                return format("[%s, %s]", pKey, pRelativePath);
            }
        });
    }

    private static DispatchEvent event(final DirectoryKey pKey, final Path pRelativePath) {
        return argThat(new ArgumentMatcher<DispatchEvent>() {
            @Override
            public boolean matches(final DispatchEvent event) {
                return isKeyEqual(event.getKey(), pKey, pRelativePath);
            }

            @Override
            public String toString() {
                return format("event [%s, %s]", pKey, pRelativePath);
            }
        });
    }


    private static void writeArbitraryContent(final Path pFile) throws IOException {
        try (final BufferedWriter writer = newBufferedWriter(pFile)) {
            writer.write(randomUUID().toString());
        }
    }

    @Inject
    private BundleContext context;

    private final PathChangeListener listener = mock(PathChangeListener.class, withSettings().name("listener"));
    private final PathChangeListener secondListener = mock(PathChangeListener.class, withSettings().name("secondListener"));
    private ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration;
    private ServiceRegistration<PathChangeListener> listenerRegistration;
    private ServiceRegistration<PathChangeListener> secondListenerRegistration;
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);
    private ServiceRegistration<KeyDeliveryHook> hookRegistration;
    private WatchedDirectory watchedDirectory;

    @Before
    public void setup() throws Exception {
        doCallRealMethod().when(listener).restrict(notNull());
        doCallRealMethod().when(secondListener).restrict(notNull());

        // Step 1: make fileobserver bundle watching R by
        // registering an appropriate service.
        final InitialCheckusmCalculationBarrier wait = new InitialCheckusmCalculationBarrier();
        listenerRegistration = context.registerService(PathChangeListener.class, wait, null);
        watchedDirectory = create(ROOT, R);
        watchedDirectoryRegistration = context.registerService(WatchedDirectory.class, watchedDirectory, null);
        wait.waitUntilChecksumsCalculated();
        listenerRegistration.unregister();

        // Step 2: register PathChangeListener
        listenerRegistration = context.registerService(PathChangeListener.class, listener, null);
        verifyForceInform(listener);
        reset(listener);

        // Step 3: register key-hook
        hookRegistration = context.registerService(KeyDeliveryHook.class, hook, null);
    }

    private void verifyForceInform(final PathChangeListener pListener) throws Exception {
        verify(pListener, timeout(5000)).modified(event(ROOT, R.relativize(E11)));
        verify(pListener, timeout(5000)).modified(event(ROOT, R.relativize(E12)));
        verify(pListener, timeout(5000)).modified(event(ROOT, R.relativize(E2)));
        verify(pListener, timeout(5000)).modified(event(ROOT, R.relativize(H11)));
        verify(pListener, timeout(5000)).modified(event(ROOT, R.relativize(H12)));
        verify(pListener, timeout(5000)).modified(event(ROOT, R.relativize(H2)));
        verify(pListener, timeout(5000)).modified(event(ROOT, R.relativize(C)));
    }

    private void unregisterService(final ServiceRegistration<?> pRegistration) {
        if (pRegistration != null) {
            try {
                pRegistration.unregister();
            } catch (final IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        unregisterService(watchedDirectoryRegistration);
        unregisterService(listenerRegistration);
        unregisterService(secondListenerRegistration);
        unregisterService(hookRegistration);
    }

    @Test
    public void doExlusivelyInformNewlyRegisteredListener() throws Exception {
        secondListenerRegistration = context.registerService(PathChangeListener.class, secondListener, null);
        verifyForceInform(secondListener);
        verifyZeroInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void insureNoInteractionWithUnregisteredFileListener() throws Exception {
        listenerRegistration.unregister();

        delete(E11);
        delete(E12);
        delete(E2);
        delete(H11);
        delete(H12);
        delete(H2);
        delete(C);

        sleep(6000);
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void unregisterAndRegisterAdditionalWatchedDirectory() throws Exception {
        // Insure listener gets informed about unregistration
        watchedDirectoryRegistration.unregister();
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(R)));

        // Now, listener should be informed about newly registered root
        watchedDirectoryRegistration = context.registerService(WatchedDirectory.class, watchedDirectory, null);

        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(E11)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(E12)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(E2)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(H11)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(H12)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(H2)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(C)));
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutFileDeletion() throws IOException {
        delete(E11);
        delete(E12);
        delete(E2);
        delete(H11);
        delete(H12);
        delete(H2);
        delete(C);
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(E11)));
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(E12)));
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(E2)));
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(H11)));
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(H12)));
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(H2)));
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(C)));
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutDirectoryDeletion() throws IOException {
        deleteDirectory(E1);
        deleteDirectory(H1);
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(E1)));
        verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(H1)));

        // See PathChangeListener::discard for explanation; the following works not for MacOS X
        if ("Linux".equals(System.getProperty("os.name"))) {
            verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(E11)));
            verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(E12)));
            verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(H11)));
            verify(listener, timeout(15000)).discard(key(ROOT, R.relativize(H12)));
        }

        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutFileChange() throws IOException {
        writeArbitraryContent(E12);
        writeArbitraryContent(H12);
        writeArbitraryContent(C);

        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(E12)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(H12)));
        verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(C)));
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutFileCreation() throws IOException {
        final Path newFile = E1.resolve("newFile.txt");
        writeArbitraryContent(newFile);

        final InOrder order = inOrder(hook, listener);
        order.verify(hook, timeout(15000)).beforeModify(key(ROOT, R.relativize(newFile)), eq(newFile));
        order.verify(listener, timeout(15000)).modified(event(ROOT, R.relativize(newFile)));
        order.verify(hook, timeout(15000)).afterModify(key(ROOT, R.relativize(newFile)), eq(newFile));

        verifyNoMoreInteractions(listener);
    }
}