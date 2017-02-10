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

import ch.sourcepond.commons.smartswitch.api.SmartSwitchFactory;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.slf4j.Logger;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class DefaultObserverHandlerFactory implements Closeable {
    private static final Logger LOG = getLogger(DefaultObserverHandlerFactory.class);
    private final ExecutorService observerExecutor;

    DefaultObserverHandlerFactory(final SmartSwitchFactory pSmartSwitch) {
        observerExecutor = pSmartSwitch.whenService(ExecutorService.class).
                withFilter("(sourcepond.io.fileobserver.observerexecutor=*)").
                isUnavailableThenUse(() -> newCachedThreadPool()).
                insteadAndExecuteWhenAvailable(ExecutorService::shutdown);
    }

    @Override
    public void close() {
        try {
            observerExecutor.shutdown();
        } catch (final SecurityException e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    ObserverHandler newHander(final FileObserver pObserver) {
        return new DefaultObserverHandler(observerExecutor, pObserver);
    }
}
