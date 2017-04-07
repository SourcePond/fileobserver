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

/**
 *
 */
public interface KeyDeliveryHook {

    default void before(final FileKey pKey) {
        // noop by default
    }

    default void after(final FileKey pKey) {
        // noop by default
    }

    default void beforeModify(final FileKey pKey, Path pFile) {
        before(pKey);
    }

    default void beforeDiscard(final FileKey pKey) {
        before(pKey);
    }

    default void afterModify(final FileKey pKey, Path pFile) {
        after(pKey);
    }

    default void afterDiscard(final FileKey pKey) {
        after(pKey);
    }
}
