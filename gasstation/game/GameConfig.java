package gasstation.game;

import java.math.BigDecimal;

/**
 * Every tunable number in the game, in one place.
 *
 * <p>Defaults are the "normal" difficulty. Setters are fluent, so a scenario can
 * be written in one expression:
 *
 * <pre>
 *   GameConfig hard = GameConfig.hard().setDays(45).setSeed(1234L);
 * </pre>
 */
public class GameConfig {

    // --- the run itself
    private String stationName = "Riverside Fuels";
    private int days = 40;
    private long seed = 0L; // 0 means "pick a random seed"

    // --- opening position
    private BigDecimal startingCash = Money.cash(8_000);
    private BigDecimal startingDebt = Money.cash(15_000);
    private BigDecimal targetNetWorth = Money.cash(25_000);
    private double tankCapacityLitres = 10_000;
    private double startingFuelLitres = 3_000;
    private int startingPumps = 2;
    private BigDecimal startingPumpPrice = Money.perLitre(1.559);

    // --- running the place
    private BigDecimal dailyFixedCosts = Money.cash(220);
    private BigDecimal deliveryFee = Money.cash(150);
    private double dailyInterestRate = 0.0008; // 0.08% a day on the outstanding loan

    // --- demand
    private int passingTraffic = 240;        // cars going past the door each day
    private BigDecimal referencePrice = Money.perLitre(1.559);
    private double priceSensitivity = 14.0;  // how sharply drivers react to being off the going rate
    private int fillsPerPumpPerDay = 38;     // throughput of one pump in a day
    private double minFillLitres = 22;
    private double maxFillLitres = 68;

    // --- wholesale market
    private double startingWholesale = 1.28;
    private double wholesaleMean = 1.30;
    private double wholesaleVolatility = 0.035;
    private double wholesaleReversion = 0.15;
    private double wholesaleFloor = 0.95;
    private double wholesaleCeiling = 1.85;

    // --- events
    private double eventChance = 0.30;

    public static GameConfig normal() {
        return new GameConfig();
    }

    /** More cash, less debt, a gentler market. */
    public static GameConfig easy() {
        return new GameConfig()
                .setStartingCash(Money.cash(12_000))
                .setStartingDebt(Money.cash(10_000))
                .setTargetNetWorth(Money.cash(34_000))
                .setPassingTraffic(285)
                .setPriceSensitivity(11.0)
                .setWholesaleVolatility(0.025)
                .setEventChance(0.22)
                .setDailyFixedCosts(Money.cash(180));
    }

    /** Thin cash, a big loan, a jumpy market and a higher bar. */
    public static GameConfig hard() {
        return new GameConfig()
                .setStartingCash(Money.cash(5_000))
                .setStartingDebt(Money.cash(20_000))
                .setTargetNetWorth(Money.cash(12_000))
                .setPassingTraffic(215)
                .setPriceSensitivity(16.0)
                .setWholesaleVolatility(0.05)
                .setEventChance(0.40)
                .setDailyFixedCosts(Money.cash(260));
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public String getStationName() { return stationName; }
    public int getDays() { return days; }
    public long getSeed() { return seed; }
    public BigDecimal getStartingCash() { return startingCash; }
    public BigDecimal getStartingDebt() { return startingDebt; }
    public BigDecimal getTargetNetWorth() { return targetNetWorth; }
    public double getTankCapacityLitres() { return tankCapacityLitres; }
    public double getStartingFuelLitres() { return startingFuelLitres; }
    public int getStartingPumps() { return startingPumps; }
    public BigDecimal getStartingPumpPrice() { return startingPumpPrice; }
    public BigDecimal getDailyFixedCosts() { return dailyFixedCosts; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public double getDailyInterestRate() { return dailyInterestRate; }
    public int getPassingTraffic() { return passingTraffic; }
    public BigDecimal getReferencePrice() { return referencePrice; }
    public double getPriceSensitivity() { return priceSensitivity; }
    public int getFillsPerPumpPerDay() { return fillsPerPumpPerDay; }
    public double getMinFillLitres() { return minFillLitres; }
    public double getMaxFillLitres() { return maxFillLitres; }
    public double getStartingWholesale() { return startingWholesale; }
    public double getWholesaleMean() { return wholesaleMean; }
    public double getWholesaleVolatility() { return wholesaleVolatility; }
    public double getWholesaleReversion() { return wholesaleReversion; }
    public double getWholesaleFloor() { return wholesaleFloor; }
    public double getWholesaleCeiling() { return wholesaleCeiling; }
    public double getEventChance() { return eventChance; }

    // ------------------------------------------------------------------
    // Fluent setters
    // ------------------------------------------------------------------

    public GameConfig setStationName(String v) { this.stationName = v; return this; }
    public GameConfig setDays(int v) { this.days = requirePositive(v, "days"); return this; }
    public GameConfig setSeed(long v) { this.seed = v; return this; }
    public GameConfig setStartingCash(BigDecimal v) { this.startingCash = v; return this; }
    public GameConfig setStartingDebt(BigDecimal v) { this.startingDebt = v; return this; }
    public GameConfig setTargetNetWorth(BigDecimal v) { this.targetNetWorth = v; return this; }
    public GameConfig setTankCapacityLitres(double v) { this.tankCapacityLitres = v; return this; }
    public GameConfig setStartingFuelLitres(double v) { this.startingFuelLitres = v; return this; }
    public GameConfig setStartingPumps(int v) { this.startingPumps = requirePositive(v, "pumps"); return this; }
    public GameConfig setStartingPumpPrice(BigDecimal v) { this.startingPumpPrice = v; return this; }
    public GameConfig setDailyFixedCosts(BigDecimal v) { this.dailyFixedCosts = v; return this; }
    public GameConfig setDeliveryFee(BigDecimal v) { this.deliveryFee = v; return this; }
    public GameConfig setDailyInterestRate(double v) { this.dailyInterestRate = v; return this; }
    public GameConfig setPassingTraffic(int v) { this.passingTraffic = requirePositive(v, "passingTraffic"); return this; }
    public GameConfig setReferencePrice(BigDecimal v) { this.referencePrice = v; return this; }
    public GameConfig setPriceSensitivity(double v) { this.priceSensitivity = v; return this; }
    public GameConfig setFillsPerPumpPerDay(int v) { this.fillsPerPumpPerDay = requirePositive(v, "fills"); return this; }
    public GameConfig setMinFillLitres(double v) { this.minFillLitres = v; return this; }
    public GameConfig setMaxFillLitres(double v) { this.maxFillLitres = v; return this; }
    public GameConfig setStartingWholesale(double v) { this.startingWholesale = v; return this; }
    public GameConfig setWholesaleMean(double v) { this.wholesaleMean = v; return this; }
    public GameConfig setWholesaleVolatility(double v) { this.wholesaleVolatility = v; return this; }
    public GameConfig setWholesaleReversion(double v) { this.wholesaleReversion = v; return this; }
    public GameConfig setEventChance(double v) { this.eventChance = v; return this; }

    private static int requirePositive(int value, String what) {
        if (value <= 0) {
            throw new IllegalArgumentException(what + " must be positive, was " + value);
        }
        return value;
    }
}
