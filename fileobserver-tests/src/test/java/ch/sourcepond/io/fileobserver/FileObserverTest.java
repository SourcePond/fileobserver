package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import ch.sourcepond.testing.BundleContextClassLoaderRule;
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

import javax.inject.Inject;
import java.nio.file.Path;

import static ch.sourcepond.io.fileobserver.DirectoryKey.ROOT;
import static ch.sourcepond.io.fileobserver.DirectorySetup.*;
import static ch.sourcepond.io.fileobserver.spi.WatchedDirectory.create;
import static ch.sourcepond.testing.OptionsHelper.karafContainer;
import static ch.sourcepond.testing.OptionsHelper.mockitoBundles;
import static java.lang.String.format;
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

    @Inject
    private BundleContext context;

    /**
     * Registers a new observer.
     *
     * <h3>Preconditions</h3>
     * <ul>
     *     <li>Fileobserver bundle is watching {@link DirectorySetup#R}</li>
     *     <li>No observers have been added yet</li>
     * </ul>
     *
     * <h3>Expected result</h3>
     * {@link ch.sourcepond.io.fileobserver.api.FileObserver#modified(FileKey, Path)} has been called for every
     * file contained in {@link DirectorySetup#R} and its sub-directories.
     *
     * @throws Exception
     */
    @Test
    public void registerObserver() throws Exception {
        // Step 1: make fileobserver bundle watching R by
        // registering an appropriate service.
        final WatchedDirectory r = create(ROOT, R);
        context.registerService(WatchedDirectory.class, r, null);

        // Step 2: register FileObserver
        final FileObserver observer = mock(FileObserver.class);
        context.registerService(FileObserver.class, observer, null);

        verify(observer, timeout(2000)).modified(key(ROOT, R.relativize(E11)), eq(E11));
        verify(observer, timeout(2000)).modified(key(ROOT, R.relativize(E12)), eq(E12));
        verify(observer, timeout(2000)).modified(key(ROOT, R.relativize(E2)), eq(E2));
        verify(observer, timeout(2000)).modified(key(ROOT, R.relativize(H11)), eq(H11));
        verify(observer, timeout(2000)).modified(key(ROOT, R.relativize(H12)), eq(H12));
        verify(observer, timeout(2000)).modified(key(ROOT, R.relativize(H2)), eq(H2));
        verify(observer, timeout(2000)).modified(key(ROOT, R.relativize(C)), eq(C));
        verifyNoMoreInteractions(observer);
    }
}
