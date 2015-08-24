package ch.sourcepond.utils.fileobserver;

/**
 * @author rolandhauser
 *
 */
public final class WorkspaceLockedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7901691209040103022L;

	public WorkspaceLockedException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
