package ch.sourcepond.utils.fileobserver;

import java.util.EventObject;

/**
 * @author rolandhauser
 *
 */
public final class ResourceEvent extends EventObject {

	/**
	 * 
	 *
	 */
	public static enum Type {
		LISTENER_ADDED, LISTENER_REMOVED, RESOURCE_CREATED, RESOURCE_MODIFIED, RESOURCE_DELETED
	}

	/**
	 * 
	 */
	public ResourceEvent(final Resource pSource, final Type pType) {
		super(pSource);
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
}
