package ch.sourcepond.io.fileobserver.spi;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class WatchedDirectoryTest {

    private enum TestKey {
        TEST_KEY
    }

    private final Path path = mock(Path.class);

    @Test(expected = NullPointerException.class)
    public void createKeyIsNull() {
        WatchedDirectory.create(null, path);
    }

    @Test(expected = NullPointerException.class)
    public void createDirectoryIsNull() {
        WatchedDirectory.create(TestKey.TEST_KEY, null);
    }

    @Test
    public void create() {
        final WatchedDirectory dir = WatchedDirectory.create(TestKey.TEST_KEY, path);
        assertSame(TestKey.TEST_KEY, dir.getKey());
        assertSame(path, dir.getDirectory());
    }
}
