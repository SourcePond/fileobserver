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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Collects all files of the root-directory specified. Every file will be relatived against the root-directory.
 */
class FileCollector extends SimpleFileVisitor<Path> {
    private final Collection<Path> files = new LinkedList<>();
    private final Path rootDirectory;
    private final Consumer<Path> consumer;

    FileCollector(final Path pRootDirectory, final Consumer<Path> pConsumer) {
        rootDirectory = pRootDirectory;
        consumer = pConsumer;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        consumer.accept(rootDirectory.relativize(file));
        return CONTINUE;
    }
}
