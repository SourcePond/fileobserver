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

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.Update;
import ch.sourcepond.io.checksum.api.UpdateObserver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Thread.currentThread;

/**
 *
 */
class UninitializedResource implements InvocationHandler, UpdateObserver {
    private final Path file;
    private final ConcurrentMap<Path, Resource> resources;
    private boolean initialCalculationPending = true;
    private Resource resource;

    UninitializedResource(final Path pFile,
                          final ConcurrentMap<Path, Resource> pResources) {
        file = pFile;
        resources = pResources;
    }

    synchronized void setResource(final Resource pResource) {
        resource = pResource;
        notifyAll();
    }

    @Override
    public synchronized Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        try {
            while (initialCalculationPending) {
                wait();
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
        return method.invoke(resource, args);
    }

    @Override
    public synchronized void done(final Update pUpdate) {
        try {
            while (resource == null) {
                wait();
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
        resources.replace(file, resource);
        initialCalculationPending = false;
        notifyAll();
    }
}
