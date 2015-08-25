package ch.sourcepond.utils.fileobserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author rolandhauser
 *
 */
public class WorkspaceLockedExceptionTest {
	private static final String ANY_MESSAGE = "anyMessage";
	private static final Exception ANY_CAUSE = new Exception();

	/**
	 * 
	 */
	@Test
	public void verifyNewWithoutParams() {
		final WorkspaceLockedException ex = new WorkspaceLockedException();
		assertNull(ex.getCause());
		assertNull(ex.getMessage());
	}

	/**
	 * 
	 */
	@Test
	public void verifyNewWithMessage() {
		final WorkspaceLockedException ex = new WorkspaceLockedException(ANY_MESSAGE);
		assertEquals(ANY_MESSAGE, ex.getMessage());
		assertNull(ex.getCause());
	}

	/**
	 * 
	 */
	@Test
	public void verifyNewWithCause() {
		final WorkspaceLockedException ex = new WorkspaceLockedException(ANY_CAUSE);
		assertEquals(Exception.class.getName(), ex.getMessage());
		assertEquals(ANY_CAUSE, ex.getCause());
	}

	/**
	 * 
	 */
	@Test
	public void verifyNewWithMessageAndCause() {
		final WorkspaceLockedException ex = new WorkspaceLockedException(ANY_MESSAGE, ANY_CAUSE);
		assertEquals(ANY_MESSAGE, ex.getMessage());
		assertEquals(ANY_CAUSE, ex.getCause());
	}
}
