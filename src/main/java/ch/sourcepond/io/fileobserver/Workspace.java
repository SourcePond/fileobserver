/*Copyright (C) 2015 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fileobserver;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

import ch.sourcepond.io.fileobserver.ResourceEvent.Type;

/**
 * <p>
 * Container to watch for changes on the directory specified during construction
 * of this object, see {@link WorkspaceFactory} for further information.
 * </p>
 * 
 * <p>
 * Implementations of this interface must be <em>thread-safe</em>.
 * </p>
 *
 */
public interface Workspace extends Closeable {

	/**
	 * <p>
	 * Adds the {@link ResourceChangeListener} specified to this workspace. The
	 * workspace will generate a {@link ResourceEvent} for every existing file
	 * including those located in sub-directories. If an event matches the
	 * listeners {@link ResourceFilter}, it will be dispatched to the newly
	 * added listener. If the listener specified is already registered, calling
	 * this method has no effect.
	 * </p>
	 * 
	 * <p>
	 * This method is a shorthand for
	 * {@link #addListener(ResourceChangeListener, ResourceFilter)} with
	 * {@link ResourceFilter#DISPATCH_ALL} as filter.
	 * </p>
	 * 
	 * @param pListener
	 *            Listener instance to be added, must not be {@code null}
	 * @throws NullPointerException
	 *             Thrown, if the listener specified is {@code null}
	 * @throws IllegalStateException
	 *             Thrown, if the this workspace has been closed.
	 */
	void addListener(ResourceChangeListener pListener);

	/**
	 * <p>
	 * Adds the {@link ResourceChangeListener} specified to this workspace. The
	 * workspace will generate a {@link ResourceEvent} for every existing file
	 * including those located in sub-directories. If an event matches the
	 * listeners {@link ResourceFilter}, it will be dispatched to the newly
	 * added listener. If the listener specified is already registered, calling
	 * this method has no effect.
	 * </p>
	 * 
	 * <p>
	 * A {@link ResourceEvent} will only be dispatched to the listener, if it
	 * matches the filter specified (see
	 * {@link ResourceFilter#isDispatched(ResourceEvent)}).
	 * </p>
	 * 
	 * @param pListener
	 *            Listener instance to be added, must not be {@code null}
	 * @param pFilter
	 *            Filter to decide if the listener specified should receive an
	 *            event when a specific path has been changed. Must not be
	 *            {@code null}.
	 * @throws NullPointerException
	 *             Thrown, if either argument is {@code null}.
	 * @throws IllegalStateException
	 *             Thrown, if the this workspace has been closed.
	 */
	void addListener(ResourceChangeListener pListener, ResourceFilter pFilter);

	/**
	 * Removes the {@link ResourceChangeListener} specified from this resource
	 * object. The listener will receive a {@link ResourceEvent} of type
	 * {@link Type#LISTENER_REMOVED} immediately after it has been successfully
	 * unregistered. If the listener specified is not known by this resource or
	 * is {@code null}, calling this method has no effect. The same happens, if
	 * this workspace has been closed.
	 * 
	 * @param pListener
	 *            Listener instance to be removed
	 */
	void removeListener(ResourceChangeListener pListener);

	/**
	 * <p>
	 * Copies the content from the given input-stream to the path specified. The
	 * path specified is relative to the workspace path (see
	 * {@link WorkspaceFactory#create(java.util.concurrent.ExecutorService, Path)}
	 * .
	 * </p>
	 * 
	 * <p>
	 * This method is a shorthand for {@code copy(InputStream, true, String...)}
	 * . The input-stream specified will be closed before this method returns a
	 * result or throws an exception.
	 * </p>
	 * 
	 * @param pOriginContent
	 *            Input-stream from where to load the content of the newly
	 *            created and observed resource; must not be {@code null}
	 * @param pPath
	 *            Path where to store the content, actually points to the
	 *            observed resource, must not be empty.
	 * @throws NullPointerException
	 *             Thrown, if the {@link URL} specified is {@code null}.
	 * @throws IllegalArgumentException
	 *             Thrown, if the path specified is empty contains blank
	 *             elements.
	 * @throws IOException
	 *             Thrown, if the content could not be copied for some reason.
	 */
	void copy(InputStream pOriginContent, String... pPath) throws FileAlreadyExistsException, IOException;

	/**
	 * <p>
	 * Copies the content from the given input-stream to the path specified. The
	 * path specified is relative to the workspace path (see
	 * {@link WorkspaceFactory#create(java.util.concurrent.ExecutorService, Path)}
	 * .
	 * </p>
	 * 
	 * <p>
	 * If a file with the path specified already exists, it will be overwritten
	 * when {@code pReplaceExisting} is {@code true}. If not, a
	 * {@link FileAlreadyExistsException} will be caused to be thrown. The
	 * input-stream specified will be closed in any case before this method
	 * returns a result or throws an exception.
	 * </p>
	 * 
	 * @param pOriginContent
	 *            Input-stream from where to load the content of the newly
	 *            created and observed resource; must not be {@code null}
	 * @param pReplaceExisting
	 *            Determines, whether an existing file shall be overwritten (
	 *            {@code true}) or a {@link FileAlreadyExistsException}
	 *            exception should be caused to be thrown ({@code false}).
	 * @param pPath
	 *            Path where to store the content, actually points to the
	 *            observed resource, must not be empty.
	 * @throws NullPointerException
	 *             Thrown, if the {@link URL} specified is {@code null}.
	 * @throws IllegalArgumentException
	 *             Thrown, if the path specified is empty contains blank
	 *             elements.
	 * @throws FileAlreadyExistsException
	 *             Thrown, if the target file already exists and
	 *             {@code pReplaceExisting} is {@code false}.
	 * @throws IOException
	 *             Thrown, if the content could not be copied for some reason.
	 */
	void copy(InputStream pOriginContent, boolean pReplaceExisting, String... pPath)
			throws FileAlreadyExistsException, IOException;
}
