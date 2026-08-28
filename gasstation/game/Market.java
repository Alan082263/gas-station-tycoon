package gasstation.game;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Random;

/**
 * The world outside the forecourt: what fuel costs you, and how many people
 * want it at the price you are asking.
 *
 * <p><b>Demand.</b> A fixed stream of cars passes the door every day, and each
 * driver decides between you and the station down the road. The share you win
 * follows an S-curve around the going rate: price at the going rate and you
 * take about half of them, undercut it and your share climbs quickly, go over
 * it and it falls away just as fast. That is the whole game - margin per litre
 * against litres sold, with your pumps capping how many cars you can physically
 * serve however cheap you go.
 *
 * <p><b>Wholesale.</b> The price you pay follows a mean-reverting random walk,
 * so it drifts, spikes and comes back. Buying cheap and holding stock is a real
 * strategy - if you have the tank space and the cash.
 */
public class Market {

    private final GameConfig config;
    private final Random random;

    private BigDecimal wholesalePerLitre;

    public Market(GameConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.wholesalePerLitre = Money.perLitre(config.getStartingWholesale());
    }

    public BigDecimal getWholesalePerLitre() {
        return wholesalePerLitre;
    }

    /**
     * Moves tomorrow's wholesale price: a random step, pulled gently back
     * towards the long-run mean, and clamped to a sane range.
     */
    public void advance() {
        double current = wholesalePerLitre.doubleValue();
        double drift = config.getWholesaleReversion() * (config.getWholesaleMean() - current);
        double shock = config.getWholesaleVolatility() * random.nextGaussian();
        setWholesale(current + drift + shock);
    }

    /** Applies an event's shock to the wholesale price. */
    public void shiftWholesale(double delta) {
        if (delta != 0) {
            setWholesale(wholesalePerLitre.doubleValue() + delta);
        }
    }

    private void setWholesale(double value) {
        double clamped = Math.max(config.getWholesaleFloor(), Math.min(config.getWholesaleCeiling(), value));
        this.wholesalePerLitre = Money.perLitre(clamped);
    }

    /**
     * How many cars would stop today.
     *
     * @param pumpPrice        what you are charging per litre
     * @param weatherFactor    the weather's demand multiplier
     * @param dayOfWeek        which day it is
     * @param eventFactor      an event's demand multiplier (1.0 if nothing happened)
     * @param upgradeBonus     extra demand from upgrades, as a fraction
     * @param sensitivity      how sharply drivers react to being over or under the going rate
     */
    public int potentialCustomers(BigDecimal pumpPrice, double weatherFactor, DayOfWeek dayOfWeek,
                                  double eventFactor, double upgradeBonus, double sensitivity) {
        double share = marketShare(pumpPrice, sensitivity);
        double noise = 0.90 + (0.20 * random.nextDouble());
        double demand = config.getPassingTraffic()
                * share
                * weatherFactor
                * dayFactor(dayOfWeek)
                * eventFactor
                * (1.0 + upgradeBonus)
                * noise;
        return (int) Math.max(0, Math.round(demand));
    }

    /**
     * The fraction of passing drivers who choose you at this price.
     *
     * <p>A logistic curve centred on the going rate: half of them at the going
     * rate, and a steep climb or fall either side of it.
     */
    public double marketShare(BigDecimal pumpPrice, double sensitivity) {
        double reference = config.getReferencePrice().doubleValue();
        double price = Math.max(0.001, pumpPrice.doubleValue());
        double relative = (price - reference) / reference;
        return 1.0 / (1.0 + Math.exp(sensitivity * relative));
    }

    /** How big a tank the next customer wants filling. */
    public double nextFillLitres() {
        double span = config.getMaxFillLitres() - config.getMinFillLitres();
        double litres = config.getMinFillLitres() + (random.nextDouble() * span);
        return Math.round(litres * 100.0) / 100.0;
    }

    /** Fridays and Saturdays are busy; Sundays are dead. */
    public static double dayFactor(DayOfWeek day) {
        switch (day) {
            case MONDAY:    return 0.95;
            case TUESDAY:   return 0.93;
            case WEDNESDAY: return 1.00;
            case THURSDAY:  return 1.06;
            case FRIDAY:    return 1.28;
            case SATURDAY:  return 1.16;
            case SUNDAY:    return 0.82;
            default:        return 1.00;
        }
    }

    /** Day 1 of the game is a Monday. */
    public static DayOfWeek dayOfWeek(int dayNumber) {
        return DayOfWeek.of(((dayNumber - 1) % 7) + 1);
    }
}
