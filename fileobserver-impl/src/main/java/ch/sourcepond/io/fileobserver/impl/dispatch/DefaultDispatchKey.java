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
package ch.sourcepond.io.fileobserver.impl.dispatch;

import ch.sourcepond.io.fileobserver.api.DispatchKey;

import java.nio.file.Path;
import java.util.Objects;

import static java.lang.String.format;

/**
 *
 */
final class DefaultDispatchKey implements DispatchKey {
    private final Object directoryKey;
    private final Path relativePath;

    public DefaultDispatchKey(final Object pDirectoryKey, final Path pRelativePath) {
        directoryKey = pDirectoryKey;
        relativePath = pRelativePath;
    }

    @Override
    public Object getDirectoryKey() {
        return directoryKey;
    }

    @Override
    public Path getRelativePath() {
        return relativePath;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultDispatchKey other = (DefaultDispatchKey) o;
        return Objects.equals(directoryKey, other.directoryKey) &&
                Objects.equals(relativePath, other.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directoryKey, relativePath);
    }

    @Override
    public String toString() {
        return format("[%s:%s]", directoryKey, relativePath);
    }
}
