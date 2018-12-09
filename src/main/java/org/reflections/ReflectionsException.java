/*
 * User: ophir
 * Date: Mar 28, 2009
 * Time: 12:52:22 AM
 */
package org.reflections;

public class ReflectionsException extends RuntimeException {
	private static final long serialVersionUID = -9026251130714337186L;

	public ReflectionsException(String message) {
		super(message);
	}

	public ReflectionsException(String message, Throwable cause) {
		super(message, cause);
	}

    public ReflectionsException(Throwable cause) {
        super(cause);
    }
}
