package ch.sourcepond.io.fileobserver.impl.filekey;

import ch.sourcepond.io.fileobserver.api.FileKey;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Objects;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY1;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Created by rolandhauser on 13.02.17.
 */
public class DefaultFileKeyTest {
    private final Path path = mock(Path.class);
    private final Path otherPath = mock(Path.class);
    private final FileKey key1 = new FileKeyFactory().newKey(TEST_KEY, path);
    private final FileKey key2 = new FileKeyFactory().newKey(TEST_KEY, path);
    private final FileKey key3 = new FileKeyFactory().newKey(TEST_KEY, otherPath);
    private final FileKey key4 = new FileKeyFactory().newKey(TEST_KEY1, otherPath);

    @Test
    public void key() {
        assertSame(TEST_KEY, key1.key());
    }

    @Test
    public void relativePath() {
        assertSame(path, key1.relativePath());
    }

    @Test
    public void verifyEquals() {
        assertTrue(key1.equals(key1));
        assertFalse(key1.equals(null));
        assertFalse(key1.equals(new Object()));
        assertTrue(key1.equals(key2));
        assertFalse(key1.equals(key3));
        assertFalse(key2.equals(key4));
    }

    @Test
    public void verifyHashCode() {
        assertEquals(Objects.hash(TEST_KEY, path), key1.hashCode());
    }
}
