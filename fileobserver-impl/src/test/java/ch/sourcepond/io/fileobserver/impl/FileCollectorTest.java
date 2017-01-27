package ch.sourcepond.io.fileobserver.impl;

import org.junit.Test;

import java.nio.file.Path;
import java.util.function.Consumer;

import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.walkFileTree;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class FileCollectorTest {
    private final Consumer<Path> consumer = mock(Consumer.class);
    private final Path rootDirectory = getDefault().getPath("src", "test", "resources");
    private final Path expected1 = rootDirectory.resolve("subdir").resolve("testfile.xml");
    private final Path expected2 = rootDirectory.resolve("testfile.txt");

    @Test
    public void verifyCollectFiles() throws Exception {
        final Path rootDirectory = getDefault().getPath("src", "test", "resources");
        final FileCollector collector = new FileCollector(rootDirectory, consumer);
        walkFileTree(rootDirectory, collector);
        verify(consumer).accept(rootDirectory.relativize(expected1));
        verify(consumer).accept(rootDirectory.relativize(expected2));
    }
}
