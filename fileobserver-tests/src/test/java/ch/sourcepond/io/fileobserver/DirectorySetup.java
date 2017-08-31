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
package ch.sourcepond.io.fileobserver;

import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static ch.sourcepond.io.fileobserver.RecursiveDeletion.deleteDirectory;
import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;
import static org.junit.Assert.*;

/**
 * <pre>
 *     + Root folder (root)
 *        + etc (root_etc)
 *        |   + network (root_etc_network)
 *        |   |    + network.conf (root_etc_network_networkConf)
 *        |   |    + dhcp.conf (root_etc_network_dhcpConf)
 *        |   + man.conf (root_etc_manConf)
 *        + home (root_home)
 *        |   + jeff (root_home_jeff)
 *        |   |   + document.txt (root_home_jeff_documentTxt)
 *        |   |   + letter.xml (root_home_jeff_letterXml)
 *        |   + index.idx (root_home_indexIdx)
 *        + config.properties (root_configProperties)
 * </pre>
 */
public class DirectorySetup {
    static final String ZIP_NAME = "hotdeploy.zip";
    private static final Path SOURCE = getDefault().getPath(getProperty("user.dir"), "src", "test", "resources", "testdir");
    public final Path root = getDefault().getPath(getProperty("java.io.tmpdir"), DirectorySetup.class.getName(), UUID.randomUUID().toString());
    public final Path root_etc = root.resolve("etc");
    public final Path root_etc_network = root_etc.resolve("network");
    public final Path root_etc_network_networkConf = root_etc_network.resolve("network.conf");
    public final Path root_etc_network_dhcpConf = root_etc_network.resolve("dhcp.conf");
    public final Path root_etc_manConf = root_etc.resolve("man.conf");
    public final Path root_home = root.resolve("home");
    public final Path root_home_jeff = root_home.resolve("jeff");
    public final Path root_home_jeff_documentTxt = root_home_jeff.resolve("document.txt");
    public final Path root_home_jeff_letterXml = root_home_jeff.resolve("letter.xml");
    public final Path root_home_indexIdx = root_home.resolve("index.idx");
    public final Path root_configProperties = root.resolve("config.properties");
    private final Path zipFile = root.resolve(ZIP_NAME);

    private void validate() {
        assertTrue(root.toString(), isDirectory(root));
        assertTrue(root_etc.toString(), isDirectory(root_etc));
        assertTrue(root_etc_network.toString(), isDirectory(root_etc_network));
        assertTrue(root_etc_network_networkConf.toString(), isRegularFile(root_etc_network_networkConf));
        assertTrue(root_etc_network_dhcpConf.toString(), isRegularFile(root_etc_network_dhcpConf));
        assertTrue(root_etc_manConf.toString(), isRegularFile(root_etc_manConf));
        assertTrue(root_home.toString(), isDirectory(root_home));
        assertTrue(root_home_jeff.toString(), isDirectory(root_home_jeff));
        assertTrue(root_home_jeff_documentTxt.toString(), isRegularFile(root_home_jeff_documentTxt));
        assertTrue(root_home_jeff_letterXml.toString(), isRegularFile(root_home_jeff_letterXml));
        assertTrue(root_home_indexIdx.toString(), isRegularFile(root_home_indexIdx));
        assertTrue(root_configProperties.toString(), isRegularFile(root_configProperties));
    }

    private void unzipEntry(final ZipEntry pEntry, final InputStream pIn) throws IOException {
        if (!pEntry.isDirectory()) {
            final Path target = root.resolve(pEntry.getName());
            createDirectories(target.getParent());
            copy(pIn, target);
        }
    }

    void unzip() throws IOException {
        try (final ZipInputStream in = new ZipInputStream(newInputStream(root.resolve(ZIP_NAME)))) {
            ZipEntry next = in.getNextEntry();
            while (next != null) {
                unzipEntry(next, in);
                next = in.getNextEntry();
            }
        }
    }

    public void nativeUnzip() throws InterruptedException,  IOException {
        final ProcessBuilder pb = new ProcessBuilder("unzip",
                "-o",
                zipFile.toString(),
                "-d",
                root.toString());
        final Process process = pb.start();
        final int errCode = process.waitFor();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
        assertEquals("Process returned error", 0, errCode);
    }

    void createZip() throws IOException {
        try (final ZipOutputStream out = new ZipOutputStream(newOutputStream(zipFile))) {
            walkFileTree(SOURCE, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    out.putNextEntry(new ZipEntry(SOURCE.relativize(file).toString()));
                    copy(file, out);
                    out.closeEntry();
                    return CONTINUE;
                }
            });
        }
    }


    private void copyDirectories() throws IOException {
        walkFileTree(SOURCE, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path relativePath = SOURCE.relativize(file);
                final Path targetPath = root.resolve(relativePath);
                createDirectories(targetPath.getParent());
                copy(file, targetPath);
                return super.visitFile(file, attrs);
            }
        });
    }

    protected void deleteWatchedResources() throws IOException {
        deleteDirectory(root_etc);
        deleteDirectory(root_home);
        deleteIfExists(zipFile);
        deleteIfExists(root_configProperties);
    }

    @After
    public void deleteRootDirectory() throws IOException {
        deleteDirectory(root);
    }

    @Before
    public void setupDirectories() throws Exception {
        copyDirectories();
        validate();
    }
}
