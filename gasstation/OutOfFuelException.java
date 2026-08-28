package gasstation;

/**
 * Thrown when the station cannot supply the fuel a customer asked for, even
 * after trying to arrange a delivery.
 *
 * <p>Checked on purpose: running low is a normal business condition the caller
 * is expected to handle (apologise to the customer), not a bug.
 */
public class OutOfFuelException extends Exception {

    private static final long serialVersionUID = 1L;

    private final double requestedLitres;
    private final double availableLitres;

    public OutOfFuelException(double requestedLitres, double availableLitres) {
        super(String.format("Requested %.2f L but only %.2f L is available", requestedLitres, availableLitres));
        this.requestedLitres = requestedLitres;
        this.availableLitres = availableLitres;
    }

    public double getRequestedLitres() {
        return requestedLitres;
    }

    /** How much the station could have supplied - useful for offering a partial fill. */
    public double getAvailableLitres() {
        return availableLitres;
    }
}
