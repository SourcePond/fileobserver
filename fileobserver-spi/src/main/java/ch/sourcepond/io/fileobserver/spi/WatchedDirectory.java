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
package ch.sourcepond.io.fileobserver.spi;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This interface represents a watched-directory. The fileobserver implementation
 * uses the whiteboard pattern to register {@link WatchedDirectory} instances.
 */
public interface WatchedDirectory extends Blacklist {

    void addBlacklistPattern(String pPattern);

    /**
     * Returns the unique key of this watched directory instance. The key remains
     * the same over the whole lifetime of this object.
     *
     * @return Watched-directory key, never {@code null}
     */
    Object getKey();

    /**
     * Registers the observer specified with this object. If the observer
     * has already been registered nothing will happen.
     *
     * @param pObserver Observer to add, must not be {@code null}
     * @throws NullPointerException Thrown, if the observer specified is {@code null}
     */
    void addObserver(RelocationObserver pObserver);

    /**
     * Removes the observer specified from this object. If the observer is {@code null}
     * or not registered nothing will happen.
     *
     * @param pObserver Observer to be removed.
     */
    void removeObserver(RelocationObserver pObserver);

    /**
     * Returns the currently watched-directory. This can be changed through
     * {@link #relocate(Path)}.
     *
     * @return Currently watched directory, never {@code null}.
     */
    Path getDirectory();

    /**
     * Sets a new destination directory if and only if the directory specified
     * is not equal to the current location (see {@link #getDirectory()}). If a new location
     * has been set, all registered {@link RelocationObserver} instances will informed
     * (see {@link #addObserver(RelocationObserver)}).
     *
     * @param pDirectory Directory which should be watched from now on; must not be {@code null}.
     * @throws IOException              Thrown, if the relocation could not be performed for some reason.
     * @throws NullPointerException     Thrown, if the directory specified is {@code null}.
     * @throws IllegalArgumentException Thrown, if the path specified is not a directory.
     */
    void relocate(Path pDirectory) throws IOException;

    /**
     * <p>Creates a new {@link WatchedDirectory} instance. The directory specified can later
     * be changed through {@link #relocate(Path)} if necessary.</p>
     * <p>
     * <p>Note: the key specified should not be changed during the lifetime
     * of the returned {@link WatchedDirectory}, otherwise unpredictable behavior
     * is very likely to occur. To avoid this, use an immutable object like an {@link Enum} or a {@link String}.
     * Furthermore, the key must be unique i.e. a key cannot be used for more than one {@link WatchedDirectory}
     * instance.</p>
     *
     * @param pKey       Key of the watched-directory, must not be {@code null}.
     * @param pDirectory Initial directory to be watched, must not be {@code null}.
     * @return New {@link WatchedDirectory} instance, never {@code null}
     * @throws NullPointerException     Thrown, if either to key or the path specified is {@code null}.
     * @throws IllegalArgumentException Thrown, if the path specified is not a directory.
     */
    static WatchedDirectory create(final Object pKey, final Path pDirectory) {
        return new DefaultWatchedDirectory(pKey, pDirectory);
    }
}
