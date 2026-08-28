package gasstation.game;

import gasstation.GasStation;

import java.math.BigDecimal;

/**
 * Something you can spend your profit on.
 *
 * <p>Two upgrades change the station itself, so they override
 * {@link #install(GasStation)}. The rest are passive: the day simulator reads
 * their bonuses when it works out demand and income.
 */
public enum Upgrade {

    /** More throughput - worth it once you are turning cars away. */
    EXTRA_PUMP("Extra pump", 4_200, "+1 pump, so 38 more fills a day", 0.00, 0.00, 0.0) {
        @Override
        public void install(GasStation station) {
            station.addPump();
        }
    },

    /** More storage - worth it when wholesale is cheap and you want to stock up. */
    BIGGER_TANK("Bigger tank", 3_200, "+8,000 L of storage, so you can buy the dips", 0.00, 0.00, 0.0) {
        @Override
        public void install(GasStation station) {
            station.getTank().expandCapacity(8_000);
        }
    },

    /** Weather protection and a bit of kerb appeal. */
    CANOPY("Forecourt canopy", 1_500, "+8% demand, and bad weather hurts half as much", 0.00, 0.08, 0.0),

    /** Shop income scales with how many people you get through the forecourt. */
    CONVENIENCE_STORE("Convenience store", 4_800, "$3.60 of shop sales per customer", 3.60, 0.04, 0.0),

    /** Cheaper than the shop, smaller return. */
    CAR_WASH("Car wash", 2_600, "$1.80 per customer", 1.80, 0.02, 0.0),

    /** Regulars stop shopping around quite so hard. */
    LOYALTY_APP("Loyalty app", 3_000, "Regulars mind a high price less", 0.00, 0.03, 3.0);

    private final String label;
    private final int cost;
    private final String description;
    private final double perCustomerIncome;
    private final double demandBonus;
    private final double sensitivityReduction;

    Upgrade(String label, int cost, String description,
            double perCustomerIncome, double demandBonus, double sensitivityReduction) {
        this.label = label;
        this.cost = cost;
        this.description = description;
        this.perCustomerIncome = perCustomerIncome;
        this.demandBonus = demandBonus;
        this.sensitivityReduction = sensitivityReduction;
    }

    public String getLabel() { return label; }

    public BigDecimal getCost() { return Money.cash(cost); }

    public String getDescription() { return description; }

    /** Extra income earned per customer served. */
    public double getPerCustomerIncome() { return perCustomerIncome; }

    /** Extra demand as a fraction, e.g. 0.08 for +8%. */
    public double getDemandBonus() { return demandBonus; }

    /** How much this softens customers' reaction to a high price. */
    public double getSensitivityReduction() { return sensitivityReduction; }

    /**
     * Applies any one-off change to the station. The default is to do nothing -
     * most upgrades are passive bonuses read at simulation time.
     */
    public void install(GasStation station) {
        // no structural change by default
    }

    @Override
    public String toString() {
        return label;
    }
}
