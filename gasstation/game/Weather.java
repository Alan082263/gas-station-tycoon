package gasstation.game;

import java.util.Random;

/**
 * The day's weather, which nudges how many people stop for fuel.
 *
 * <p>A forecourt canopy softens the bad days - see {@link #demandMultiplier(boolean)}.
 */
public enum Weather {

    HEATWAVE("Heatwave", 1.16, 8),
    SUNNY("Sunny", 1.06, 26),
    CLOUDY("Cloudy", 1.00, 28),
    FOG("Fog", 0.92, 10),
    RAIN("Rain", 0.86, 20),
    STORM("Storm", 0.68, 8);

    private final String label;
    private final double multiplier;
    private final int weight;

    Weather(String label, double multiplier, int weight) {
        this.label = label;
        this.multiplier = multiplier;
        this.weight = weight;
    }

    public String getLabel() {
        return label;
    }

    /**
     * @param hasCanopy whether the station has a forecourt canopy
     * @return the demand multiplier; a canopy halves any penalty
     */
    public double demandMultiplier(boolean hasCanopy) {
        if (hasCanopy && multiplier < 1.0) {
            return 1.0 - ((1.0 - multiplier) / 2.0);
        }
        return multiplier;
    }

    /** Picks a day's weather, weighted so cloudy days are common and storms rare. */
    public static Weather roll(Random random) {
        int total = 0;
        for (Weather w : values()) {
            total += w.weight;
        }
        int pick = random.nextInt(total);
        for (Weather w : values()) {
            pick -= w.weight;
            if (pick < 0) {
                return w;
            }
        }
        return CLOUDY; // unreachable
    }

    @Override
    public String toString() {
        return label;
    }
}
