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

import ch.sourcepond.commons.smartswitch.api.SmartSwitchFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newWorkStealingPool;

/**
 *
 */
public class ExecutorServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceFactory.class);
    private final ExecutorService observerExecutor;
    private final ExecutorService directoryWalkerExecutor;

    ExecutorServiceFactory(final SmartSwitchFactory pSmartSwitchFactory) {
        // TODO: Make the constant parallelism paramter '5' configurable
        observerExecutor = pSmartSwitchFactory.whenService(ExecutorService.class).
                withFilter("(sourcepond.io.fileobserver.observerexecutor=*)").
                isUnavailableThenUse(() -> newWorkStealingPool(5)).insteadAndExecuteWhenAvailable(ExecutorService::shutdown);
        directoryWalkerExecutor = pSmartSwitchFactory.whenService(ExecutorService.class).
                withFilter("(sourcepond.io.fileobserver.directorywalkerexecutor=*)").
                isUnavailableThenUse(() -> observerExecutor).instead();
    }

    public ExecutorService getDirectoryWalkerExecutor() {
        return directoryWalkerExecutor;
    }

    public ExecutorService getObserverExecutor() {
        return observerExecutor;
    }
}
