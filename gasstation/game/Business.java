package gasstation.game;

import gasstation.GasStation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The books: cash in hand, what you owe the bank, and what you have bought.
 *
 * <p>The {@link gasstation.GasStation} knows about fuel; this class knows about
 * money. Keeping them apart is what lets the same station model be used for a
 * plain simulation or for the game.
 */
public class Business {

    private BigDecimal cash;
    private BigDecimal debt;
    private final EnumSet<Upgrade> upgrades = EnumSet.noneOf(Upgrade.class);

    // reset at the start of each day
    private BigDecimal fuelSpendToday = Money.ZERO;
    private double litresBoughtToday;

    private BigDecimal totalInterestPaid = Money.ZERO;

    public Business(BigDecimal startingCash, BigDecimal startingDebt) {
        this.cash = Money.cash(startingCash);
        this.debt = Money.cash(startingDebt);
    }

    // ------------------------------------------------------------------
    // Cash
    // ------------------------------------------------------------------

    public BigDecimal getCash() {
        return cash;
    }

    public BigDecimal getDebt() {
        return debt;
    }

    public boolean canAfford(BigDecimal amount) {
        return cash.compareTo(amount) >= 0;
    }

    public void receive(BigDecimal amount) {
        cash = Money.cash(cash.add(amount));
    }

    public void pay(BigDecimal amount) {
        cash = Money.cash(cash.subtract(amount));
    }

    /** True when the day ended with the till in the red. */
    public boolean isBankrupt() {
        return cash.signum() < 0;
    }

    // ------------------------------------------------------------------
    // The loan
    // ------------------------------------------------------------------

    /**
     * Adds a day's interest to the outstanding loan.
     *
     * @return the interest charged
     */
    public BigDecimal accrueInterest(double dailyRate) {
        if (debt.signum() <= 0) {
            return Money.ZERO;
        }
        BigDecimal interest = Money.cash(debt.multiply(BigDecimal.valueOf(dailyRate)));
        debt = Money.cash(debt.add(interest));
        totalInterestPaid = Money.cash(totalInterestPaid.add(interest));
        return interest;
    }

    public BigDecimal getTotalInterestPaid() {
        return totalInterestPaid;
    }

    /**
     * Pays down the loan from cash.
     *
     * @return the amount actually repaid (capped by cash and by what is owed)
     */
    public BigDecimal repayDebt(BigDecimal amount) {
        BigDecimal repayment = amount.min(cash).min(debt);
        if (repayment.signum() <= 0) {
            return Money.ZERO;
        }
        pay(repayment);
        debt = Money.cash(debt.subtract(repayment));
        return repayment;
    }

    // ------------------------------------------------------------------
    // Buying things
    // ------------------------------------------------------------------

    /** Clears the day's purchase counters. Called at the start of each day. */
    public void beginDay() {
        fuelSpendToday = Money.ZERO;
        litresBoughtToday = 0;
    }

    public BigDecimal getFuelSpendToday() {
        return fuelSpendToday;
    }

    public double getLitresBoughtToday() {
        return litresBoughtToday;
    }

    /**
     * Orders fuel and pays for it, delivery fee included.
     *
     * @return the litres the tank actually accepted
     * @throws IllegalStateException if there is not enough cash
     */
    public double buyFuel(double litres, BigDecimal wholesalePerLitre, BigDecimal deliveryFee, GasStation station) {
        if (litres <= 0) {
            throw new IllegalArgumentException("Order must be more than 0 litres, was " + litres);
        }
        double accepted = Math.min(litres, station.getTank().getHeadroomLitres());
        if (accepted <= 0) {
            return 0;
        }
        BigDecimal bill = Money.cash(wholesalePerLitre.multiply(BigDecimal.valueOf(accepted)).add(deliveryFee));
        if (!canAfford(bill)) {
            throw new IllegalStateException("Cannot afford " + Money.format(bill) + "; cash is " + Money.format(cash));
        }
        station.takeDelivery(accepted);
        pay(bill);
        fuelSpendToday = Money.cash(fuelSpendToday.add(bill));
        litresBoughtToday += accepted;
        return accepted;
    }

    /**
     * The most fuel you could order right now, given cash, tank space and the
     * delivery fee.
     */
    public double affordableLitres(BigDecimal wholesalePerLitre, BigDecimal deliveryFee, GasStation station) {
        BigDecimal spendable = cash.subtract(deliveryFee);
        if (spendable.signum() <= 0) {
            return 0;
        }
        double byCash = spendable.divide(wholesalePerLitre, 2, RoundingMode.DOWN).doubleValue();
        return Math.min(byCash, station.getTank().getHeadroomLitres());
    }

    /**
     * Buys an upgrade if it is affordable and not already owned.
     *
     * @return true if the purchase went through
     */
    public boolean buyUpgrade(Upgrade upgrade, GasStation station) {
        if (upgrades.contains(upgrade) || !canAfford(upgrade.getCost())) {
            return false;
        }
        pay(upgrade.getCost());
        upgrades.add(upgrade);
        upgrade.install(station);
        return true;
    }

    public boolean hasUpgrade(Upgrade upgrade) {
        return upgrades.contains(upgrade);
    }

    public Set<Upgrade> getUpgrades() {
        return Collections.unmodifiableSet(upgrades);
    }

    /** Total extra income earned per customer from shop, car wash and so on. */
    public double getPerCustomerIncome() {
        double total = 0;
        for (Upgrade upgrade : upgrades) {
            total += upgrade.getPerCustomerIncome();
        }
        return total;
    }

    /** Total demand bonus from upgrades, as a fraction. */
    public double getDemandBonus() {
        double total = 0;
        for (Upgrade upgrade : upgrades) {
            total += upgrade.getDemandBonus();
        }
        return total;
    }

    /** How much upgrades soften customers' reaction to price. */
    public double getSensitivityReduction() {
        double total = 0;
        for (Upgrade upgrade : upgrades) {
            total += upgrade.getSensitivityReduction();
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Score
    // ------------------------------------------------------------------

    /**
     * Cash, plus the fuel in the ground valued at wholesale, minus the loan.
     * This is what the game is scored on.
     */
    public BigDecimal netWorth(GasStation station, BigDecimal wholesalePerLitre) {
        BigDecimal stock = Money.cash(
                wholesalePerLitre.multiply(BigDecimal.valueOf(station.getTank().getLevelLitres())));
        return Money.cash(cash.add(stock).subtract(debt));
    }
}
