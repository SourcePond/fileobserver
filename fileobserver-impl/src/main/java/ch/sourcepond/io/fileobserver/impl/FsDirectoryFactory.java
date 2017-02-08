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

import ch.sourcepond.io.checksum.api.ResourcesFactory;

import java.nio.file.WatchKey;

/**
 * Created by rolandhauser on 08.02.17.
 */
class FsDirectoryFactory {
    private final ResourcesFactory resourcesFactory;

    FsDirectoryFactory(final ResourcesFactory pResourcesFactory) {
        resourcesFactory = pResourcesFactory;
    }

    FsDirectory newDirectory(final FsDirectory pParent, final WatchKey pKey) {
        return new FsDirectory(resourcesFactory, pParent, pKey);
    }
}