package ch.sourcepond.utils.fileobserver.impl;

import ch.sourcepond.utils.fileobserver.ChangeObserver;

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
	Runnable newObserverTask(final ChangeObserver pObserver, final EventType pType, final InternalResource pResource) {
		return new InformObserverTask(pObserver, pType, pResource);
	}
}
