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
    protected final Path root_dir_path = fs.getPath(System.getProperty("java.io.tmpdir"), getClass().getName(), UUID.randomUUID().toString());
    protected Path subdir_1_path;
    protected Path subdir_11_path;
    protected Path subdir_111_path;
    protected Path testfile_1111_txt_path;
    protected Path testfile_111_txt_path;
    protected Path subdir_12_path;
    protected Path testfile_121_txt_path;
    protected Path testfile_11_xml_path;
    protected Path subdir_2_path;
    protected Path subdir_21_path;
    protected Path subdir_211_path;
    protected Path testfile_2111_txt_path;
    protected Path testfile_211_txt_path;
    protected Path subdir_22_path;
    protected Path testfile_221_txt_path;
    protected Path testfile_21_xml_path;
    protected Path testfile_txt_path;

    @Before
    public final void copyResources() throws Exception {
        createDirectories(root_dir_path);
        walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path relativePath = sourceDir.relativize(file);
                final Path targetFile = root_dir_path.resolve(relativePath);
                createDirectories(targetFile.getParent());
                copy(file, targetFile);
                return CONTINUE;
            }
        });
        subdir_1_path = root_dir_path.resolve("subdir_1");
        subdir_11_path = subdir_1_path.resolve("subdir_11");
        subdir_111_path = subdir_11_path.resolve("subdir_111");
        testfile_1111_txt_path = subdir_111_path.resolve("testfile_1111.txt");
        testfile_111_txt_path = subdir_11_path.resolve("testfile_111.txt");
        subdir_12_path = subdir_1_path.resolve("subdir_12");
        testfile_121_txt_path = subdir_12_path.resolve("testfile_121.txt");
        testfile_11_xml_path = subdir_1_path.resolve("testfile_11.xml");
        subdir_2_path = root_dir_path.resolve("subdir_2");
        subdir_21_path = subdir_2_path.resolve("subdir_21");
        subdir_211_path = subdir_21_path.resolve("subdir_211");
        testfile_2111_txt_path = subdir_211_path.resolve("testfile_2111.txt");
        testfile_211_txt_path = subdir_21_path.resolve("testfile_211.txt");
        subdir_22_path = subdir_2_path.resolve("subdir_22");
        testfile_221_txt_path = subdir_22_path.resolve("testfile_221.txt");
        testfile_21_xml_path = subdir_2_path.resolve("testfile_21.xml");
        testfile_txt_path = root_dir_path.resolve("testfile.txt");
    }

    @After
    public void deleteResources() throws IOException {
        if (Files.exists(root_dir_path)) {
            walkFileTree(root_dir_path, new SimpleFileVisitor<Path>() {
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
