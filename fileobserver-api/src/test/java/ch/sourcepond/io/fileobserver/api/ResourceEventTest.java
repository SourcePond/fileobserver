package ch.sourcepond.io.fileobserver.api;

import static ch.sourcepond.io.fileobserver.api.ResourceEvent.Type.RESOURCE_CREATED;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.Test;

import ch.sourcepond.io.fileobserver.api.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
public class ResourceEventTest {
	private final Path absolutePath = mock(Path.class);
	private final Path relativePath = mock(Path.class);
	private final ResourceEvent event = new ResourceEvent(absolutePath, relativePath, RESOURCE_CREATED);

	/**
	 * 
	 */
	@Test(expected = IllegalArgumentException.class)
	public void newResourceNoType() {
		new ResourceEvent(absolutePath, relativePath, null);
	}

	/**
	 * 
	 */
	@Test(expected = NullPointerException.class)
	public void newRelativePathDefined() {
		new ResourceEvent(absolutePath, null, RESOURCE_CREATED);
	}

	/**
	 * 
	 */
	@Test
	public void verifyGetSource() {
		assertSame(absolutePath, event.getSource());
	}

	/**
	 * 
	 */
	@Test
	public void verifyGetType() {
		assertSame(RESOURCE_CREATED, event.getType());
	}
}
