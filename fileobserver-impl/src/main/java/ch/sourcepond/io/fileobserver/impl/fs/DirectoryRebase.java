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
package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 *
 */
class DirectoryRebase {
    private final DirectoryFactory directoryFactory;
    private final WatchServiceWrapper registrar;
    private final Map<Path, Directory> dirs;

    DirectoryRebase(final DirectoryFactory pDirectoryFactory, final WatchServiceWrapper pRegistrar, final Map<Path, Directory> pDirs) {
        directoryFactory = pDirectoryFactory;
        registrar = pRegistrar;
        dirs = pDirs;
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
    private Collection<Directory> collectExistingRoots(final Directory pNewRoot) {
        final Path parentPath = pNewRoot.getPath();
        final Collection<Directory> pathsToRebase = new LinkedList<>();
        dirs.entrySet().forEach(e -> {
            final Path childPath = e.getKey();
            if (childPath.startsWith(parentPath) && e.getValue().isRoot()) {
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
    private void rebaseDirectSubDirectories(final Directory pBaseDirectory) {
        final Path base = pBaseDirectory.getPath();
        dirs.forEach((k, v) -> {
            if (base.equals(k.getParent())) {
                v.rebase(pBaseDirectory);
            }
        });
    }

    void rebaseExistingRootDirectories(final Directory pNewRoot) throws IOException {
        // Iterate over already registered root directories which should be converted to
        // sub-directories (rebased).
        for (final Directory existingRoot : collectExistingRoots(pNewRoot)) {

            // Create all missing directories between the new root and
            // the existing roots which shall be rebased.
            Directory parent = pNewRoot;
            for (final Path missingLevel : pathsInBetweenOf(pNewRoot.getPath(), existingRoot.getPath())) {
                parent = directoryFactory.newBranch(parent, registrar.register(missingLevel));
                dirs.put(missingLevel, parent);
            }

            // Rebase the existing root-directory; after this operation it's
            // not a root directory anymore but a sub-directory of the root
            // directory specified.
            final Directory rebasedDirectory = existingRoot.rebase(parent);
            dirs.replace(existingRoot.getPath(), rebasedDirectory);

            // This is important: we need to rebase also the direct children of the
            // former root directory otherwise they would reference an invalid parent!
            rebaseDirectSubDirectories(rebasedDirectory);
        }

        // The new root directory is being added as very last
        dirs.put(pNewRoot.getPath(), pNewRoot);
    }

    /**
     * Recursively traverses the registered directories. Directories which were not roots sometime in the past will
     * be cancelled and removed. If a directory was a root (known by {@link Directory#hasKeys()}), then it will be
     * added to the list specified. This list contains sub-directories which need to be converted back into
     * root directories.
     *
     * @param pDiscardedParent Parent directory which was discarded, never {@code null}
     * @param pToBeConverted   List of sub-directories which need to be converted into root-directories, never {@code null}
     */
    private void cancelDiscardedDirectories(final Directory pDiscardedParent,
                                            final Collection<Directory> pToBeConverted) {
        final Collection<Directory> toBeDiscarded = new LinkedList<>();
        dirs.values().removeIf(dir -> {
            if (pDiscardedParent.isDirectParentOf(dir)) {
                if (dir.hasKeys()) {
                    pToBeConverted.add(dir);
                } else {
                    toBeDiscarded.add(dir);
                    dir.cancelKey();
                    return true;
                }
            }
            return false;
        });
        toBeDiscarded.forEach(dir -> cancelDiscardedDirectories(dir, pToBeConverted));
    }

    /**
     * Cancels the watch-key of the discarded directory specified. Additionally, cancels and removes any sub-directory
     * which was not a root itself sometime in the past. Any sub-directory which was a root directory in the past
     * will be converted back to a root-directory.
     *
     * @param pDiscardedParent Discarded directory, never {@code null}
     */
    void cancelAndRebaseDiscardedDirectory(final Directory pDiscardedParent) {
        pDiscardedParent.cancelKey();
        dirs.remove(pDiscardedParent.getPath());

        final List<Directory> toBeConvertedIntoRoot = new LinkedList<>();
        cancelDiscardedDirectories(pDiscardedParent, toBeConvertedIntoRoot);
        toBeConvertedIntoRoot.forEach(d -> {
            final Directory root = d.toRootDirectory();
            rebaseDirectSubDirectories(root);
            dirs.replace(d.getPath(), root);
        });
    }
}
