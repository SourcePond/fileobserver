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
package ch.sourcepond.io.fileobserver.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EventObject;

import javax.annotation.Resource;

/**
 * Event to be fired when a resource tracks a change on its observed workspace
 * file. The path returned by {@link #getSource()} is always absolute.
 */
public class ResourceEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2307053276178570592L;

	/**
	 * Enumeration of of all possible types which a {@link ResourceEvent} can
	 * have.
	 *
	 */
	public static enum Type {
		/**
		 * Indicates, that the receiving {@link ResourceChangeListener} has just
		 * been added to the {@link Workspace}, i.e. the listener will receive
		 * events of this type for every file/directory in the watched workspace
		 * (if not filtered out, see {@link ResourceFilter}).
		 */
		LISTENER_ADDED,

		/**
		 * Indicates, that the receiving {@link ResourceChangeListener} has just
		 * been removed from the {@link Workspace}, i.e. no further events will
		 * be delivered to the listener until it is added again to the
		 * workspace. For instance, this event type is useful to indicate the
		 * removed listener to clear caches etc.
		 */
		LISTENER_REMOVED,

		/**
		 * Indicates, that the file associated with the resource has been
		 * created. This event can only happen in conjunction of previously
		 * fired event of type {@link #RESOURCE_DELETED}.
		 */
		RESOURCE_CREATED,

		/**
		 * Indicates, that the file associated with the resource has been
		 * modified. This does not necessarily mean that the content of the file
		 * has been changed; it could also be an fs-attribute change. Use
		 * {@link #CONTENT_CHANGED} if a content change should be processed.
		 */
		RESOURCE_MODIFIED,

		/**
		 * Indicates, that the content of the file associated with the resource
		 * has been modified. Use {@link #RESOURCE_MODIFIED} if an attribute
		 * change should be processed.
		 */
		CONTENT_CHANGED,

		/**
		 * Indicates, that the file associated with the resource has been
		 * deleted. After this event, {@link Resource#exists()} will return
		 * {@code false} and {@link Resource#open()} will throw an
		 * {@link IOException} until a subsequent event of type
		 * {@link #RESOURCE_CREATED} is fired.
		 */
		RESOURCE_DELETED;
	}

	private final Type type;
	private final Path relativePath;

	/**
	 * Creates a new instance of this class.
	 * 
	 * @param pAbsolutePath
	 *            Absolute path which causes this event, must not be
	 *            {@code null}
	 * @param pRelativePath
	 *            Relative path to workspace directory, must not be {@code null}
	 * @param pType
	 *            Type of this event, must not be {@code null}
	 * @throws IllegalArgumentException
	 *             Thrown, if either argument is {@code null}
	 */
	public ResourceEvent(final Path pAbsolutePath, final Path pRelativePath, final Type pType) {
		super(pAbsolutePath);
		if (pRelativePath == null) {
			throw new NullPointerException("Relative path cannot be null");
		}
		relativePath = pRelativePath;
		if (pType == null) {
			throw new IllegalArgumentException("Type cannot be null");
		}
		type = pType;
	}

	/**
	 * Returns the relative path to the workspace directory (see
	 * {@link WorkspaceFactory#create(java.util.concurrent.Executor, Path)}.
	 * 
	 * @return Relative path, never {@code null}
	 */
	public Path getRelativeSource() {
		return relativePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.EventObject#getSource()
	 */
	@Override
	public Path getSource() {
		return (Path) super.getSource();
	}

	/**
	 * Returns the concrete type of this event.
	 * 
	 * @return Type, never {@code null}
	 */
	public Type getType() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.EventObject#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + "[" + getType() + "]";
	}
}
