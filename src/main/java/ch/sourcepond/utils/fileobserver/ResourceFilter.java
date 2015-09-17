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

/**
 * A resource filter is used to decide whether a {@link ResourceChangeListener}
 * should receive a {@link ResourceEvent} or not.
 *
 */
public interface ResourceFilter {

	/**
	 * Common filter which dispatches <em>all</em> {@link ResourceEvent} objects
	 * to its assigned {@link ResourceChangeListener} instances.
	 */
	ResourceFilter DISPATCH_ALL = new ResourceFilter() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see ch.sourcepond.utils.fileobserver.ResourceFilter#isDispatched(ch.
		 * sourcepond.utils.fileobserver.ResourceEvent)
		 */
		@Override
		public boolean isDispatched(final ResourceEvent pEvent) {
			return true;
		}

	};

	/**
	 * Checks whether the event specified should be dispatched. When this method
	 * returns {@code true}, all listeners assigned to this filter will receive
	 * the event. If {@code false} is returned, no listener assigned to this
	 * filter will receive the event.
	 * 
	 * @param pEvent
	 *            Event to be checked, must not be {@code null}
	 * @return {@code true} if the event should be dispatched, {@code false}
	 *         otherwise.
	 */
	boolean isDispatched(ResourceEvent pEvent);
}
