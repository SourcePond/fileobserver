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
	void addObserver(ResourceChangeListener pObserver);

	/**
	 * @param pObserver
	 */
	void removeObserver(ResourceChangeListener pObserver);

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
