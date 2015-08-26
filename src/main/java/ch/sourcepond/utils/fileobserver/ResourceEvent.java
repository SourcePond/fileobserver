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

import java.util.EventObject;

/**
 *
 */
public final class ResourceEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6219486062224010008L;

	/**
	 * 
	 *
	 */
	public static enum Type {
		LISTENER_ADDED, LISTENER_REMOVED, RESOURCE_CREATED, RESOURCE_MODIFIED, RESOURCE_DELETED
	}

	private final Type type;

	/**
	 * 
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
	 * @return
	 */
	public Type getType() {
		return type;
	}
}
