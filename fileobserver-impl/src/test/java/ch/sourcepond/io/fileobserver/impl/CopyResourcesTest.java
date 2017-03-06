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
    protected static final String TEST_FILE_XML_NAME = "testfile_11.xml";
    protected static final String SUB_DIR_NAME = "subdir_1";
    protected final FileSystem fs = FileSystems.getDefault();
    private final Path sourceDir = fs.getPath(System.getProperty("user.dir"), "src", "test", "resources");
    protected final Path root_dir = fs.getPath(System.getProperty("java.io.tmpdir"), getClass().getName(), UUID.randomUUID().toString());
    protected Path subdir_1;
    protected Path subdir_11;
    protected Path testfile_111_txt;
    protected Path subdir_12;
    protected Path testfile_121_txt;
    protected Path testfile_11_xml;
    protected Path subdir_2;
    protected Path subdir_21;
    protected Path testfile_211_txt;
    protected Path subdir_22;
    protected Path testfile_221_txt;
    protected Path testfile_21_txt;
    protected Path testfile_txt;



    @Before
    public final void copyResources() throws Exception {
        createDirectories(root_dir);
        walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path relativePath = sourceDir.relativize(file);
                final Path targetFile = root_dir.resolve(relativePath);
                createDirectories(targetFile.getParent());
                copy(file, targetFile);
                return CONTINUE;
            }
        });
        subdir_1 = root_dir.resolve("subdir_1");
        subdir_11 = subdir_1.resolve("subdir_11");
        testfile_111_txt = subdir_11.resolve("testfile_111.txt");
        subdir_12 = subdir_1.resolve("subdir_12");
        testfile_121_txt = subdir_12.resolve("testfile_121.txt");
        testfile_11_xml = subdir_1.resolve("testfile_11.xml");
        subdir_2 = root_dir.resolve("subdir_2");
        subdir_21 = subdir_2.resolve("subdir_21");
        testfile_211_txt = subdir_21.resolve("testfile_211.txt");
        subdir_22 = subdir_2.resolve("subdir_22");
        testfile_221_txt = subdir_22.resolve("testfile_221.txt");
        testfile_21_txt = subdir_2.resolve("testfile_21.xml");
        testfile_txt = root_dir.resolve("testfile.txt");
    }

    @After
    public void deleteResources() throws IOException {
        if (Files.exists(root_dir)) {
            walkFileTree(root_dir, new SimpleFileVisitor<Path>() {
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
}
