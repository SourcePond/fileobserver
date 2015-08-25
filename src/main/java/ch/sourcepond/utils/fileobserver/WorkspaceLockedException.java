package ch.sourcepond.utils.fileobserver;

/**
 * @author rolandhauser
 *
 */
public final class WorkspaceLockedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1321903497565893588L;

	public WorkspaceLockedException() {
		super();
	}

	public WorkspaceLockedException(final String message) {
		super(message);
	}

	public WorkspaceLockedException(final Throwable cause) {
		super(cause);
	}

	public WorkspaceLockedException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
