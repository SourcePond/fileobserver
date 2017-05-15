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

import ch.sourcepond.io.checksum.api.Algorithm;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourcesFactory;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 *
 */
public class UninitializedResourceFactory {
    private final ResourcesFactory resourcesFactory;

    public UninitializedResourceFactory(final ResourcesFactory pResourcesFactory) {
        resourcesFactory = pResourcesFactory;
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Creates a new checksum {@link Resource} with the algorithm and file specified.
     *
     * @param pAlgorithm Algorithm, must not be {@code null}
     * @param pFile      File on which checksums shall be tracked, must not be {@code null}
     * @return New resource instance, never {@code null}
     */
    Resource newResource(final Algorithm pAlgorithm, final Path pFile, final ConcurrentMap<Path, Resource> pResources) {
        final UninitializedResource proxy = new UninitializedResource(pFile, pResources);
        proxy.setResource(resourcesFactory.create(pAlgorithm, pFile, proxy));
        return (Resource) newProxyInstance(getClass().getClassLoader(), new Class<?>[] {
                Resource.class
        }, proxy);
    }
}
