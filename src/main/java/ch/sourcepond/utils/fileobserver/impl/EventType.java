package ch.sourcepond.utils.fileobserver.impl;

import ch.sourcepond.utils.fileobserver.ChangeObserver;
import ch.sourcepond.utils.fileobserver.Resource;

/**
 * @author rolandhauser
 *
 */
interface EventType {
	EventType CREATED = new EventType() {

		@Override
		public void dispatch(final ChangeObserver pObserver, final Resource pResource) {
			pObserver.created(pResource);
		}
	};
	EventType MODIFIED = new EventType() {

		@Override
		public void dispatch(final ChangeObserver pObserver, final Resource pResource) {
			pObserver.modified(pResource);
		}
	};
	EventType DELETED = new EventType() {

		@Override
		public void dispatch(final ChangeObserver pObserver, final Resource pResource) {
			pObserver.deleted(pResource.getOriginContent());
		}
	};

	void dispatch(ChangeObserver pObserver, Resource pResource);
}
