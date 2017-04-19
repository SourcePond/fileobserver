/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fileobserver.impl;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;
import static java.util.UUID.randomUUID;

/**
 *
 */
public abstract class CopyResourcesTest {
    protected static final String TEST_FILE_TXT_NAME = "testfile.txt";
    protected static final String TEST_FILE_XML_NAME = "testfile_11.xml";
    protected static final String SUB_DIR_NAME = "subdir_1";
    private final Path sourceDir = getDefault().getPath(getProperty("user.dir"), "src", "test", "resources");
    protected final Path root_dir_path = createRootPath();
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

    /**
     * Creates the target root path; can also be located on a different file-system!
     * @return
     */
    protected Path createRootPath() {
        return getDefault().getPath(getProperty("java.io.tmpdir"), getClass().getName(), randomUUID().toString());
    }

    @Before
    public final void copyResources() throws Exception {
        createDirectories(root_dir_path);
        walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path relativePath = sourceDir.relativize(file);
                final Path targetFile = root_dir_path.resolve(relativePath.toString());
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

    protected void deleteDirectory(final Path pDirectory) throws IOException {
        walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {
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

    @After
    public void deleteResources() throws IOException {
        if (exists(root_dir_path)) {
            deleteDirectory(root_dir_path);
        }
    }
}
