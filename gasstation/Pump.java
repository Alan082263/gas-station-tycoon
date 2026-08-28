package gasstation;

import java.util.Objects;

/**
 * One of the station's pumps.
 *
 * <p>A pump is the only route fuel takes out of the {@link FuelTank}. It serves
 * at most one customer at a time, which is what makes "the station has two
 * pumps" an actual constraint rather than a comment.
 *
 * <p>Pumps are created by the {@link GasStation} that owns them.
 */
public class Pump {

    private final int number;
    private final FuelTank tank;

    private Customer currentCustomer;
    private double lifetimeLitresDispensed;

    Pump(int number, FuelTank tank) {
        this.number = number;
        this.tank = Objects.requireNonNull(tank, "tank");
    }

    public int getNumber() {
        return number;
    }

    public boolean isInUse() {
        return currentCustomer != null;
    }

    /** The customer at this pump, or {@code null} if it is free. */
    public Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public double getLifetimeLitresDispensed() {
        return lifetimeLitresDispensed;
    }

    /**
     * Claims the pump for a customer.
     *
     * @throws IllegalStateException if someone is already using it
     */
    public void beginFuelling(Customer customer) {
        Objects.requireNonNull(customer, "customer");
        if (isInUse()) {
            throw new IllegalStateException(
                    "Pump " + number + " is already serving " + currentCustomer.getName());
        }
        this.currentCustomer = customer;
    }

    /** Releases the pump for the next customer. Safe to call on an idle pump. */
    public void finishFuelling() {
        this.currentCustomer = null;
    }

    /**
     * Draws fuel from the tank and meters it. Called by the station, which
     * handles pricing and receipts.
     *
     * @return the litres dispensed
     * @throws IllegalStateException if nobody is at the pump, or the tank is short
     */
    double dispense(double litres) {
        if (!isInUse()) {
            throw new IllegalStateException("Pump " + number + " has no customer at it");
        }
        tank.withdraw(litres);
        lifetimeLitresDispensed += litres;
        return litres;
    }

    @Override
    public String toString() {
        String state = isInUse() ? "serving " + currentCustomer.getName() : "idle";
        return String.format("Pump %d [%s, %.1f L lifetime]", number, state, lifetimeLitresDispensed);
    }
}
