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
package ch.sourcepond.io.fileobserver.api;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 *
 */
public interface WatchedDirectory {

    Enum<?> getKey();

    Path getDirectory();

    static WatchedDirectory create(final Enum<?> pKey, final Path pDirectory) {
        requireNonNull(pKey, "Key is null");
        requireNonNull(pDirectory, "Directory is null");

        return new WatchedDirectory() {
            @Override
            public Enum<?> getKey() {
                return null;
            }

            @Override
            public Path getDirectory() {
                return null;
            }
        };
    }
}
