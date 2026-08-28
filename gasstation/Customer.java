package gasstation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Someone who arrives wanting a quantity of fuel, and leaves with receipts.
 */
public class Customer {

    private final String name;
    private final double requestedLitres;
    private final List<Transaction> receipts = new ArrayList<>();

    /**
     * @param name            the customer's name
     * @param requestedLitres how much fuel they want; must be positive
     */
    public Customer(String name, double requestedLitres) {
        this.name = Objects.requireNonNull(name, "name");
        if (requestedLitres <= 0) {
            throw new IllegalArgumentException("A customer must ask for more than 0 litres, was " + requestedLitres);
        }
        this.requestedLitres = requestedLitres;
    }

    public String getName() {
        return name;
    }

    public double getRequestedLitres() {
        return requestedLitres;
    }

    /** Recorded by the station when a sale completes. */
    void addReceipt(Transaction transaction) {
        receipts.add(transaction);
    }

    public List<Transaction> getReceipts() {
        return Collections.unmodifiableList(receipts);
    }

    /** Everything this customer has paid the station. */
    public BigDecimal getTotalSpent() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : receipts) {
            total = total.add(t.getTotalPrice());
        }
        return total;
    }

    @Override
    public String toString() {
        return String.format("%s (wants %.1f L)", name, requestedLitres);
    }
}
