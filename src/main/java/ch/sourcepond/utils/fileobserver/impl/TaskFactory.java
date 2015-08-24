package ch.sourcepond.utils.fileobserver.impl;

import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
class TaskFactory {

	/**
	 * @param pObserver
	 * @param pResource
	 * @return
	 */
	Runnable newObserverTask(final ResourceChangeListener pObserver, final ResourceEvent pEvent,
			final InternalResource pResource) {
		return new InformObserverTask(pObserver, pEvent, pResource);
	}
}
