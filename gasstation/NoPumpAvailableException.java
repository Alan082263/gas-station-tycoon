package gasstation;

/**
 * Thrown when a customer arrives and both pumps are occupied.
 */
public class NoPumpAvailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoPumpAvailableException(String message) {
        super(message);
    }
}
