package ch.sourcepond.io.fileobserver;

import static ch.sourcepond.io.fileobserver.ResourceEvent.Type.RESOURCE_CREATED;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.Test;

import ch.sourcepond.io.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
public class ResourceEventTest {
	private final Path resource = mock(Path.class);
	private final ResourceEvent event = new ResourceEvent(resource, RESOURCE_CREATED);

	/**
	 * 
	 */
	@Test(expected = IllegalArgumentException.class)
	public void newResourceNoType() {
		new ResourceEvent(resource, null);
	}

	/**
	 * 
	 */
	@Test
	public void verifyGetResource() {
		assertSame(resource, event.getSource());
	}

	/**
	 * 
	 */
	@Test
	public void verifyGetType() {
		assertSame(RESOURCE_CREATED, event.getType());
	}
}
