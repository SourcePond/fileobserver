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
import java.util.EventObject;

/**
 * Event to be fired when a resource tracks a change on its observed workspace
 * file.
 */
public final class ResourceEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5819632600293975764L;

	/**
	 * Enumeration of of all possible types which a {@link ResourceEvent} can
	 * have.
	 *
	 */
	public static enum Type {
		/**
		 * Indicates, that the receiving {@link ResourceChangeListener} has just
		 * been added to the {@link Resource}, i.e. the listener will receive
		 * all subsequent change events of the resource.
		 */
		LISTENER_ADDED,

		/**
		 * Indicates, that the receiving {@link ResourceChangeListener} has just
		 * been removed from the {@link Resource}, i.e. no further events will
		 * be delivered to the listener until it is added again to the resource.
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
		 * has been changed (could also be an fs-attribute change).
		 */
		RESOURCE_MODIFIED,

		/**
		 * Indicates, that the file associated with the resource has been
		 * deleted. After this event, {@link Resource#exists()} will return
		 * {@code false} and {@link Resource#open()} will throw an
		 * {@link IOException} until a subsequent event of type
		 * {@link #RESOURCE_CREATED} is fired.
		 */
		RESOURCE_DELETED,

		/**
		 * Indicates, that the workspace associated with a {@link Resource} has
		 * been closed. In this case calling
		 * {@link Resource#addListener(ResourceChangeListener)} or
		 * {@link Resource#open()} will cause an exception to be thrown.
		 */
		WORKSPACE_CLOSED;
	}

	private final Type type;

	/**
	 * Creates a new instance of this class.
	 * 
	 * @param pSource
	 *            Resource which causes this event, must not be {@code null}
	 * @param pType
	 *            Type of this event, must not be {@code null}
	 * @throws IllegalArgumentException
	 *             Thrown, if either argument is {@code null}
	 */
	public ResourceEvent(final Resource pSource, final Type pType) {
		super(pSource);
		if (pType == null) {
			throw new IllegalArgumentException("type cannot be null");
		}
		type = pType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.EventObject#getSource()
	 */
	@Override
	public Resource getSource() {
		return (Resource) super.getSource();
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
