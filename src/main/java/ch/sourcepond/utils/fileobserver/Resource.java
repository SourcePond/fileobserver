package ch.sourcepond.utils.fileobserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author rolandhauser
 *
 */
public interface Resource {

	/**
	 * @param pObserver
	 */
	void addListener(ResourceChangeListener pListener);

	/**
	 * @param pObserver
	 */
	void removeListener(ResourceChangeListener pListener);

	/**
	 * @return
	 */
	URL getOriginContent();

	/**
	 * @return
	 * @throws IOException
	 */
	InputStream open() throws IOException;
}
