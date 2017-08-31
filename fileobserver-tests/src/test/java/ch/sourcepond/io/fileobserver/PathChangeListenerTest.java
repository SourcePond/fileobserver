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

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.testing.BundleContextClassLoaderRule;
import org.junit.*;
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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Random;

import static ch.sourcepond.io.fileobserver.RecursiveDeletion.deleteDirectory;
import static ch.sourcepond.io.fileobserver.spi.WatchedDirectory.create;
import static ch.sourcepond.testing.OptionsHelper.karafContainer;
import static ch.sourcepond.testing.OptionsHelper.mockitoBundles;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Files.newOutputStream;
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
public class PathChangeListenerTest extends DirectorySetup {
    private static final String ROOT = "watchedRoot";
    private static final Random RANDOM = new Random();

    @Rule
    public BundleContextClassLoaderRule rule = new BundleContextClassLoaderRule(this);

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

    private static DispatchKey key(final Object pKey, final Path pRelativePath) {
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

    private static PathChangeEvent event(final Object pKey, final Path pRelativePath) {
        return argThat(new ArgumentMatcher<PathChangeEvent>() {
            @Override
            public boolean matches(final PathChangeEvent event) {
                return isKeyEqual(event.getKey(), pKey, pRelativePath);
            }

            @Override
            public String toString() {
                return format("event [%s, %s]", pKey, pRelativePath);
            }
        });
    }


    private static void writeArbitraryContent(final Path pFile) throws Exception {
        try (final OutputStream out = newOutputStream(pFile)) {
            out.write(RANDOM.nextInt());
        }
        sleep(1000);
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

    private <T> ServiceRegistration<T> registerService(final Class<T> pInterface, final T pService) throws Exception {
        final ServiceRegistration<T> reg = context.registerService(pInterface, pService, null);
        sleep(1500);
        return reg;
    }

    @Before
    public void setup() throws Exception {
        doCallRealMethod().when(listener).restrict(notNull(), same(root.getFileSystem()));
        doCallRealMethod().when(secondListener).restrict(notNull(), same(root.getFileSystem()));

        // Step 1: make fileobserver bundle watching root by
        // registering an appropriate service.
        watchedDirectory = create(ROOT, root);
        watchedDirectory.addBlacklistPattern("glob:" + ZIP_NAME);

        watchedDirectoryRegistration = registerService(WatchedDirectory.class, watchedDirectory);

        // Step 2: register PathChangeListener
        listenerRegistration = registerService(PathChangeListener.class, listener);

        // Step 3: register key-hook
        hookRegistration = registerService(KeyDeliveryHook.class, hook);
        reset(listener, hook);
    }

    private void verifyForceInform(final PathChangeListener pListener) throws Exception {
        verify(pListener, timeout(15000)).modified(event(ROOT, root.relativize(root_etc_network_networkConf)));
        verify(pListener, timeout(15000)).modified(event(ROOT, root.relativize(root_etc_network_dhcpConf)));
        verify(pListener, timeout(15000)).modified(event(ROOT, root.relativize(root_etc_manConf)));
        verify(pListener, timeout(15000)).modified(event(ROOT, root.relativize(root_home_jeff_documentTxt)));
        verify(pListener, timeout(15000)).modified(event(ROOT, root.relativize(root_home_jeff_letterXml)));
        verify(pListener, timeout(15000)).modified(event(ROOT, root.relativize(root_home_indexIdx)));
        verify(pListener, timeout(15000)).modified(event(ROOT, root.relativize(root_configProperties)));
    }

    private void unregisterService(final ServiceRegistration<?> pRegistration) throws Exception {
        if (pRegistration != null) {
            try {
                pRegistration.unregister();
            } catch (final IllegalStateException e) {
                // Ignore
            }
            sleep(1000);
        }
    }

    @After
    public void tearDown() throws Exception {
        unregisterService(watchedDirectoryRegistration);
        unregisterService(listenerRegistration);
        unregisterService(secondListenerRegistration);
        unregisterService(hookRegistration);
    }

    @Test
    public void doExlusivelyInformNewlyRegisteredListener() throws Exception {
        secondListenerRegistration = registerService(PathChangeListener.class, secondListener);
        verifyForceInform(secondListener);
        verifyZeroInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void insureNoInteractionWithUnregisteredFileListener() throws Exception {
        listenerRegistration.unregister();

        delete(root_etc_network_networkConf);
        delete(root_etc_network_dhcpConf);
        delete(root_etc_manConf);
        delete(root_home_jeff_documentTxt);
        delete(root_home_jeff_letterXml);
        delete(root_home_indexIdx);
        delete(root_configProperties);

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
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root)));

        // Now, listener should be informed about newly registered root
        watchedDirectoryRegistration = registerService(WatchedDirectory.class, watchedDirectory);

        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_etc_network_networkConf)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_etc_network_dhcpConf)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_etc_manConf)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_home_jeff_documentTxt)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_home_jeff_letterXml)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_home_indexIdx)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_configProperties)));
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutFileDeletion() throws IOException {
        delete(root_etc_network_networkConf);
        delete(root_etc_network_dhcpConf);
        delete(root_etc_manConf);
        delete(root_home_jeff_documentTxt);
        delete(root_home_jeff_letterXml);
        delete(root_home_indexIdx);
        delete(root_configProperties);
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_etc_network_networkConf)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_etc_network_dhcpConf)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_etc_manConf)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_home_jeff_documentTxt)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_home_jeff_letterXml)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_home_indexIdx)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_configProperties)));
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutDirectoryDeletion() throws IOException {
        deleteWatchedResources();
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_etc_network_networkConf)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_etc_network_dhcpConf)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_etc_manConf)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_home_indexIdx)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_home_jeff_documentTxt)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_home_jeff_letterXml)));
        verify(listener, timeout(15000)).discard(key(ROOT, root.relativize(root_configProperties)));
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutFileChange() throws Exception {
        writeArbitraryContent(root_etc_network_dhcpConf);
        writeArbitraryContent(root_home_jeff_letterXml);
        writeArbitraryContent(root_configProperties);

        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_etc_network_dhcpConf)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_home_jeff_letterXml)));
        verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(root_configProperties)));
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void listenerShouldBeInformedAboutFileCreation() throws Exception {
        final Path newFile = root_etc_network.resolve("newFile.txt");
        writeArbitraryContent(newFile);

        final InOrder order = inOrder(hook, listener);
        order.verify(hook, timeout(15000)).beforeModify(key(ROOT, root.relativize(newFile)), eq(newFile));
        order.verify(listener, timeout(15000)).modified(event(ROOT, root.relativize(newFile)));
        order.verify(hook, timeout(15000)).afterModify(key(ROOT, root.relativize(newFile)), eq(newFile));

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void listenerShouldBeInformedAboutFileCreationThroughUnzip() throws Exception {
        listenerShouldBeInformedAboutDirectoryDeletion();

        reset(listener);
        sleep(1000);
        createZip();
        unzip();
        verifyForceInform(listener);
    }
}
