package ch.sourcepond.io.fileobserver;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.io.File;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

/**
 * Created by rolandhauser on 09.02.17.
 */
@RunWith(PaxExam.class)
public class FileObserverTest {

    @Configuration
    public Option[] config() {
        return new Option[] {
                // Provision and launch a container based on a distribution of Karaf (Apache ServiceMix).
                karafDistributionConfiguration()
                        .frameworkUrl(
                                maven()
                                        .groupId("org.apache.karaf")
                                        .artifactId("apache-karaf")
                                        .type("tar.gz")
                                        .version("4.1.0"))
                        .karafVersion("4.1.0")
                        .name("Apache Karaf Minimal")
                        .unpackDirectory(new File("target/pax"))
                        .useDeployFolder(false),
                // It is really nice if the container sticks around after the test so you can check the contents
                // of the data directory when things go wrong.
                keepRuntimeFolder(),
                // Don't bother with local console output as it just ends up cluttering the logs
                configureConsole().ignoreLocalConsole(),
                // Force the log level to INFO so we have more details during the test.  It defaults to WARN.
                logLevel(LogLevelOption.LogLevel.DEBUG),
                // Provision the example feature exercised by this test
                features(
                        "mvn:ch.sourcepond.io/fileobserver-feature/1.0-SNAPSHOT/xml/features", "checksum-feature"),
                // Remember that the test executes in another process.  If you want to debug it, you need
                // to tell Pax Exam to launch that process with debugging enabled.  Launching the test class itself with
                // debugging enabled (for example in Eclipse) will not get you the desired results.
                //debugConfiguration("5000", true),
        };
    }

    @Inject
    private BundleContext context;

    @Test
    public void doSometing() {
        final FileObserver observer = Mockito.mock(FileObserver.class);
    }

}
