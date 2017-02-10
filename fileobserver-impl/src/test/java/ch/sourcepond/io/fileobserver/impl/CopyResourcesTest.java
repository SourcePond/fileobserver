package ch.sourcepond.io.fileobserver.impl;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;

/**
 *
 */
public abstract class CopyResourcesTest {
    protected static final String TEST_FILE_TXT_NAME = "testfile.txt";
    protected static final String TEST_FILE_XML_NAME = "testfile.xml";
    protected static final String SUB_DIR_NAME = "subdir";
    protected final FileSystem fs = FileSystems.getDefault();
    private final Path sourceDir = fs.getPath(System.getProperty("user.directory"), "src", "test", "resources");
    protected final Path directory = fs.getPath(System.getProperty("java.io.tmpdir"), getClass().getName(), UUID.randomUUID().toString());
    protected Path subDirectory;
    protected Path testfileTxt;
    protected Path testfileXml;

    @Before
    public final void copyResources() throws Exception {
        createDirectories(directory);
        walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path relativePath = sourceDir.relativize(file);
                final Path targetFile = directory.resolve(relativePath);
                createDirectories(targetFile.getParent());
                copy(file, targetFile);
                return CONTINUE;
            }
        });
        testfileTxt = directory.resolve("testfile.txt");
        subDirectory = directory.resolve("subdir");
        testfileXml = subDirectory.resolve("testfile.xml");
    }

    @After
    public void deleteResources() throws IOException {
        walkFileTree(directory, new SimpleFileVisitor<Path>() {
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
}
