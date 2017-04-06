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
package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.impl.filekey.KeyDeliveryConsumer;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 *
 */
class DispatcherTaskFactory {

    /**
     *
     * @param pObserverExecutor
     * @param pHooks
     * @param pObservers
     * @param pKey
     * @param pFireEventConsumer
     * @param pBeforeConsumer
     * @param pAfterConsumer
     * @param pParentKeys
     * @return
     */
    Runnable createTask(ExecutorService pObserverExecutor,
                        Collection<KeyDeliveryHook> pHooks,
                        Collection<FileObserver> pObservers,
                        FileKey pKey,
                        Consumer<FileObserver> pFireEventConsumer,
                        KeyDeliveryConsumer pBeforeConsumer,
                        KeyDeliveryConsumer pAfterConsumer,
                        Collection<FileKey> pParentKeys) {
        return new DispatcherTask(pObserverExecutor,
                pHooks,
                pObservers,
                pKey,
                pFireEventConsumer,
                pBeforeConsumer,
                pAfterConsumer,
                pParentKeys);
    }
}
