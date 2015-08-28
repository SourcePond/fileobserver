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
package ch.sourcepond.utils.fileobserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;

/**
 * <p>
 * A resource allows to observe a specific file associated with a workspace
 * instance. A resource can be retrieved through one of the {@code watch}
 * -methods of {@link Workspace}.
 * </p>
 * 
 * <p>
 * When a resource is created, the content from the original location will be
 * copied into the workspace. The resource object points to that copy and will
 * track any change made on it (see {@link ResourceEvent.Type} for supported
 * event types). Note: a resource does <em>not</em> observe the original content
 * for any changes.
 * </p>
 * 
 * <p>
 * Resources are <em>thread-safe</em>.
 * </p>
 */
public interface Resource {

	/**
	 * Adds the {@link ResourceChangeListener} specified to this resource. The
	 * listener will receive a {@link ResourceEvent} of type
	 * {@link Type#LISTENER_ADDED} immediately after it has been successfully
	 * registered with this resource. If the listener specified is already
	 * registered, calling this method has no effect.
	 * 
	 * @param pListener
	 *            Listener instance to be added, must not be {@code null}
	 * @throws NullPointerException
	 *             Thrown, if the listener specified is {@code null}
	 * @throws IllegalStateException
	 *             Thrown, if the associated workspace of this resource has been
	 *             closed
	 */
	void addListener(ResourceChangeListener pListener);

	/**
	 * Removes the {@link ResourceChangeListener} specified from this resource
	 * object. The listener will receive a {@link ResourceEvent} of type
	 * {@link Type#LISTENER_REMOVED} immediately after it has been successfully
	 * unregistered. If the listener specified is not known by this resource or
	 * is {@code null}, calling this method has no effect. The same happens, if
	 * the associated workspace of this resource has been closed.
	 * 
	 * @param pListener
	 *            Listener instance to be removed
	 */
	void removeListener(ResourceChangeListener pListener);

	/**
	 * Returns the {@link URL} object which points to the origin content, i.e.
	 * from where the resource has been copied into the workspace.
	 * 
	 * @return Origin content URL, never {@code null}
	 */
	URL getOriginContent();

	/**
	 * Indicates, whether the observed file of this resource does exist. If the
	 * file has been deleted (see {@link Type#RESOURCE_DELETED}, this method
	 * returns {@code false}. If the file has been re-created (see
	 * {@link Type#RESOURCE_CREATED}, this method returns {@code true}. Note: if
	 * the associated workspace of this resource has been closed, calling this
	 * method returns {@code false}, even when the file still physically exists.
	 * 
	 * @return {@code true} if the observed file exists and the workspace is not
	 *         closed, {@code false} otherwise.
	 */
	boolean exists();

	/**
	 * Opens the observed file of this resource for reading. If the file does
	 * not exist in the workspace (because it has been deleted, see
	 * {@link Type#RESOURCE_DELETED}), or, the workspace has been closed, an
	 * {@link IOException} will be caused to be thrown.
	 * 
	 * @return Input stream for reading, never {@code null}.
	 * @throws IOException
	 *             Thrown, if the file observed by this resource could not be
	 *             opened for reading.
	 */
	InputStream open() throws IOException;
}
