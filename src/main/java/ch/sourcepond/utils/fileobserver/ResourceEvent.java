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
