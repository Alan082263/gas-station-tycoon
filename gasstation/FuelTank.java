package gasstation;

/**
 * The station's main storage tank.
 *
 * <p>The tank knows two things only: how much it can hold and how much it is
 * holding right now. It refuses any operation that would put it into an
 * impossible state (a negative level, or more fuel than it can hold).
 *
 * <p>Note that {@link #withdraw(double)} is package-private: fuel leaves the
 * tank through a {@link Pump} and nowhere else. Refilling, by contrast, is
 * public, because that is the owner's job.
 */
public class FuelTank {

    /** Tolerance for floating-point comparisons of litre amounts. */
    static final double EPSILON = 1e-9;

    private double capacityLitres;
    private double levelLitres;

    /**
     * @param capacityLitres     how much the tank holds when full; must be positive
     * @param initialLevelLitres how much is in it to start with; 0 .. capacity
     */
    public FuelTank(double capacityLitres, double initialLevelLitres) {
        if (capacityLitres <= 0) {
            throw new IllegalArgumentException("Tank capacity must be positive, was " + capacityLitres);
        }
        if (initialLevelLitres < 0 || initialLevelLitres > capacityLitres) {
            throw new IllegalArgumentException(
                    "Initial level must be between 0 and " + capacityLitres + " litres, was " + initialLevelLitres);
        }
        this.capacityLitres = capacityLitres;
        this.levelLitres = initialLevelLitres;
    }

    public double getCapacityLitres() {
        return capacityLitres;
    }

    public double getLevelLitres() {
        return levelLitres;
    }

    /** Empty space in the tank, in litres. */
    public double getHeadroomLitres() {
        return capacityLitres - levelLitres;
    }

    /** How full the tank is, 0.0 .. 100.0. */
    public double getFillPercentage() {
        return (levelLitres / capacityLitres) * 100.0;
    }

    /** True if the tank could supply {@code litres} right now. */
    public boolean holdsAtLeast(double litres) {
        return litres <= levelLitres + EPSILON;
    }

    public boolean isEmpty() {
        return levelLitres <= EPSILON;
    }

    /**
     * Removes fuel from the tank. Only a pump may do this.
     *
     * @return the litres removed
     * @throws IllegalArgumentException if {@code litres} is not positive
     * @throws IllegalStateException    if the tank does not hold that much
     */
    double withdraw(double litres) {
        if (litres <= 0) {
            throw new IllegalArgumentException("Amount withdrawn must be positive, was " + litres);
        }
        if (!holdsAtLeast(litres)) {
            throw new IllegalStateException(
                    String.format("Tank holds %.2f L, cannot withdraw %.2f L", levelLitres, litres));
        }
        levelLitres -= litres;
        if (levelLitres < EPSILON) {
            levelLitres = 0.0; // tidy away floating-point dust
        }
        return litres;
    }

    /**
     * Takes a delivery of fuel, up to whatever fits.
     *
     * @return the litres actually accepted (less than requested if the tank filled up)
     */
    public double addFuel(double litres) {
        if (litres <= 0) {
            throw new IllegalArgumentException("Delivery must be positive, was " + litres);
        }
        double accepted = Math.min(litres, getHeadroomLitres());
        levelLitres += accepted;
        return accepted;
    }

    /**
     * Installs extra storage - a bigger tank, or a second one plumbed in.
     * The fuel already in the ground is unaffected.
     *
     * @param extraLitres capacity to add; must be positive
     */
    public void expandCapacity(double extraLitres) {
        if (extraLitres <= 0) {
            throw new IllegalArgumentException("Added capacity must be positive, was " + extraLitres);
        }
        this.capacityLitres += extraLitres;
    }

    /**
     * Tops the tank right up.
     *
     * @return the litres added
     */
    public double refillToCapacity() {
        double added = getHeadroomLitres();
        levelLitres = capacityLitres;
        return added;
    }

    @Override
    public String toString() {
        return String.format("Tank: %.1f / %.1f L (%.1f%% full)", levelLitres, capacityLitres, getFillPercentage());
    }
}
