package ch.sourcepond.utils.fileobserver;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;

/**
 * @author rolandhauser
 *
 */
public interface Workspace extends Closeable {

	/**
	 * @param pContent
	 * @return
	 * @throws IOException
	 */
	Resource watchFile(URL pOriginContent, String... pPath) throws IOException;

	/**
	 * @param pContent
	 * @return
	 * @throws IOException
	 */
	Resource watchFile(URL pOriginContent, boolean pReplaceExisting, String... pPath) throws IOException;
}
