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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;
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
 *     + Root folder (R)
 *        + etc (E)
 *        |   + network (E1)
 *        |   |    + network.conf (E11)
 *        |   |    + dhcp.conf (E12)
 *        |   + man.conf (E2)
 *        + home (H)
 *        |   + jeff (H1)
 *        |   |   + document.txt (H11)
 *        |   |   + letter.xml (H12)
 *        |   + index.idx (H2)
 *        + config.properties (C)
 * </pre>
 */
class DirectorySetup implements TestRule {
    static final String ZIP_NAME = "hotdeploy.zip";
    private static final Path SOURCE = getDefault().getPath(getProperty("user.dir"), "src", "test", "resources", "testdir");
    public static final Path R = getDefault().getPath(getProperty("java.io.tmpdir"), DirectorySetup.class.getName(), UUID.randomUUID().toString());
    public static final Path E = R.resolve("etc");
    public static final Path E1 = E.resolve("network");
    public static final Path E11 = E1.resolve("network.conf");
    public static final Path E12 = E1.resolve("dhcp.conf");
    public static final Path E2 = E.resolve("man.conf");
    public static final Path H = R.resolve("home");
    public static final Path H1 = H.resolve("jeff");
    public static final Path H11 = H1.resolve("document.txt");
    public static final Path H12 = H1.resolve("letter.xml");
    public static final Path H2 = H.resolve("index.idx");
    public static final Path C = R.resolve("config.properties");
    private static final Path ZIP_FILE = R.resolve(ZIP_NAME);

    private void validate() {
        assertTrue(R.toString(), isDirectory(R));
        assertTrue(E.toString(), isDirectory(E));
        assertTrue(E1.toString(), isDirectory(E1));
        assertTrue(E11.toString(), isRegularFile(E11));
        assertTrue(E12.toString(), isRegularFile(E12));
        assertTrue(E2.toString(), isRegularFile(E2));
        assertTrue(H.toString(), isDirectory(H));
        assertTrue(H1.toString(), isDirectory(H1));
        assertTrue(H11.toString(), isRegularFile(H11));
        assertTrue(H12.toString(), isRegularFile(H12));
        assertTrue(H2.toString(), isRegularFile(H2));
        assertTrue(C.toString(), isRegularFile(C));
    }

    private void unzipEntry(final ZipEntry pEntry, final InputStream pIn) throws IOException {
        if (!pEntry.isDirectory()) {
            final Path target = R.resolve(pEntry.getName());
            createDirectories(target.getParent());
            copy(pIn, target);
        }
    }

    void unzip() throws IOException {
        try (final ZipInputStream in = new ZipInputStream(newInputStream(R.resolve(ZIP_NAME)))) {
            ZipEntry next = in.getNextEntry();
            while (next != null) {
                unzipEntry(next, in);
                next = in.getNextEntry();
            }
        }
    }

    void createZip() throws IOException {
        try (final ZipOutputStream out = new ZipOutputStream(newOutputStream(ZIP_FILE))) {
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
                final Path targetPath = R.resolve(relativePath);
                createDirectories(targetPath.getParent());
                copy(file, targetPath);
                return super.visitFile(file, attrs);
            }
        });
    }

    void deleteDirectories() throws IOException {
        deleteDirectory(E);
        deleteDirectory(H);
        deleteIfExists(ZIP_FILE);
        deleteIfExists(C);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                copyDirectories();
                validate();
                try {
                    base.evaluate();
                } finally {
                    deleteDirectories();
                }
            }
        };
    }
}
