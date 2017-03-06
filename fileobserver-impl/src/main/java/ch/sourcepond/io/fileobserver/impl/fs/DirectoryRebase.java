package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 */
class DirectoryRebase {
    private final DirectoryFactory directoryFactory;
    private final WatchServiceRegistrar registrar;

    DirectoryRebase(final DirectoryFactory pDirectoryFactory, final WatchServiceRegistrar pRegistrar) {
        directoryFactory = pDirectoryFactory;
        registrar = pRegistrar;
    }

    /**
     * Collects the paths between the parent and the child path specified. The returned collection will <em>not</em>
     * contain the parent and child path specified, and, will be ordered from the base path to the farthest child.
     *
     * @param pParent The parent path, must not be {@code null}
     * @param pChild  The child path, must not be {@code null}
     * @return Collection of paths in between excluding the parent and child specified
     */
    private Collection<Path> pathsInBetweenOf(final Path pParent, final Path pChild) {
        final Deque<Path> hierarchy = new LinkedList<>();
        Path p = pChild.getParent();
        while (!p.equals(pParent)) {
            hierarchy.addFirst(p);
            p = p.getParent();
        }
        return hierarchy;
    }

    /**
     * Collects all existing root directories which are children of the new root directory specified specified.
     *
     * @param pNewRoot New root directory to match, must not be {@code null}
     * @return Collection of directories, never {@code null}.
     */
    private Collection<Directory> collectExistingRoots(final Directory pNewRoot, final Map<Path, Directory> pDirs) {
        final Path parentPath = pNewRoot.getPath();
        final Collection<Directory> pathsToRebase = new LinkedList<>();
        pDirs.entrySet().forEach(e -> {
            if (e.getKey().startsWith(parentPath) && e.getValue().isRoot()) {
                pathsToRebase.add(e.getValue());
            }
        });
        return pathsToRebase;
    }

    /**
     * Sets the directory specified on all directories whose path is a direct child of
     * {@link Directory#getPath()} of the base directory specified.
     *
     * @param pBaseDirectory Parent directory to set, must not be {@code null}
     */
    private void rebaseDirectSubDirectories(final Directory pBaseDirectory, final Map<Path, Directory> pDirs) {
        final Path base = pBaseDirectory.getPath();
        pDirs.forEach((k, v) -> {
            if (base.equals(k.getParent())) {
                v.rebase(pBaseDirectory);
            }
        });
    }

    void rebaseExistingRootDirectories(final Directory pNewRoot, final Map<Path, Directory> pDirs) throws IOException {
        // Iterate over already registered root directories which should be converted to
        // sub-directories (rebased).
        for (final Directory existingRoot : collectExistingRoots(pNewRoot, pDirs)) {

            // Create all missing directories between the new root and
            // the existing roots which shall be rebased.
            Directory parent = pNewRoot;
            for (final Path missingLevel : pathsInBetweenOf(pNewRoot.getPath(), existingRoot.getPath())) {
                parent = directoryFactory.newBranch(parent, registrar.register(missingLevel));
                pDirs.put(missingLevel, parent);
            }

            // Rebase the existing root-directory; after this operation it's
            // not a root directory anymore but a sub-directory of the root
            // directory specified.
            final Directory rebasedDirectory = existingRoot.rebase(parent);
            pDirs.replace(existingRoot.getPath(), rebasedDirectory);

            // This is important: we need to rebase also the direct children of the
            // former root directory otherwise they would reference an invalid parent!
            rebaseDirectSubDirectories(rebasedDirectory, pDirs);
        }

        // The new root directory is being added as very last
        pDirs.put(pNewRoot.getPath(), pNewRoot);
    }
}
