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

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
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
public class FileObserverTest {

    @Rule
    public BundleContextClassLoaderRule rule = new BundleContextClassLoaderRule(this);

    @Rule
    public DirectorySetup dirSetup = new DirectorySetup();

    @Configuration
    public Option[] config() {
        MavenUrlReference fileObserverRepo = maven()
                .groupId("ch.sourcepond.io")
                .artifactId("fileobserver-feature")
                .classifier("features")
                .type("xml")
                .versionAsInProject();

        return new Option[]{
                mavenBundle().groupId("ch.sourcepond.testing").artifactId("bundle-test-support").versionAsInProject(),
                mockitoBundles(),
                karafContainer(features(fileObserverRepo, "fileobserver-feature"))
        };
    }

    private static FileKey key(final DirectoryKey pKey, final Path pRelativePath) {
        return argThat(new ArgumentMatcher<FileKey>() {
            @Override
            public boolean matches(final FileKey fileKey) {
                return pKey.equals(fileKey.getDirectoryKey()) && fileKey.getRelativePath().equals(pRelativePath);
            }

            @Override
            public String toString() {
                return format("[%s, %s]", pKey, pRelativePath);
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

    private final FileObserver observer = mock(FileObserver.class, withSettings().name("observer"));
    private final FileObserver secondObserver = mock(FileObserver.class, withSettings().name("secondObserver"));
    private ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration;
    private ServiceRegistration<FileObserver> fileObserverRegistration;
    private ServiceRegistration<FileObserver> secondObserverRegistration;
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);
    private ServiceRegistration<KeyDeliveryHook> hookRegistration;
    private WatchedDirectory watchedDirectory;

    @Before
    public void setup() throws Exception {
        doCallRealMethod().when(observer).setup(notNull());
        doCallRealMethod().when(secondObserver).setup(notNull());

        // Step 1: make fileobserver bundle watching R by
        // registering an appropriate service.
        final InitialCheckusmCalculationBarrier wait = new InitialCheckusmCalculationBarrier();
        fileObserverRegistration = context.registerService(FileObserver.class, wait, null);
        watchedDirectory = create(ROOT, R);
        watchedDirectoryRegistration = context.registerService(WatchedDirectory.class, watchedDirectory, null);
        wait.waitUntilChecksumsCalculated();
        fileObserverRegistration.unregister();

        // Step 2: register FileObserver
        fileObserverRegistration = context.registerService(FileObserver.class, observer, null);
        verifyForceInform(observer);
        reset(observer);

        // Step 3: register key-hook
        hookRegistration = context.registerService(KeyDeliveryHook.class, hook, null);
    }

    private void verifyForceInform(final FileObserver pObserver) throws Exception {
        verify(pObserver, timeout(5000)).modified(key(ROOT, R.relativize(E11)), eq(E11));
        verify(pObserver, timeout(5000)).modified(key(ROOT, R.relativize(E12)), eq(E12));
        verify(pObserver, timeout(5000)).modified(key(ROOT, R.relativize(E2)), eq(E2));
        verify(pObserver, timeout(5000)).modified(key(ROOT, R.relativize(H11)), eq(H11));
        verify(pObserver, timeout(5000)).modified(key(ROOT, R.relativize(H12)), eq(H12));
        verify(pObserver, timeout(5000)).modified(key(ROOT, R.relativize(H2)), eq(H2));
        verify(pObserver, timeout(5000)).modified(key(ROOT, R.relativize(C)), eq(C));
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
        unregisterService(fileObserverRegistration);
        unregisterService(secondObserverRegistration);
        unregisterService(hookRegistration);
    }

    @Test
    public void doExlusivelyInformNewlyRegisteredObserver() throws Exception {
        secondObserverRegistration = context.registerService(FileObserver.class, secondObserver, null);
        verifyForceInform(secondObserver);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void insureNoInteractionWithUnregisteredFileObserver() throws Exception {
        fileObserverRegistration.unregister();

        delete(E11);
        delete(E12);
        delete(E2);
        delete(H11);
        delete(H12);
        delete(H2);
        delete(C);

        sleep(6000);
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void unregisterAndRegisterAdditionalWatchedDirectory() throws Exception {
        // Insure observer gets informed about unregistration
        watchedDirectoryRegistration.unregister();
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(R)));

        // Now, observer should be informed about newly registered root
        watchedDirectoryRegistration = context.registerService(WatchedDirectory.class, watchedDirectory, null);

        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(E11)), eq(E11));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(E12)), eq(E12));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(E2)), eq(E2));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(H11)), eq(H11));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(H12)), eq(H12));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(H2)), eq(H2));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(C)), eq(C));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void observerShouldBeInformedAboutFileDeletion() throws IOException {
        delete(E11);
        delete(E12);
        delete(E2);
        delete(H11);
        delete(H12);
        delete(H2);
        delete(C);
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(E11)));
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(E12)));
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(E2)));
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(H11)));
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(H12)));
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(H2)));
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(C)));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void observerShouldBeInformedAboutDirectoryDeletion() throws IOException {
        deleteDirectory(E1);
        deleteDirectory(H1);
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(E1)));
        verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(H1)));

        // See FileObserver::discard for explanation; the following works not for MacOS X
        if ("Linux".equals(System.getProperty("os.name"))) {
            verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(E11)));
            verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(E12)));
            verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(H11)));
            verify(observer, timeout(15000)).discard(key(ROOT, R.relativize(H12)));
        }

        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void observerShouldBeInformedAboutFileChange() throws IOException {
        writeArbitraryContent(E12);
        writeArbitraryContent(H12);
        writeArbitraryContent(C);

        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(E12)), eq(E12));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(H12)), eq(H12));
        verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(C)), eq(C));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void observerShouldBeInformedAboutFileCreation() throws IOException {
        final Path newFile = E1.resolve("newFile.txt");
        writeArbitraryContent(newFile);

        final InOrder order = inOrder(hook, observer);
        order.verify(hook, timeout(15000)).beforeModify(key(ROOT, R.relativize(newFile)), eq(newFile));
        order.verify(observer, timeout(15000)).modified(key(ROOT, R.relativize(newFile)), eq(newFile));
        order.verify(hook, timeout(15000)).afterModify(key(ROOT, R.relativize(newFile)), eq(newFile));

        verifyNoMoreInteractions(observer);
    }
}
