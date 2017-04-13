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
package ch.sourcepond.io.fileobserver.impl.filekey;

import ch.sourcepond.io.fileobserver.api.FileKey;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 *
 */
final class DefaultFileKey implements FileKey {
    private final Object directoryKey;
    private final Path relativePath;

    public DefaultFileKey(final Object pDirectoryKey, final Path pRelativePath) {
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
        final DefaultFileKey other = (DefaultFileKey) o;
        return Objects.equals(directoryKey, other.directoryKey) &&
                Objects.equals(relativePath, other.relativePath);
    }

    @Override
    public boolean isParentKeyOf(final FileKey pOther) {
        requireNonNull(pOther, "Other key is null");
        return getDirectoryKey().equals(pOther.getDirectoryKey()) && pOther.getRelativePath().startsWith(getRelativePath());
    }

    @Override
    public boolean isSubKeyOf(final FileKey pOther) {
        requireNonNull(pOther, "Other key is null");
        return getDirectoryKey().equals(pOther.getDirectoryKey()) && getRelativePath().startsWith(pOther.getRelativePath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(directoryKey, relativePath);
    }

    @Override
    public String toString() {
        return format("[%s:%s]", directoryKey, relativePath);
    }

    @Override
    public Collection<FileKey> findSubKeys(final Collection<FileKey> pKeys) {
        final Collection<FileKey> subKeys = new LinkedList<>();
        pKeys.forEach(k -> {
            if (k.isSubKeyOf(this)) {
                subKeys.add(k);
            }
        });
        return subKeys;
    }

    @Override
    public void removeSubKeys(final Collection<FileKey> pKeys) {
        pKeys.removeIf(k -> k.isSubKeyOf(this));
    }
}
