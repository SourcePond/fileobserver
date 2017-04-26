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
 * A dispatch event encapsulates all information about a file modification. Additionally it provides the
 * ability to re-dispatch this event (see {@link #replay()}).
 */
public interface DispatchEvent {

    /**
     * Returns the key which uniquely identifies the file returned by {@link #getFile()}. Always use this key
     * for any caching (see {@link DispatchKey} for further information).
     *
     * @return Unique key of the file modified (see {@link #getFile()}).
     */
    DispatchKey getKey();

    /**
     * <p>Returns the absolute path of the file (never a directory) which has been modified. Clients may use this for
     * reading data. Note: do <em>never</em> use the file returned by this method for any caching! For that
     * purpose, always use the key returned by {@link #getKey()} (see {@link DispatchKey} for further information).</p>
     *
     * @return Absolute file, never {@code null}
     */
    Path getFile();

    /**
     * Returns how many times this event has already been replayed i.e. rescheduled to be delivered again to interested
     * {@link PathChangeListener} instances.
     *
     * @return Positive integer, or 0 if this event never has been replayed.
     */
    int getNumReplays();

    /**
     * <p>Enqueue this event again for later delivery. This is useful if the file returned by {@link #getFile()}
     * cannot be processed right now but sometime in the future. For instance, this could be the case when a file is
     * delivered during a startup procedure but not all necessary services are up to handle this event properly. If so,
     * a client can stop processing the file and schedule its dispatch to a later point in time. It's possible to
     * query how may times this event has been rescheduled through {@link #getNumReplays()}.</p>
     *
     * <p>Attention: it's possible to produce an infinite loop if the {@link PathChangeListener#modified(DispatchKey, Path)}
     * implementation does always call this method for some reason! So implementors should take care that this
     * method is guarded by an appropriate condition.</p>
     */
    void replay();
}
