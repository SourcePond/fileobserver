package ch.sourcepond.utils.fileobserver;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;

/**
 * @author rolandhauser
 *
 */
public interface Watcher extends Closeable {

	/**
	 * @param pContent
	 * @return
	 * @throws IOException
	 */
	Resource watchFile(URL pContent, String... pPath) throws IOException;
}
