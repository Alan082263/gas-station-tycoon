package gasstation.game;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * What happened on one day of trading.
 *
 * <p>Built by {@link DaySimulator} - the setters are package-private, so once
 * a result reaches the rest of the program it is read-only.
 */
public class DayResult {

    private int day;
    private DayOfWeek dayOfWeek;
    private Weather weather;
    private RandomEvent event;

    private BigDecimal pumpPrice = Money.ZERO;
    private BigDecimal wholesalePrice = Money.ZERO;

    private int potentialCustomers;
    private int servedCustomers;
    private int turnedAwayNoFuel;
    private int turnedAwayQueue;
    private double litresSold;

    private BigDecimal fuelRevenue = Money.ZERO;
    private BigDecimal shopRevenue = Money.ZERO;
    private BigDecimal fuelPurchases = Money.ZERO;
    private BigDecimal runningCosts = Money.ZERO;
    private BigDecimal eventCosts = Money.ZERO;
    private BigDecimal interest = Money.ZERO;

    private BigDecimal cashChange = Money.ZERO;
    private BigDecimal cashAfter = Money.ZERO;
    private BigDecimal debtAfter = Money.ZERO;
    private BigDecimal netWorthAfter = Money.ZERO;
    private double fuelInTankAfter;

    DayResult(int day, DayOfWeek dayOfWeek) {
        this.day = day;
        this.dayOfWeek = dayOfWeek;
    }

    public int getDay() { return day; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public Weather getWeather() { return weather; }

    /** The day's event, or {@code null} if nothing happened. */
    public RandomEvent getEvent() { return event; }

    public BigDecimal getPumpPrice() { return pumpPrice; }
    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public int getPotentialCustomers() { return potentialCustomers; }
    public int getServedCustomers() { return servedCustomers; }

    /** Customers refused because the tank could not cover their fill. */
    public int getTurnedAwayNoFuel() { return turnedAwayNoFuel; }

    /** Customers who saw the queue and drove on - you ran out of pump time. */
    public int getTurnedAwayQueue() { return turnedAwayQueue; }

    public int getTotalTurnedAway() { return turnedAwayNoFuel + turnedAwayQueue; }

    public double getLitresSold() { return litresSold; }
    public BigDecimal getFuelRevenue() { return fuelRevenue; }
    public BigDecimal getShopRevenue() { return shopRevenue; }
    public BigDecimal getTotalRevenue() { return Money.cash(fuelRevenue.add(shopRevenue)); }
    public BigDecimal getFuelPurchases() { return fuelPurchases; }
    public BigDecimal getRunningCosts() { return runningCosts; }
    public BigDecimal getEventCosts() { return eventCosts; }
    public BigDecimal getInterest() { return interest; }
    public BigDecimal getCashChange() { return cashChange; }
    public BigDecimal getCashAfter() { return cashAfter; }
    public BigDecimal getDebtAfter() { return debtAfter; }
    public BigDecimal getNetWorthAfter() { return netWorthAfter; }
    public double getFuelInTankAfter() { return fuelInTankAfter; }

    /** Gross margin on the fuel actually sold today, at today's wholesale price. */
    public BigDecimal getFuelMargin() {
        BigDecimal costOfSales = Money.cash(wholesalePrice.multiply(BigDecimal.valueOf(litresSold)));
        return Money.cash(fuelRevenue.subtract(costOfSales));
    }

    // --- package-private setters, used while the day is being simulated

    void setWeather(Weather weather) { this.weather = weather; }
    void setEvent(RandomEvent event) { this.event = event; }
    void setPumpPrice(BigDecimal v) { this.pumpPrice = v; }
    void setWholesalePrice(BigDecimal v) { this.wholesalePrice = v; }
    void setPotentialCustomers(int v) { this.potentialCustomers = v; }
    void setServedCustomers(int v) { this.servedCustomers = v; }
    void setTurnedAwayNoFuel(int v) { this.turnedAwayNoFuel = v; }
    void setTurnedAwayQueue(int v) { this.turnedAwayQueue = v; }
    void setLitresSold(double v) { this.litresSold = v; }
    void setFuelRevenue(BigDecimal v) { this.fuelRevenue = v; }
    void setShopRevenue(BigDecimal v) { this.shopRevenue = v; }
    void setFuelPurchases(BigDecimal v) { this.fuelPurchases = v; }
    void setRunningCosts(BigDecimal v) { this.runningCosts = v; }
    void setEventCosts(BigDecimal v) { this.eventCosts = v; }
    void setInterest(BigDecimal v) { this.interest = v; }
    void setCashChange(BigDecimal v) { this.cashChange = v; }
    void setCashAfter(BigDecimal v) { this.cashAfter = v; }
    void setDebtAfter(BigDecimal v) { this.debtAfter = v; }
    void setNetWorthAfter(BigDecimal v) { this.netWorthAfter = v; }
    void setFuelInTankAfter(double v) { this.fuelInTankAfter = v; }

    /** A one-line summary, for the history table at the end of the game. */
    public String toLine() {
        return String.format("Day %2d %-3s  $%s  %3d cars  %7.0f L  rev %10s  cash %11s  net %11s%s",
                day,
                dayOfWeek.toString().substring(0, 3),
                pumpPrice,
                servedCustomers,
                litresSold,
                Money.format(getTotalRevenue()),
                Money.format(cashAfter),
                Money.format(netWorthAfter),
                event == null ? "" : "  (" + event.getTitle() + ")");
    }
}
