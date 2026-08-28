package gasstation.game;

import gasstation.GasStation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plays the game many times with no human, to check it is neither a walkover
 * nor impossible.
 *
 * <p>Each strategy is a different answer to the one real question in the game -
 * what do you charge? Running them over the same seeds shows whether being
 * clever beats being simple, which is the test of whether the game has any
 * decisions in it.
 *
 * <pre>
 *   java gasstation.game.BalanceCheck        # normal difficulty, 200 runs each
 *   java gasstation.game.BalanceCheck 500 hard
 * </pre>
 */
public class BalanceCheck {

    /** A way of playing. */
    interface Strategy {
        String name();

        /** Decides the day's price and orders fuel. */
        void takeMorning(GameConfig config, GasStation station, Business business,
                         Market market, DayOutlook outlook, int day);
    }

    /** Buys fuel to a target fullness, if it can afford to and a tanker is coming. */
    private static void topUp(GameConfig config, GasStation station, Business business,
                              Market market, DayOutlook outlook, double targetFraction, BigDecimal cashBuffer) {
        if (outlook.deliveriesBlocked()) {
            return;
        }
        double target = station.getTank().getCapacityLitres() * targetFraction;
        double wanted = target - station.getTank().getLevelLitres();
        if (wanted < 200) {
            return;
        }
        BigDecimal spendable = business.getCash().subtract(cashBuffer).subtract(config.getDeliveryFee());
        if (spendable.signum() <= 0) {
            return;
        }
        double affordable = spendable.doubleValue() / market.getWholesalePerLitre().doubleValue();
        double litres = Math.min(wanted, Math.min(affordable, station.getTank().getHeadroomLitres()));
        if (litres >= 100) {
            business.buyFuel(litres, market.getWholesalePerLitre(), config.getDeliveryFee(), station);
        }
    }

    private static final Strategy CHEAPEST = new Strategy() {
        public String name() { return "Race to the bottom (wholesale + $0.10)"; }

        public void takeMorning(GameConfig config, GasStation station, Business business,
                                Market market, DayOutlook outlook, int day) {
            station.setPricePerLitre(Money.perLitre(market.getWholesalePerLitre().doubleValue() + 0.10));
            topUp(config, station, business, market, outlook, 0.95, Money.cash(300));
        }
    };

    private static final Strategy MARKET_RATE = new Strategy() {
        public String name() { return "Match the going rate"; }

        public void takeMorning(GameConfig config, GasStation station, Business business,
                                Market market, DayOutlook outlook, int day) {
            station.setPricePerLitre(config.getReferencePrice());
            topUp(config, station, business, market, outlook, 0.85, Money.cash(500));
        }
    };

    private static final Strategy GREEDY = new Strategy() {
        public String name() { return "Charge what you like (rate + $0.25)"; }

        public void takeMorning(GameConfig config, GasStation station, Business business,
                                Market market, DayOutlook outlook, int day) {
            station.setPricePerLitre(Money.perLitre(config.getReferencePrice().doubleValue() + 0.25));
            topUp(config, station, business, market, outlook, 0.85, Money.cash(500));
        }
    };

    private static final Strategy ADAPTIVE = new Strategy() {
        public String name() { return "Play it properly (margin floor, buy the dips, upgrade)"; }

        public void takeMorning(GameConfig config, GasStation station, Business business,
                                Market market, DayOutlook outlook, int day) {
            double wholesale = market.getWholesalePerLitre().doubleValue();
            double reference = config.getReferencePrice().doubleValue();

            // With only two pumps you cannot serve a crowd, so take the margin and
            // let the cheap-seekers drive on. With a third pump, volume pays.
            boolean roomToGrow = station.getPumpCount() > config.getStartingPumps();
            double markup = roomToGrow ? 0.032 : 0.095;
            double price = reference + markup + (0.30 * (wholesale - config.getWholesaleMean()));
            // On a busy day people are less fussy, so take the margin.
            if (outlook.hasEvent() && outlook.getEvent().getDemandMultiplier() > 1.2) {
                price += 0.06;
            }
            // On a dead day, buy the volume back.
            if (outlook.getWeather() == Weather.STORM) {
                price -= 0.03;
            }
            station.setPricePerLitre(Money.perLitre(Math.max(reference - 0.03, Math.min(reference + 0.18, price))));

            // Stock up hard when fuel is cheap, top up lightly when it is not.
            boolean cheap = wholesale < config.getWholesaleMean() - 0.02;
            topUp(config, station, business, market, outlook, cheap ? 1.00 : 0.70, Money.cash(cheap ? 400 : 1_200));

            // Spend on the place once there is comfortable cash behind it.
            for (Upgrade upgrade : new Upgrade[]{
                    Upgrade.EXTRA_PUMP, Upgrade.CONVENIENCE_STORE, Upgrade.CANOPY,
                    Upgrade.CAR_WASH, Upgrade.LOYALTY_APP, Upgrade.BIGGER_TANK}) {
                if (business.hasUpgrade(upgrade)) {
                    continue;
                }
                BigDecimal needed = upgrade.getCost().add(Money.cash(3_000));
                if (day < config.getDays() - 6 && business.getCash().compareTo(needed) >= 0) {
                    business.buyUpgrade(upgrade, station);
                }
                break; // one at a time, in payback order
            }
        }
    };

    /** Holds a fixed price all game - used to sweep the price curve. */
    static Strategy fixedPrice(final double price, final boolean buyPump) {
        return new Strategy() {
            public String name() { return String.format("Flat $%.2f", price); }

            public void takeMorning(GameConfig config, GasStation station, Business business,
                                    Market market, DayOutlook outlook, int day) {
                station.setPricePerLitre(Money.perLitre(price));
                if (buyPump && day == 2) {
                    business.buyUpgrade(Upgrade.EXTRA_PUMP, station);
                }
                topUp(config, station, business, market, outlook, 0.85, Money.cash(400));
            }
        };
    }

    /** The outcome of one full playthrough. */
    static class RunResult {
        final BigDecimal netWorth;
        final boolean bankrupt;
        final boolean won;
        final int daysSurvived;

        RunResult(BigDecimal netWorth, boolean bankrupt, boolean won, int daysSurvived) {
            this.netWorth = netWorth;
            this.bankrupt = bankrupt;
            this.won = won;
            this.daysSurvived = daysSurvived;
        }
    }

    /** Plays one full game with no input at all. */
    static RunResult playOnce(GameConfig config, Strategy strategy, long seed) {
        Random random = new Random(seed);
        GasStation station = new GasStation(config.getStationName(), config.getTankCapacityLitres(),
                config.getStartingFuelLitres(), config.getStartingPumpPrice(), config.getStartingPumps());
        station.setDeliveriesAvailable(false);
        station.setReserveFraction(0.0);

        Business business = new Business(config.getStartingCash(), config.getStartingDebt());
        Market market = new Market(config, random);
        DaySimulator simulator = new DaySimulator(config, market, random);

        for (int day = 1; day <= config.getDays(); day++) {
            business.beginDay();
            DayOutlook outlook = simulator.dawn(day);
            strategy.takeMorning(config, station, business, market, outlook, day);
            simulator.runDay(outlook, station, business);
            if (business.isBankrupt()) {
                return new RunResult(business.netWorth(station, market.getWholesalePerLitre()), true, false, day);
            }
        }
        BigDecimal netWorth = business.netWorth(station, market.getWholesalePerLitre());
        boolean won = netWorth.compareTo(config.getTargetNetWorth()) >= 0;
        return new RunResult(netWorth, false, won, config.getDays());
    }

    /**
     * Prints median net worth against a flat pump price, with and without a
     * third pump. A game worth playing has a peak somewhere in the middle and a
     * visible reason to buy the pump.
     */
    static void sweep(String label, int runs) {
        System.out.printf("PRICE SWEEP (%s, %d runs each)%n%n", label, runs);
        System.out.printf("%8s %16s %16s%n", "PRICE", "2 PUMPS", "3 PUMPS");
        for (double price = 1.38; price <= 1.86001; price += 0.03) {
            BigDecimal two = medianOf(label, fixedPrice(price, false), runs);
            BigDecimal three = medianOf(label, fixedPrice(price, true), runs);
            System.out.printf("   $%.2f %16s %16s%n", price, Money.format(two), Money.format(three));
        }
        System.out.println();
    }

    private static BigDecimal medianOf(String label, Strategy strategy, int runs) {
        List<BigDecimal> results = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            results.add(playOnce(copyOf(label), strategy, 1_000L + i).netWorth);
        }
        results.sort(BigDecimal::compareTo);
        return results.get(results.size() / 2);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("sweep")) {
            sweep(args.length > 1 ? args[1] : "normal", args.length > 2 ? Integer.parseInt(args[2]) : 120);
            return;
        }
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        GameConfig template = GameConfig.normal();
        String label = "normal";
        if (args.length > 1) {
            label = args[1];
            if (label.equals("easy")) {
                template = GameConfig.easy();
            } else if (label.equals("hard")) {
                template = GameConfig.hard();
            }
        }

        System.out.printf("%d runs per strategy, %s difficulty, target %s in %d days%n%n",
                runs, label, Money.format(template.getTargetNetWorth()), template.getDays());
        System.out.printf("%-52s %8s %8s %14s %14s %14s%n",
                "STRATEGY", "WIN%", "BUST%", "MEDIAN", "WORST", "BEST");

        Strategy[] strategies = {CHEAPEST, MARKET_RATE, GREEDY, ADAPTIVE};
        for (Strategy strategy : strategies) {
            List<BigDecimal> results = new ArrayList<>();
            int wins = 0;
            int busts = 0;
            for (int i = 0; i < runs; i++) {
                GameConfig config = copyOf(label);
                RunResult result = playOnce(config, strategy, 1_000L + i);
                results.add(result.netWorth);
                if (result.won) {
                    wins++;
                }
                if (result.bankrupt) {
                    busts++;
                }
            }
            results.sort(BigDecimal::compareTo);
            System.out.printf("%-52s %7.1f%% %7.1f%% %14s %14s %14s%n",
                    strategy.name(),
                    100.0 * wins / runs,
                    100.0 * busts / runs,
                    Money.format(results.get(results.size() / 2)),
                    Money.format(results.get(0)),
                    Money.format(results.get(results.size() - 1)));
        }
        System.out.println();
        System.out.println("A healthy balance: the naive strategies mostly lose, playing well mostly wins,");
        System.out.println("and no strategy wins every time.");
    }

    private static GameConfig copyOf(String label) {
        if (label.equals("easy")) {
            return GameConfig.easy();
        }
        if (label.equals("hard")) {
            return GameConfig.hard();
        }
        return GameConfig.normal();
    }
}
