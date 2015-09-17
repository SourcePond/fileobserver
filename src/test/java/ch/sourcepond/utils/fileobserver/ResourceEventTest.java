package ch.sourcepond.utils.fileobserver;

import static ch.sourcepond.utils.fileobserver.ResourceEvent.Type.RESOURCE_CREATED;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.Test;

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
