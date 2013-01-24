package es.upv.dsic.geoclef.geography;

/**
 * Coordinates are outside the correct range (it may happens if the database includes some errors)
 * @author buscaldi
 *
 */
public class InvalidCoordinateRangeException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidCoordinateRangeException(String string) {
		super(string);
	}

}
