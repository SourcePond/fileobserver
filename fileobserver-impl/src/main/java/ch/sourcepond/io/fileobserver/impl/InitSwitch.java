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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 */
class InitSwitch<T> {
    private final Consumer<T> consumer;
    private Set<T> objects = new HashSet<>();

    InitSwitch(final Consumer<T> pConsumer) {
        consumer = pConsumer;
    }

    void init() {
        final Set<T> objs;
        synchronized (this) {
            objs = objects;
            if (objects == null) {
                throw new IllegalStateException("Init already done");
            }
            objects = null;
        }
        objs.forEach(consumer::accept);
    }

    void add(final T pObj) {
        synchronized (this) {
            if (objects != null) {
                objects.add(pObj);
                return;
            }
        }
        consumer.accept(pObj);
    }
}