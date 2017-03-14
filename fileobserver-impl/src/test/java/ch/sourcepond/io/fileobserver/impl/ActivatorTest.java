package ch.sourcepond.io.fileobserver.impl;


import ch.sourcepond.io.fileobserver.impl.fs.VirtualRoot;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.mock;

/**
 * This test simply call the start method and checks that
 * no exception is thrown. The integration test actually test
 * the activator in depth.
 */
public class ActivatorTest {
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final BundleContext context = mock(BundleContext.class);
    private final Activator activator = new Activator(virtualRoot);

    @Test
    public void verifyDefaultConstructor() {
        // Should not cause an exception
        new Activator();
    }

    @Test
    public void init() throws Exception {
        // No exception should be caused to be thrown
        activator.start(context);
    }
}
