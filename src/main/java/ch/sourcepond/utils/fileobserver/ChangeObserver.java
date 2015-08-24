package ch.sourcepond.utils.fileobserver;

import java.net.URL;

/**
 * @author rolandhauser
 *
 */
public interface ChangeObserver {

	/**
	 * @param pResource
	 */
	void created(Resource pResource);

	/**
	 * @param pResource
	 */
	void modified(Resource pResource);

	/**
	 * @param pContent
	 */
	void deleted(URL pContent);
}
