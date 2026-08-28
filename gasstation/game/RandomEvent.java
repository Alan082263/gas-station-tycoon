package gasstation.game;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Something that happens to you, good or bad.
 *
 * <p>An event can pull four levers: how many customers show up, what wholesale
 * fuel costs from tomorrow, whether the tanker can reach you, and your cash.
 */
public enum RandomEvent {

    TANKER_STRIKE("Tanker drivers' strike",
            "The depot drivers walked out. No deliveries today.",
            1.00, 0.00, 0, 0, 0, true, 7),

    ROADWORKS("Roadworks",
            "The council closed a lane outside. Traffic is crawling past you.",
            0.55, 0.00, 0, 0, 0, false, 9),

    PRICE_WAR("Price war",
            "The station across the road slashed its price overnight.",
            0.70, 0.00, 0, 0, 0, false, 10),

    CARD_OUTAGE("Card machines down",
            "Your card terminals were dead until mid-afternoon.",
            0.78, 0.00, 0, 0, 0, false, 8),

    PUMP_BREAKDOWN("Pump breakdown",
            "A meter jammed solid. One pump is out all day and the engineer wants paying.",
            1.00, 0.00, 450, 1, 0, false, 9),

    FUEL_LEAK("Leaking seal",
            "A tank seal failed. You lost fuel and paid for the cleanup.",
            0.92, 0.00, 1_200, 0, 400, false, 5),

    INSPECTION("Trading standards",
            "An inspector found a mislabelled pump and fined you.",
            1.00, 0.00, 600, 0, 0, false, 6),

    OIL_SHOCK("Refinery fire",
            "A refinery went up overseas. Wholesale prices jumped.",
            1.00, 0.14, 0, 0, 0, false, 8),

    OIL_GLUT("Supply glut",
            "Storage is overflowing at the terminal. Wholesale prices dropped.",
            1.00, -0.11, 0, 0, 0, false, 8),

    FOOTBALL_MATCH("Cup final",
            "Thousands are driving to the stadium and most of them are low on fuel.",
            1.45, 0.00, 0, 0, 0, false, 9),

    HOLIDAY_RUSH("Long weekend",
            "Everyone is heading out of town at once.",
            1.35, 0.00, 0, 0, 0, false, 10),

    GOOD_PRESS("Good press",
            "The local paper called you the friendliest forecourt in the county.",
            1.22, 0.00, 0, 0, 0, false, 7),

    HAULAGE_CONTRACT("Haulage contract",
            "A delivery firm signed up its vans with you for the day.",
            1.28, 0.00, 0, 0, 0, false, 6);

    private final String title;
    private final String description;
    private final double demandMultiplier;
    private final double wholesaleShift;
    private final int immediateCost;
    private final int pumpsOffline;
    private final int fuelLostLitres;
    private final boolean blocksDelivery;
    private final int weight;

    RandomEvent(String title, String description, double demandMultiplier, double wholesaleShift,
                int immediateCost, int pumpsOffline, int fuelLostLitres, boolean blocksDelivery, int weight) {
        this.title = title;
        this.description = description;
        this.demandMultiplier = demandMultiplier;
        this.wholesaleShift = wholesaleShift;
        this.immediateCost = immediateCost;
        this.pumpsOffline = pumpsOffline;
        this.fuelLostLitres = fuelLostLitres;
        this.blocksDelivery = blocksDelivery;
        this.weight = weight;
    }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    /** How this event scales the day's customer count. */
    public double getDemandMultiplier() { return demandMultiplier; }

    /** How it moves the wholesale price, in dollars per litre. */
    public double getWholesaleShift() { return wholesaleShift; }

    /** A bill you have to pay today. */
    public BigDecimal getImmediateCost() { return Money.cash(immediateCost); }

    /** Pumps out of action for the day. */
    public int getPumpsOffline() { return pumpsOffline; }

    /** Fuel lost out of the tank. */
    public int getFuelLostLitres() { return fuelLostLitres; }

    /** True if no delivery can reach you today. */
    public boolean blocksDelivery() { return blocksDelivery; }

    /** True if this event is, on the whole, good news. */
    public boolean isGoodNews() {
        return demandMultiplier > 1.0 || wholesaleShift < 0;
    }

    /**
     * Rolls for an event.
     *
     * @param chance probability that anything happens at all, 0.0 .. 1.0
     * @return the event, or {@code null} for an uneventful day
     */
    public static RandomEvent roll(Random random, double chance) {
        if (random.nextDouble() >= chance) {
            return null;
        }
        int total = 0;
        for (RandomEvent e : values()) {
            total += e.weight;
        }
        int pick = random.nextInt(total);
        for (RandomEvent e : values()) {
            pick -= e.weight;
            if (pick < 0) {
                return e;
            }
        }
        return null; // unreachable
    }

    @Override
    public String toString() {
        return title;
    }
}
