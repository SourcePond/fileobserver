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
package ch.sourcepond.io.fileobserver.impl.directory;

import java.nio.file.WatchKey;

import static org.mockito.Mockito.mock;

/**
 *
 */
public abstract class DirectoryBaseTest {
    static final Object DIRECTORY_KEY = new Object();
    final DirectoryFactory factory = mock(DirectoryFactory.class);
    final WatchKey watchKey = mock(WatchKey.class);
}
