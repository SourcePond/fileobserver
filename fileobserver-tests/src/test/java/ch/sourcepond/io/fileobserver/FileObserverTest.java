package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.testing.BundleContextClassLoaderRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
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
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf")
                .artifactId("apache-karaf").versionAsInProject()
                .type("tar.gz");
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
                return pKey.equals(fileKey.key()) && fileKey.relativePath().equals(pRelativePath);
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

    private final FileObserver observer = mock(FileObserver.class);
    private ServiceRegistration<WatchedDirectory> watchedDirectoryRegistration;
    private ServiceRegistration<FileObserver> fileObserverRegistration;
    private WatchedDirectory watchedDirectory;

    @Before
    public void setup() throws Exception {
        // Step 1: make fileobserver bundle watching R by
        // registering an appropriate service.
        final InitialCheckusmCalculationBarrier wait = new InitialCheckusmCalculationBarrier();
        final ServiceRegistration<FileObserver> waitReg = fileObserverRegistration = context.registerService(FileObserver.class, wait, null);
        watchedDirectory = create(ROOT, R);
        watchedDirectoryRegistration = context.registerService(WatchedDirectory.class, watchedDirectory, null);
        wait.waitUntilChecksumsCalculated();
        waitReg.unregister();

        // Step 2: register FileObserver
        fileObserverRegistration = context.registerService(FileObserver.class, observer, null);

        verify(observer, timeout(500)).modified(key(ROOT, R.relativize(E11)), eq(E11));
        verify(observer, timeout(500)).modified(key(ROOT, R.relativize(E12)), eq(E12));
        verify(observer, timeout(500)).modified(key(ROOT, R.relativize(E2)), eq(E2));
        verify(observer, timeout(500)).modified(key(ROOT, R.relativize(H11)), eq(H11));
        verify(observer, timeout(500)).modified(key(ROOT, R.relativize(H12)), eq(H12));
        verify(observer, timeout(500)).modified(key(ROOT, R.relativize(H2)), eq(H2));
        verify(observer, timeout(500)).modified(key(ROOT, R.relativize(C)), eq(C));

        reset(observer);
    }

    private void unregisterService(final ServiceRegistration<?> pRegistration) {
        try {
            pRegistration.unregister();
        } catch (final IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        unregisterService(watchedDirectoryRegistration);
        unregisterService(fileObserverRegistration);
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

        verify(observer, timeout(25000)).modified(key(ROOT, R.relativize(E12)), eq(E12));
        verify(observer, timeout(25000)).modified(key(ROOT, R.relativize(H12)), eq(H12));
        verify(observer, timeout(25000)).modified(key(ROOT, R.relativize(C)), eq(C));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void observerShouldBeInformedAboutFileCreation() throws IOException {
        final Path newFile = E1.resolve("newFile.txt");
        writeArbitraryContent(newFile);
        verify(observer, timeout(25000)).modified(key(ROOT, R.relativize(newFile)), eq(newFile));
        verifyNoMoreInteractions(observer);
    }
}
