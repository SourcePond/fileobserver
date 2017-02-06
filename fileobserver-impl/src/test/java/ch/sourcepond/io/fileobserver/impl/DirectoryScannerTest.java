package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.ResourceObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DirectoryScannerTest {
    private static final String NEW_FILE_NAME = "newfile.txt";
    private static final String TEST_FILE_TXT_NAME = "testfile.txt";
    private static final String TEST_FILE_XML_NAME = "testfile.xml";
    private static final String SUB_DIR_NAME = "subdir";
    private static final String NEW_SUB_DIR_NAME = "newsubdir";
    private final ResourceObserver observer = mock(ResourceObserver.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Directories directories = new Directories(new ObserverHandlerFactory(executor), new FsDirectoriesFactory());
    private final DirectoryScanner scanner = new DirectoryScanner(executor, directories);
    private final FileSystem fs = FileSystems.getDefault();
    private final Path sourceDir = fs.getPath(System.getProperty("user.dir"), "src", "test", "resources");
    private final Path targetDir = fs.getPath(System.getProperty("java.io.tmpdir"), getClass().getName(), UUID.randomUUID().toString());

    @Before
    public void setup() throws Exception {
        when(observer.accept(anyString(), any())).thenReturn(true);
        directories.addObserver(observer);
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
        directories.addRoot(targetDir);
        scanner.start();

        // Necessary for on macOS (polling watch-service)
        // TODO: remove this when JDK for macOS uses native FS facilities
        Thread.sleep(1000);
    }


    @After
    public void tearDown() throws IOException {
        directories.removeRoot(targetDir);
        scanner.close();
        executor.shutdown();
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

    private void changeContent(final Path pPath) throws Exception {
        try (final BufferedWriter writer = Files.newBufferedWriter(pPath, CREATE)) {
            writer.write(UUID.randomUUID().toString());
        }
        Thread.sleep(5000);
    }

    private String toKey(final Path pPath) throws IOException {
        return targetDir.relativize(pPath).toString();
    }

    private void fileModification(final Path pPath) throws Exception {
        changeContent(pPath);
        verify(observer, timeout(5000)).modified(toKey(pPath), pPath);
    }

    private void fileDelete(final Path pPath) throws IOException {
        Files.delete(pPath);
        verify(observer, timeout(5000)).deleted(toKey(pPath));
    }

    @Test
    public void processFileCreateInRootDirectory() throws Exception {
        fileModification(targetDir.resolve(NEW_FILE_NAME));
    }

    @Test
    public void processFileModificationInRootDirectory() throws Exception {
        final Path path = targetDir.resolve(TEST_FILE_TXT_NAME);
        changeContent(path);
        Thread.sleep(5000);
        verify(observer, times(2)).modified(toKey(path), path);
    }

    @Test
    public void processFileDeleteInRootDirectory() throws IOException {
        fileDelete(targetDir.resolve(TEST_FILE_TXT_NAME));
    }

    @Test
    public void processFileCreateInSubDirectory() throws Exception {
        fileModification(targetDir.resolve(SUB_DIR_NAME).resolve(NEW_FILE_NAME));
    }

    @Test
    public void processFileModificationInSubDirectory() throws Exception {
        fileModification(targetDir.resolve(SUB_DIR_NAME).resolve(TEST_FILE_TXT_NAME));
    }

    @Test
    public void processFileDeleteInSubDirectory() throws IOException {
        fileDelete(targetDir.resolve(SUB_DIR_NAME).resolve(TEST_FILE_XML_NAME));
    }

    private Path createSubDirectory(final Path pFile) throws Exception {
        Files.createDirectories(pFile.getParent());
        // Necessary for on macOS (polling watch-service)
        // TODO: remove this when JDK for macOS uses native FS facilities
        Thread.sleep(3000);
        return pFile;
    }

    @Test
    public void processFileCreateInNewSubDirectory() throws Exception {
        fileModification(createSubDirectory(targetDir.resolve(NEW_SUB_DIR_NAME).resolve(NEW_FILE_NAME)));
    }

    @Test
    public void processFileDeleteInNewSubDirectory() throws Exception {
        final Path path = createSubDirectory(targetDir.resolve(NEW_SUB_DIR_NAME).resolve(NEW_FILE_NAME));
        changeContent(path);

        // Necessary for on macOS (polling watch-service)
        // TODO: remove this when JDK for macOS uses native FS facilities
        Thread.sleep(3000);

        fileDelete(path);
    }

    @Test
    public void startListenAgainAfterSubDirectoryDeletionAndRecreation() throws Exception {
        final Path file = createSubDirectory(targetDir.resolve(NEW_SUB_DIR_NAME).resolve(NEW_FILE_NAME));
        changeContent(file);
        verify(observer, timeout(5000)).modified(toKey(file), file);
        fileDelete(file);

        // Necessary for on macOS (polling watch-service)
        // TODO: remove this when JDK for macOS uses native FS facilities
        Thread.sleep(3000);

        // Delete parent directory
        Files.delete(file.getParent());

        // Necessary for on macOS (polling watch-service)
        // TODO: remove this when JDK for macOS uses native FS facilities
        Thread.sleep(3000);

        createSubDirectory(file);
        changeContent(file);
        Thread.sleep(3000);
        verify(observer, times(2)).modified(toKey(file), file);
    }



}
