package gasstation.game;

import gasstation.GasStation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The game. You have inherited a two-pump forecourt and a loan you did not ask
 * for. Thirty days to turn it into something worth having.
 *
 * <p>Each morning you see the news, set your pump price, decide how much fuel
 * to buy at today's wholesale, and choose whether to spend on the place. Then
 * you open up and find out how many people liked your price.
 *
 * <pre>
 *   javac gasstation/*.java gasstation/game/*.java
 *   java gasstation.game.GasStationTycoon
 *   java gasstation.game.GasStationTycoon --hard --seed=42 --days=45
 * </pre>
 */
public class GasStationTycoon {

    private final GameConfig config;
    private final ConsoleIO io;
    private final GasStation station;
    private final Business business;
    private final Market market;
    private final DaySimulator simulator;
    private final List<DayResult> history = new ArrayList<>();
    private final long seed;

    private boolean quit;

    public GasStationTycoon(GameConfig config, ConsoleIO io) {
        this.config = config;
        this.io = io;
        this.seed = config.getSeed() != 0 ? config.getSeed() : new Random().nextLong();
        Random random = new Random(seed);

        this.station = new GasStation(config.getStationName(),
                config.getTankCapacityLitres(),
                config.getStartingFuelLitres(),
                config.getStartingPumpPrice(),
                config.getStartingPumps());
        // You are the owner now: no automatic deliveries, no safety net.
        this.station.setDeliveriesAvailable(false);
        this.station.setReserveFraction(0.0);

        this.business = new Business(config.getStartingCash(), config.getStartingDebt());
        this.market = new Market(config, random);
        this.simulator = new DaySimulator(config, market, random);
    }

    // ------------------------------------------------------------------
    // The main loop
    // ------------------------------------------------------------------

    public void play() {
        printIntro();

        for (int day = 1; day <= config.getDays() && !quit; day++) {
            business.beginDay();
            DayOutlook outlook = simulator.dawn(day);

            printDashboard(outlook);
            morning(outlook);
            if (quit) {
                break;
            }

            DayResult result = simulator.runDay(outlook, station, business);
            history.add(result);
            printDayReport(result);

            if (business.isBankrupt()) {
                printBankruptEnding(result);
                return;
            }
            if (day < config.getDays() && !io.isClosed()) {
                io.pause();
            }
        }

        if (quit) {
            io.println("You hand back the keys. The forecourt stands empty.");
            return;
        }
        printFinalEnding();
    }

    /** The morning phase: everything you can do before opening the pumps. */
    private void morning(DayOutlook outlook) {
        while (!quit) {
            io.println();
            io.println("  [1] Set pump price       (now $" + station.getPricePerLitre() + "/L)");
            io.printf("  [2] Order fuel           (wholesale $%s/L + %s delivery)%n",
                    market.getWholesalePerLitre(), Money.format(config.getDeliveryFee()));
            io.println("  [3] Upgrades");
            io.println("  [4] Repay the loan       (owe " + Money.format(business.getDebt()) + ")");
            io.println("  [5] How this works");
            io.println("  [6] OPEN FOR BUSINESS");
            io.println("  [0] Give up");

            int choice = io.askInt("  > ", 0, 6, 6);
            if (io.isClosed() && choice == 6) {
                // Input ran out; just trade the day out with what we have.
                return;
            }
            switch (choice) {
                case 1: setPrice(); break;
                case 2: orderFuel(outlook); break;
                case 3: showUpgrades(); break;
                case 4: repayLoan(); break;
                case 5: printHelp(); break;
                case 6: return;
                case 0: quit = true; return;
                default: break;
            }
        }
    }

    // ------------------------------------------------------------------
    // Morning actions
    // ------------------------------------------------------------------

    private void setPrice() {
        io.printf("    The going rate around here is $%s/L. You pay $%s/L wholesale.%n",
                config.getReferencePrice(), market.getWholesalePerLitre());
        double price = io.askDouble("    New price per litre: $", 0.10, 9.99,
                station.getPricePerLitre().doubleValue());
        BigDecimal newPrice = Money.perLitre(price);
        station.setPricePerLitre(newPrice);

        BigDecimal margin = newPrice.subtract(market.getWholesalePerLitre());
        io.printf("    Price set to $%s/L - margin of $%s a litre.%n", newPrice, margin);
        if (margin.signum() <= 0) {
            io.println("    You are selling at a loss. Bold.");
        }
    }

    private void orderFuel(DayOutlook outlook) {
        if (outlook.deliveriesBlocked()) {
            io.println("    No tanker is coming today - " + outlook.getEvent().getTitle() + ".");
            return;
        }
        BigDecimal wholesale = market.getWholesalePerLitre();
        double headroom = station.getTank().getHeadroomLitres();
        double affordable = business.affordableLitres(wholesale, config.getDeliveryFee(), station);

        io.printf("    Tank: %,.0f / %,.0f L - room for %,.0f L.%n",
                station.getTank().getLevelLitres(), station.getTank().getCapacityLitres(), headroom);
        io.printf("    Cash %s. At $%s/L plus %s delivery you can afford %,.0f L.%n",
                Money.format(business.getCash()), wholesale, Money.format(config.getDeliveryFee()), affordable);

        if (affordable < 1) {
            io.println("    You cannot afford a delivery today.");
            return;
        }

        String input = io.ask("    Litres to order (or 'max', blank to skip): ");
        if (input == null || input.isEmpty()) {
            return;
        }
        double litres;
        if (input.equalsIgnoreCase("max")) {
            litres = affordable;
        } else {
            try {
                litres = Double.parseDouble(input.replace(",", "").trim());
            } catch (NumberFormatException e) {
                io.println("    That is not a number.");
                return;
            }
        }
        if (litres <= 0) {
            return;
        }
        if (litres > affordable) {
            io.printf("    You can only manage %,.0f L today.%n", affordable);
            return;
        }

        double delivered = business.buyFuel(litres, wholesale, config.getDeliveryFee(), station);
        BigDecimal bill = Money.cash(wholesale.multiply(BigDecimal.valueOf(delivered)).add(config.getDeliveryFee()));
        io.printf("    %,.0f L delivered for %s. Tank now %,.0f L. Cash %s.%n",
                delivered, Money.format(bill), station.getTank().getLevelLitres(), Money.format(business.getCash()));
        if (business.getCash().compareTo(config.getDailyFixedCosts()) < 0) {
            io.println("    ! That is everything you had. If today goes badly you are finished.");
        }
    }

    private void showUpgrades() {
        Upgrade[] all = Upgrade.values();
        io.println("    Cash: " + Money.format(business.getCash()));
        for (int i = 0; i < all.length; i++) {
            Upgrade upgrade = all[i];
            String owned = business.hasUpgrade(upgrade) ? "  [OWNED]" : "";
            io.printf("    [%d] %-20s %9s  %s%s%n",
                    i + 1, upgrade.getLabel(), Money.format(upgrade.getCost()), upgrade.getDescription(), owned);
        }
        int choice = io.askInt("    Buy which? (0 to go back): ", 0, all.length, 0);
        if (choice == 0) {
            return;
        }
        Upgrade chosen = all[choice - 1];
        if (business.hasUpgrade(chosen)) {
            io.println("    You already have that.");
        } else if (!business.canAfford(chosen.getCost())) {
            io.println("    Not enough cash for that yet.");
        } else if (business.buyUpgrade(chosen, station)) {
            io.printf("    Bought: %s. Cash %s.%n", chosen.getLabel(), Money.format(business.getCash()));
            if (chosen == Upgrade.EXTRA_PUMP) {
                io.printf("    You now have %d pumps - %d fills a day.%n",
                        station.getPumpCount(), station.getPumpCount() * config.getFillsPerPumpPerDay());
            }
            if (chosen == Upgrade.BIGGER_TANK) {
                io.printf("    Storage is now %,.0f L.%n", station.getTank().getCapacityLitres());
            }
        }
    }

    private void repayLoan() {
        if (business.getDebt().signum() <= 0) {
            io.println("    You owe the bank nothing. Enjoy it.");
            return;
        }
        io.printf("    You owe %s, costing %s a day in interest. You have %s.%n",
                Money.format(business.getDebt()),
                Money.format(Money.cash(business.getDebt().doubleValue() * config.getDailyInterestRate())),
                Money.format(business.getCash()));
        double amount = io.askDouble("    Repay how much? $", 0, 1_000_000, 0);
        if (amount <= 0) {
            return;
        }
        BigDecimal repaid = business.repayDebt(Money.cash(amount));
        if (repaid.signum() <= 0) {
            io.println("    Nothing repaid.");
        } else {
            io.printf("    Repaid %s. You now owe %s.%n", Money.format(repaid), Money.format(business.getDebt()));
        }
    }

    // ------------------------------------------------------------------
    // Screens
    // ------------------------------------------------------------------

    private void printIntro() {
        io.println();
        io.println("################################################################");
        io.println("#                    G A S   S T A T I O N                     #");
        io.println("#                        t y c o o n                           #");
        io.println("################################################################");
        io.println();
        io.printf("You have taken over %s: %d pumps, a %,.0f litre tank,%n",
                config.getStationName(), config.getStartingPumps(), config.getTankCapacityLitres());
        io.printf("%s in the till and %s owed to the bank.%n",
                Money.format(config.getStartingCash()), Money.format(config.getStartingDebt()));
        io.println();
        io.printf("You have %d days to be worth %s.%n", config.getDays(), Money.format(config.getTargetNetWorth()));
        io.printf("Go into the red at the end of any day and the bank takes the keys.%n");
        io.printf("(seed %d - pass --seed=%d to replay this exact run)%n", seed, seed);
        io.println();
    }

    private void printHelp() {
        io.println();
        io.println("    HOW THIS WORKS");
        io.println("    --------------");
        io.printf("    Drivers know what fuel costs. Around here the going rate is $%s.%n",
                config.getReferencePrice());
        io.println("    Undercut it and far more cars stop; go over it and they drive on to");
        io.println("    the next forecourt. The catch is that every litre you sell cheap is");
        io.println("    margin you never get back, and your pumps can only serve so many");
        io.printf("    cars a day (%d per pump).%n", config.getFillsPerPumpPerDay());
        io.println();
        io.println("    Wholesale prices drift day to day. Fill the tank when fuel is cheap");
        io.println("    and you are buying tomorrow's margin today - if you have the space.");
        io.println();
        io.printf("    Fixed costs are %s a day whether you sell anything or not, and the%n",
                Money.format(config.getDailyFixedCosts()));
        io.printf("    loan charges %.2f%% a day. Standing still loses.%n",
                config.getDailyInterestRate() * 100);
        io.println();
        io.println("    You are scored on net worth: cash, plus the fuel in the ground,");
        io.println("    minus what you owe.");
    }

    private void printDashboard(DayOutlook outlook) {
        io.println();
        io.println("================================================================");
        String dayName = outlook.getDayOfWeek().toString();
        dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase();
        io.printf("  DAY %d of %d  -  %s, %s%n",
                outlook.getDay(), config.getDays(), dayName, outlook.getWeather().getLabel());
        io.println("================================================================");
        io.printf("  Cash %-14s Debt %-14s Net worth %s%n",
                Money.format(business.getCash()),
                Money.format(business.getDebt()),
                Money.format(business.netWorth(station, market.getWholesalePerLitre())));
        io.printf("  Tank %,.0f / %,.0f L      Pumps %d (%d fills/day)%n",
                station.getTank().getLevelLitres(), station.getTank().getCapacityLitres(),
                station.getPumpCount(), station.getPumpCount() * config.getFillsPerPumpPerDay());
        io.printf("  Your price $%s/L      Wholesale $%s/L      Margin $%s/L%n",
                station.getPricePerLitre(), market.getWholesalePerLitre(),
                station.getPricePerLitre().subtract(market.getWholesalePerLitre()));

        if (outlook.hasEvent()) {
            RandomEvent event = outlook.getEvent();
            io.println();
            io.printf("  %s %s%n", event.isGoodNews() ? "GOOD NEWS:" : "NEWS:", event.getTitle().toUpperCase());
            io.println("  " + event.getDescription());
        }
        if (station.getTank().getLevelLitres() < 600) {
            io.println();
            io.println("  ! The tank is nearly dry. Order fuel or you will turn cars away.");
        }
    }

    private void printDayReport(DayResult r) {
        io.println();
        io.println("  ---------------- CLOSING TIME ----------------");
        io.printf("  %d cars served", r.getServedCustomers());
        if (r.getTotalTurnedAway() > 0) {
            io.printf(", %d turned away (%d queue, %d no fuel)",
                    r.getTotalTurnedAway(), r.getTurnedAwayQueue(), r.getTurnedAwayNoFuel());
        }
        io.println();
        io.printf("  %,.0f litres sold at $%s%n", r.getLitresSold(), r.getPumpPrice());
        io.println();
        io.printf("    Fuel sales      %s%n", Money.format(r.getFuelRevenue(), 12));
        if (r.getShopRevenue().signum() > 0) {
            io.printf("    Shop & extras   %s%n", Money.format(r.getShopRevenue(), 12));
        }
        if (r.getFuelPurchases().signum() > 0) {
            io.printf("    Fuel bought     %s%n", Money.format(r.getFuelPurchases().negate(), 12));
        }
        io.printf("    Running costs   %s%n", Money.format(r.getRunningCosts().negate(), 12));
        if (r.getEventCosts().signum() > 0) {
            io.printf("    One-off bills   %s%n", Money.format(r.getEventCosts().negate(), 12));
        }
        io.println("                    ------------");
        io.printf("    Cash change     %s%n", Money.format(r.getCashChange(), 12));
        io.println();
        io.printf("  Cash %s   Debt %s (+%s interest)   Net worth %s%n",
                Money.format(r.getCashAfter()), Money.format(r.getDebtAfter()),
                Money.format(r.getInterest()), Money.format(r.getNetWorthAfter()));
        io.printf("  Tank %,.0f L left.%n", r.getFuelInTankAfter());
    }

    private void printBankruptEnding(DayResult r) {
        io.println();
        io.println("################################################################");
        io.println("#                       B A N K R U P T                        #");
        io.println("################################################################");
        io.printf("You ended day %d %s in the red. The bank takes the keys.%n",
                r.getDay(), Money.format(r.getCashAfter().abs()));
        printHistory();
    }

    private void printFinalEnding() {
        BigDecimal netWorth = business.netWorth(station, market.getWholesalePerLitre());
        BigDecimal target = config.getTargetNetWorth();
        boolean won = netWorth.compareTo(target) >= 0;

        io.println();
        io.println("################################################################");
        io.println(won ? "#                          W I N                               #"
                       : "#                     T I M E ' S   U P                        #");
        io.println("################################################################");
        io.printf("After %d days: net worth %s against a target of %s.%n",
                config.getDays(), Money.format(netWorth), Money.format(target));
        io.printf("Cash %s, fuel in the ground %,.0f L, still owing %s.%n",
                Money.format(business.getCash()), station.getTank().getLevelLitres(),
                Money.format(business.getDebt()));
        io.println();
        io.println(verdict(netWorth, target, won));
        printHistory();
    }

    private String verdict(BigDecimal netWorth, BigDecimal target, boolean won) {
        double ratio = target.signum() == 0 ? 1 : netWorth.doubleValue() / target.doubleValue();
        if (!won) {
            if (ratio < 0) {
                return "You are worth less than nothing. The pumps kept running; that is all.";
            }
            if (ratio < 0.5) {
                return "A living, barely. Buy cheaper, or charge more - you did neither.";
            }
            return "So close. One better week on price and you would have had it.";
        }
        if (ratio > 2.0) {
            return "You own the road. People drive past two other stations to get here.";
        }
        if (ratio > 1.4) {
            return "A proper business now - bank paid off and money in the bank.";
        }
        return "You made it. Not comfortably, but the keys are yours.";
    }

    private void printHistory() {
        io.println();
        io.println("  ---- THE RUN ----");
        for (DayResult r : history) {
            io.println("  " + r.toLine());
        }
        io.println();
        io.printf("  %,.0f litres sold across %d days for %s.%n",
                station.getTotalLitresSold(), history.size(), Money.format(station.getTotalRevenue()));
        io.printf("  %d deliveries taken, %s paid in interest.%n",
                station.getDeliveryCount(), Money.format(business.getTotalInterestPaid()));
        if (!business.getUpgrades().isEmpty()) {
            io.println("  Bought: " + business.getUpgrades());
        }
        io.printf("  Seed %d - run with --seed=%d to play this run again.%n", seed, seed);
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        GameConfig config = GameConfig.normal();
        for (String arg : args) {
            if (arg.equals("--easy")) {
                config = GameConfig.easy();
            } else if (arg.equals("--hard")) {
                config = GameConfig.hard();
            }
        }
        // Read the value flags after the difficulty, so they win.
        for (String arg : args) {
            try {
                if (arg.startsWith("--seed=")) {
                    config.setSeed(Long.parseLong(arg.substring(7)));
                } else if (arg.startsWith("--days=")) {
                    config.setDays(Integer.parseInt(arg.substring(7)));
                } else if (arg.startsWith("--name=")) {
                    config.setStationName(arg.substring(7));
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    System.out.println("Usage: java gasstation.game.GasStationTycoon "
                            + "[--easy|--hard] [--seed=N] [--days=N] [--name=TEXT]");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ignoring bad argument: " + arg);
            }
        }
        new GasStationTycoon(config, new ConsoleIO()).play();
    }
}
