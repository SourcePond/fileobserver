package ch.sourcepond.io.fileobserver.impl.directory;


import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by rolandhauser on 01.03.17.
 */
public class MemoryUsageTest extends SimpleFileVisitor<Path> {
    private final ConcurrentMap<Path, MessageDigest> map = new ConcurrentHashMap<>();
    private volatile int files;

    @Test
    public void showMemoryConsumption() throws Exception {
        System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " kb");
        System.gc();
        System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " kb");
        Files.walkFileTree(FileSystems.getDefault().getPath("/Users", "rolandhauser"), this);
        System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " kb");
        System.gc();
        System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " kb");
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        byte[] buffer = new byte[4096];
        try (final InputStream in = new BufferedInputStream(Files.newInputStream(file), 4096)) {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            digest.digest();
            map.put(file, digest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (files++ > 1000) {
            return FileVisitResult.TERMINATE;
        }
        return super.visitFile(file, attrs);
    }
}
