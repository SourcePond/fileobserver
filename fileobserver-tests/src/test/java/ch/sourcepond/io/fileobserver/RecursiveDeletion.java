package ch.sourcepond.io.fileobserver;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.walkFileTree;

/**
 * Created by rolandhauser on 09.03.17.
 */
class RecursiveDeletion extends SimpleFileVisitor<Path> {

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

    public static void deleteDirectory(final Path pToBeDeleted) throws IOException{
        walkFileTree(pToBeDeleted, new RecursiveDeletion());
    }
}
