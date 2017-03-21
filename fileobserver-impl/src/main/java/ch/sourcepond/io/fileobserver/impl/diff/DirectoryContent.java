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
package ch.sourcepond.io.fileobserver.impl.diff;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 *
 */
class DirectoryContent extends SimpleFileVisitor<Path> {
    private final DefaultFileKeyFactory keyFactory;
    private final Set<FileKey> keys = new HashSet<>();
    private final Object directoryKey;
    private final Path previousDirectory;

    DirectoryContent(final DefaultFileKeyFactory pKeyFactory,
                     final Object pDirectoryKey,
                     final Path pPreviousDirectory) {
        keyFactory = pKeyFactory;
        directoryKey = pDirectoryKey;
        previousDirectory = pPreviousDirectory;
    }

    Set<FileKey> getKeys() {
        return keys;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        keys.add(keyFactory.newKey(directoryKey, previousDirectory.relativize(file)));
        return CONTINUE;
    }
}
