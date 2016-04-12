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

import static ch.sourcepond.io.fileobserver.api.ResourceFilter.FILES_ONLY;

import ch.sourcepond.io.fileobserver.api.ResourceEvent.Type;

/**
 * A listener to receive notifications about changes on a watched resource.
 *
 */
public interface ResourceChangeListener {

	String PATH_ATTRIBUTE = "sourcepond.io.watched.path";

	/**
	 * Returns the {@link ResourceFilter} instance to be used in conjunction
	 * with this listener. Defaults to {@link ResourceFilter#FILES_ONLY}.
	 * 
	 * @return {@link ResourceFilter} instance, should not be {@code null}
	 */
	default ResourceFilter getFilter() {
		return FILES_ONLY;
	}

	/**
	 * Receives change notifications tracked by the {@link Workspace} on which
	 * this listener is registered. See {@link Type} for an overview of all
	 * supported event kinds.
	 * 
	 * @param pEvent
	 *            Change notification, never {@code null}
	 */
	void resourceChange(ResourceEvent pEvent);
}
