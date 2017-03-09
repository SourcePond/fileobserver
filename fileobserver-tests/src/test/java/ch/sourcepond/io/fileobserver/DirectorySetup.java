package ch.sourcepond.io.fileobserver;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
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
public class DirectorySetup implements TestRule {
    public static final Path R = getDefault().getPath(System.getProperty("java.io.tmpdir"), DirectorySetup.class.getName(), UUID.randomUUID().toString());
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

    private void copyDirectories() throws IOException {
        final Path source = getDefault().getPath(System.getProperty("user.dir"), "src", "test", "resources", "testdir");
        walkFileTree(source, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final Path relativePath = source.relativize(file);
                final Path targetPath = R.resolve(relativePath);
                createDirectories(targetPath.getParent());
                copy(file, targetPath);
                return super.visitFile(file, attrs);
            }
        });
    }

    private void deleteDirectories() throws IOException {
        walkFileTree(R, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                copyDirectories();
                validate();
                sleep(5000);
                try {
                    base.evaluate();
                } finally {
                    deleteDirectories();
                }
            }
        };
    }
}
