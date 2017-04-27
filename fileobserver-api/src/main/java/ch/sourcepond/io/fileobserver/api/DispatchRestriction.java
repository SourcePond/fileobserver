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

import java.nio.file.PathMatcher;

/**
 * A {@link PathChangeListener} is able to specify which file changes should be observed. To do the necessary restrict,
 * an object implementing this interface is passed to the {@link PathChangeListener#restrict(DispatchRestriction)} method
 * of a listener when it is being registered. Note:
 * <ul>
 * <li>If neither {@link #accept(Object...)} nor {@link #acceptAll()} has ever been called during the listener registration, the
 * listener will not receive any events at all!</li>
 * <li>If {@link #accept(Object...)} has been called, the listener will only accept events when their
 * {@link PathChangeEvent#getKey()} method returns an accepted value.</li>
 * <li>When none of the {@code add*} methods has been called during the listener registration, any dispatch event or
 * file/directory discard will be delivered, if the directory-key is accepted.</li>
 * <li>When one of the {@code add*} methods has been called at least once, a dispatch event or file/directory
 * discard will only be delivered, if {@link DispatchKey#getRelativePath()} matches one of the compound
 * path matchers, and, if the directory-key is accepted.</li>
 * </ul>
 */
public interface DispatchRestriction extends SimpleDispatchRestriction {

    /**
     * <p>Specifies, which directory-keys should be accepted by the {@link PathChangeListener}. This means, that
     * dispatch events or a file/directory discards are pre-filtered before any added path-matcher applies (if
     * any, see {@link #addPathMatcher(String)} and {@link #addPathMatcher(PathMatcher)}).</p>
     *
     * @param pDirectoryKeys Directory-keys which are accepted by the listener, must not be {@code null}
     * @throws NullPointerException Thrown, if a key is {@code null}.
     * @throws IllegalArgumentException Thrown, if no keys are specified, i.e. the vararg is empty.
     * @throws IllegalStateException Thrown, if either this method or {@link #acceptAll()} has already been called.
     */
    SimpleDispatchRestriction accept(Object... pDirectoryKeys);

    /**
     * <p>Specifies, that all directory-keys should be accepted by the {@link PathChangeListener}. This means, that
     * any dispatch event or a file/directory discard is directly matched against the registered compound path-matcher
     * (if any, see {@link #addPathMatcher(String)} and {@link #addPathMatcher(PathMatcher)}).</p>
     *
     * @throws IllegalStateException Thrown, if either this method or {@link #accept(Object...)} has already been called.
     */
    SimpleDispatchRestriction acceptAll();
}
