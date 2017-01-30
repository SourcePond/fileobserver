package ch.sourcepond.io.fileobserver.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.mockito.Mockito.mock;

/**
 * Created by rolandhauser on 30.01.17.
 */
public class WatchEventProducerTest {
    private final ResourceEventProducer resourceEventProducer = mock(ResourceEventProducer.class);
    private final FileSystem fs = getDefault();
    private final Path sourceDir = fs.getPath(System.getProperty("user.dir"), "src", "test", "resources");
    private final Path targetDir = fs.getPath(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
    private WatchService watchService;

    @Before
    public void setup() throws IOException {
        watchService = getDefault().newWatchService();
        createDirectories(targetDir);
        walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path relativePath = sourceDir.relativize(file);
                final Path targetFile = targetDir.resolve(relativePath);
                createDirectories(targetFile.getParent());
                copy(file, targetFile);
                return CONTINUE;
            }
        });
        targetDir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    }

    @After
    public void tearDown() throws IOException {
        walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                delete(dir);
                return CONTINUE;
            }
        });
    }

    @Test
    public void test() {

    }


}
