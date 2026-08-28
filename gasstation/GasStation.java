package gasstation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The gas station itself: one tank, two pumps, one price per litre, and the
 * owner's standing instruction to keep the tank full enough to serve customers.
 *
 * <p>Responsibilities are split like this:
 * <ul>
 *   <li>{@link FuelTank} - knows how much fuel there is.</li>
 *   <li>{@link Pump}     - meters fuel out of the tank, one customer at a time.</li>
 *   <li>{@code GasStation} - sets the price, keeps the tank stocked, and records sales.</li>
 * </ul>
 *
 * <p>The stocking rule: before any sale the station checks whether that sale
 * would take the tank below its reserve (a percentage of capacity, 20% by
 * default). If it would, the station orders a delivery and tops the tank right
 * up. Customers only get turned away when a delivery cannot be had.
 */
public class GasStation {

    /** A station opens with two pumps unless told otherwise. */
    public static final int DEFAULT_PUMP_COUNT = 2;

    private static final double DEFAULT_RESERVE_FRACTION = 0.20;

    private final String name;
    private final FuelTank tank;
    private final List<Pump> pumps = new ArrayList<>();
    private final List<Transaction> sales = new ArrayList<>();

    private BigDecimal pricePerLitre;
    private double reserveFraction = DEFAULT_RESERVE_FRACTION;

    /** Set false to simulate a supplier who cannot deliver today. */
    private boolean deliveriesAvailable = true;

    private int nextTransactionId = 1;
    private int deliveryCount;
    private double litresDelivered;
    private double litresLost;

    /**
     * @param name               the station's trading name
     * @param tankCapacityLitres size of the main tank
     * @param initialFuelLitres  fuel in the tank at opening time
     * @param pricePerLitre      the pump price, e.g. {@code new BigDecimal("1.729")}
     */
    public GasStation(String name, double tankCapacityLitres, double initialFuelLitres, BigDecimal pricePerLitre) {
        this(name, tankCapacityLitres, initialFuelLitres, pricePerLitre, DEFAULT_PUMP_COUNT);
    }

    /**
     * @param pumpCount how many pumps to install; must be at least one
     */
    public GasStation(String name, double tankCapacityLitres, double initialFuelLitres,
                      BigDecimal pricePerLitre, int pumpCount) {
        if (pumpCount < 1) {
            throw new IllegalArgumentException("A station needs at least one pump, was " + pumpCount);
        }
        this.name = Objects.requireNonNull(name, "name");
        this.tank = new FuelTank(tankCapacityLitres, initialFuelLitres);
        this.pricePerLitre = validPrice(pricePerLitre);
        for (int i = 1; i <= pumpCount; i++) {
            pumps.add(new Pump(i, tank));
        }
    }

    // ------------------------------------------------------------------
    // Serving customers
    // ------------------------------------------------------------------

    /**
     * Serves a customer from the first free pump: finds a pump, arranges fuel if
     * the tank is getting low, dispenses, and prices the sale.
     *
     * @return the completed sale
     * @throws NoPumpAvailableException if both pumps are busy
     * @throws OutOfFuelException       if the station cannot supply that much fuel
     */
    public Transaction serve(Customer customer) throws NoPumpAvailableException, OutOfFuelException {
        Objects.requireNonNull(customer, "customer");
        Pump pump = findAvailablePump().orElseThrow(() -> new NoPumpAvailableException(
                "Both pumps are busy; " + customer.getName() + " has to wait"));

        pump.beginFuelling(customer);
        try {
            return fuel(pump, customer, customer.getRequestedLitres());
        } finally {
            pump.finishFuelling(); // the pump is freed even if the sale fails
        }
    }

    /**
     * Fuels a customer at a pump the caller has already claimed with
     * {@link Pump#beginFuelling(Customer)}. Use this when you want to hold a
     * pump open across several steps; otherwise use {@link #serve(Customer)}.
     *
     * @throws OutOfFuelException if the station cannot supply that much fuel
     */
    public Transaction fuel(Pump pump, Customer customer, double litres) throws OutOfFuelException {
        Objects.requireNonNull(pump, "pump");
        Objects.requireNonNull(customer, "customer");
        if (litres <= 0) {
            throw new IllegalArgumentException("Fill must be more than 0 litres, was " + litres);
        }
        if (pump.getCurrentCustomer() != customer) {
            throw new IllegalStateException("Pump " + pump.getNumber() + " is not serving " + customer.getName());
        }

        ensureFuelFor(litres);
        if (!tank.holdsAtLeast(litres)) {
            throw new OutOfFuelException(litres, tank.getLevelLitres());
        }

        pump.dispense(litres);

        Transaction sale = new Transaction(nextTransactionId++, pump.getNumber(),
                customer.getName(), litres, pricePerLitre);
        sales.add(sale);
        customer.addReceipt(sale);
        return sale;
    }

    /** The first pump not currently in use, if there is one. */
    public Optional<Pump> findAvailablePump() {
        for (Pump pump : pumps) {
            if (!pump.isInUse()) {
                return Optional.of(pump);
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // The owner's job: keeping the tank stocked
    // ------------------------------------------------------------------

    /**
     * Orders a delivery if this sale would take the tank below its reserve.
     * Silently does nothing if no delivery can be had - the sale then either
     * succeeds on what is left, or fails with {@link OutOfFuelException}.
     */
    private void ensureFuelFor(double litres) {
        double levelAfterSale = tank.getLevelLitres() - litres;
        if (levelAfterSale < getReserveLitres() && deliveriesAvailable) {
            orderDelivery();
        }
    }

    /**
     * Calls the supplier and fills the tank to capacity.
     *
     * @return the litres delivered (0 if the tank was already full)
     */
    public double orderDelivery() {
        double added = tank.refillToCapacity();
        if (added > FuelTank.EPSILON) {
            deliveryCount++;
            litresDelivered += added;
        }
        return added;
    }

    /**
     * Takes a delivery of a specific size, for when the owner is buying to a
     * budget rather than simply topping up.
     *
     * @param litres litres ordered
     * @return the litres actually accepted (less than ordered if the tank filled up)
     */
    public double takeDelivery(double litres) {
        double accepted = tank.addFuel(litres);
        if (accepted > FuelTank.EPSILON) {
            deliveryCount++;
            litresDelivered += accepted;
        }
        return accepted;
    }

    /**
     * Installs another pump on the forecourt.
     *
     * @return the new pump
     */
    public Pump addPump() {
        Pump pump = new Pump(pumps.size() + 1, tank);
        pumps.add(pump);
        return pump;
    }

    /** How many pumps the station currently has. */
    public int getPumpCount() {
        return pumps.size();
    }

    /**
     * Fuel that left the tank without being sold - a spill, a leak, or theft.
     *
     * @return the litres actually lost (never more than the tank held)
     */
    public double recordFuelLoss(double litres) {
        if (litres <= 0) {
            throw new IllegalArgumentException("Loss must be positive, was " + litres);
        }
        double lost = Math.min(litres, tank.getLevelLitres());
        if (lost <= FuelTank.EPSILON) {
            return 0;
        }
        tank.withdraw(lost);
        litresLost += lost;
        return lost;
    }

    /** Total fuel lost to spills and leaks. */
    public double getLitresLost() {
        return litresLost;
    }

    /** The level below which the station reorders, in litres. */
    public double getReserveLitres() {
        return tank.getCapacityLitres() * reserveFraction;
    }

    /** True when the tank has fallen to or below the reserve level. */
    public boolean needsRefill() {
        return tank.getLevelLitres() <= getReserveLitres() + FuelTank.EPSILON;
    }

    /**
     * @param fraction reserve level as a fraction of capacity, 0.0 .. 1.0
     */
    public void setReserveFraction(double fraction) {
        if (fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Reserve fraction must be between 0 and 1, was " + fraction);
        }
        this.reserveFraction = fraction;
    }

    public double getReserveFraction() {
        return reserveFraction;
    }

    public void setDeliveriesAvailable(boolean available) {
        this.deliveriesAvailable = available;
    }

    public boolean isDeliveriesAvailable() {
        return deliveriesAvailable;
    }

    // ------------------------------------------------------------------
    // Price and books
    // ------------------------------------------------------------------

    public BigDecimal getPricePerLitre() {
        return pricePerLitre;
    }

    public void setPricePerLitre(BigDecimal pricePerLitre) {
        this.pricePerLitre = validPrice(pricePerLitre);
    }

    private static BigDecimal validPrice(BigDecimal price) {
        Objects.requireNonNull(price, "pricePerLitre");
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Price per litre must be positive, was " + price);
        }
        return price;
    }

    public String getName() {
        return name;
    }

    public FuelTank getTank() {
        return tank;
    }

    public List<Pump> getPumps() {
        return Collections.unmodifiableList(pumps);
    }

    public List<Transaction> getSales() {
        return Collections.unmodifiableList(sales);
    }

    public double getTotalLitresSold() {
        double litres = 0;
        for (Transaction sale : sales) {
            litres += sale.getLitres();
        }
        return litres;
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = BigDecimal.ZERO;
        for (Transaction sale : sales) {
            revenue = revenue.add(sale.getTotalPrice());
        }
        return revenue;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }

    public double getLitresDelivered() {
        return litresDelivered;
    }

    /** A short status line: tank level, pump states, price. */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" | ").append(tank)
          .append(String.format(" | reserve %.0f L", getReserveLitres()))
          .append(" | $").append(pricePerLitre).append("/L");
        for (Pump pump : pumps) {
            sb.append(System.lineSeparator()).append("    ").append(pump);
        }
        return sb.toString();
    }

    /** End-of-day report: every sale, plus the totals. */
    public String getSalesReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== SALES REPORT: ").append(name).append(" =====").append(System.lineSeparator());
        if (sales.isEmpty()) {
            sb.append("  (no sales)").append(System.lineSeparator());
        } else {
            for (Transaction sale : sales) {
                sb.append("  ").append(sale).append(System.lineSeparator());
            }
        }
        sb.append(String.format("  Sales      : %d%n", sales.size()));
        sb.append(String.format("  Litres sold: %.2f L%n", getTotalLitresSold()));
        sb.append(String.format("  Revenue    : $%s%n", getTotalRevenue()));
        sb.append(String.format("  Deliveries : %d (%.1f L)%n", deliveryCount, litresDelivered));
        sb.append(String.format("  Tank now   : %.1f L (%.1f%%)", tank.getLevelLitres(), tank.getFillPercentage()));
        return sb.toString();
    }

    @Override
    public String toString() {
        return name + " [" + pumps.size() + " pumps, " + tank + "]";
    }
}
