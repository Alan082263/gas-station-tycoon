package gasstation.game;

import gasstation.Customer;
import gasstation.GasStation;
import gasstation.NoPumpAvailableException;
import gasstation.OutOfFuelException;
import gasstation.Transaction;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Random;

/**
 * Runs one trading day. Pure logic and no input or output, so the same code
 * drives the console game and the automated playtests in {@link BalanceCheck}.
 *
 * <p>Customers are real {@link Customer} objects served through
 * {@link GasStation#serve(Customer)}, so every sale produces a real
 * {@link Transaction} and draws real litres out of the tank. The game does not
 * re-implement the station - it plays it.
 *
 * <p>A day has two halves. {@link #dawn(int)} rolls the weather and the day's
 * event so the player can react to the news; {@link #runDay} then trades.
 */
public class DaySimulator {

    private final GameConfig config;
    private final Market market;
    private final Random random;

    public DaySimulator(GameConfig config, Market market, Random random) {
        this.config = config;
        this.market = market;
        this.random = random;
    }

    /**
     * Rolls the day's weather and event, and applies any shock to the wholesale
     * price straight away - the news reaches the depot before it reaches you.
     */
    public DayOutlook dawn(int dayNumber) {
        DayOfWeek dayOfWeek = Market.dayOfWeek(dayNumber);
        Weather weather = Weather.roll(random);
        RandomEvent event = RandomEvent.roll(random, config.getEventChance());
        if (event != null) {
            market.shiftWholesale(event.getWholesaleShift());
        }
        return new DayOutlook(dayNumber, dayOfWeek, weather, event);
    }

    /**
     * Trades for the day and settles the money.
     *
     * <p>Fuel ordered this morning has already been paid for; this adds the
     * day's takings and subtracts running costs, event bills and loan interest.
     */
    public DayResult runDay(DayOutlook outlook, GasStation station, Business business) {
        DayResult result = new DayResult(outlook.getDay(), outlook.getDayOfWeek());
        RandomEvent event = outlook.getEvent();

        result.setWeather(outlook.getWeather());
        result.setEvent(event);
        result.setPumpPrice(station.getPricePerLitre());
        result.setWholesalePrice(market.getWholesalePerLitre());
        result.setFuelPurchases(business.getFuelSpendToday());

        // --- what the event does to you today
        double eventDemand = 1.0;
        int pumpsOffline = 0;
        BigDecimal eventCost = Money.ZERO;
        if (event != null) {
            eventDemand = event.getDemandMultiplier();
            pumpsOffline = event.getPumpsOffline();
            eventCost = event.getImmediateCost();
            if (event.getFuelLostLitres() > 0) {
                station.recordFuelLoss(event.getFuelLostLitres());
            }
        }
        result.setEventCosts(eventCost);

        // --- how many people want fuel at your price
        double sensitivity = Math.max(4.0, config.getPriceSensitivity() - business.getSensitivityReduction());
        double weatherFactor = outlook.getWeather().demandMultiplier(business.hasUpgrade(Upgrade.CANOPY));
        int potential = market.potentialCustomers(station.getPricePerLitre(), weatherFactor,
                outlook.getDayOfWeek(), eventDemand, business.getDemandBonus(), sensitivity);
        result.setPotentialCustomers(potential);

        // --- how many of them you can physically get through the forecourt
        int workingPumps = Math.max(0, station.getPumpCount() - pumpsOffline);
        int capacity = workingPumps * config.getFillsPerPumpPerDay();
        int queueTurnedAway = Math.max(0, potential - capacity);
        int attempts = Math.min(potential, capacity);

        // --- serve them, one at a time, through the real pumps
        int served = 0;
        int noFuel = 0;
        double litresSold = 0;
        BigDecimal fuelRevenue = Money.ZERO;

        for (int i = 0; i < attempts; i++) {
            Customer customer = new Customer("Car " + (i + 1), market.nextFillLitres());
            try {
                Transaction sale = station.serve(customer);
                served++;
                litresSold += sale.getLitres();
                fuelRevenue = fuelRevenue.add(sale.getTotalPrice());
            } catch (OutOfFuelException e) {
                noFuel++;
                if (station.getTank().isEmpty()) {
                    noFuel += attempts - i - 1; // nothing left for anyone behind them
                    break;
                }
            } catch (NoPumpAvailableException e) {
                // Cannot happen here: serve() frees the pump before returning.
                queueTurnedAway++;
            }
        }

        result.setServedCustomers(served);
        result.setTurnedAwayNoFuel(noFuel);
        result.setTurnedAwayQueue(queueTurnedAway);
        result.setLitresSold(Math.round(litresSold * 100.0) / 100.0);
        result.setFuelRevenue(Money.cash(fuelRevenue));

        // --- shop, car wash and the like
        BigDecimal shopRevenue = Money.cash(business.getPerCustomerIncome() * served);
        result.setShopRevenue(shopRevenue);

        // --- settle up
        BigDecimal interest = business.accrueInterest(config.getDailyInterestRate());
        BigDecimal running = config.getDailyFixedCosts();
        result.setInterest(interest);
        result.setRunningCosts(running);

        business.receive(fuelRevenue.add(shopRevenue));
        business.pay(running.add(eventCost));

        BigDecimal cashChange = Money.cash(fuelRevenue.add(shopRevenue)
                .subtract(running)
                .subtract(eventCost)
                .subtract(business.getFuelSpendToday()));
        result.setCashChange(cashChange);
        result.setCashAfter(business.getCash());
        result.setDebtAfter(business.getDebt());
        result.setFuelInTankAfter(station.getTank().getLevelLitres());

        // --- the market drifts overnight
        market.advance();
        result.setNetWorthAfter(business.netWorth(station, market.getWholesalePerLitre()));

        return result;
    }
}
