package gasstation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * A completed sale: an immutable record of who bought how much, at which pump,
 * at what price.
 *
 * <p>Money is held as {@link BigDecimal} rather than {@code double}. Prices per
 * litre carry three decimals (1.729), totals are rounded to cents once, at the
 * moment of sale.
 */
public final class Transaction {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int id;
    private final int pumpNumber;
    private final String customerName;
    private final double litres;
    private final BigDecimal pricePerLitre;
    private final BigDecimal totalPrice;
    private final LocalDateTime timestamp;

    Transaction(int id, int pumpNumber, String customerName, double litres, BigDecimal pricePerLitre) {
        this.id = id;
        this.pumpNumber = pumpNumber;
        this.customerName = Objects.requireNonNull(customerName, "customerName");
        this.litres = litres;
        this.pricePerLitre = Objects.requireNonNull(pricePerLitre, "pricePerLitre");
        this.totalPrice = pricePerLitre.multiply(BigDecimal.valueOf(litres)).setScale(2, RoundingMode.HALF_UP);
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public int getPumpNumber() {
        return pumpNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public double getLitres() {
        return litres;
    }

    public BigDecimal getPricePerLitre() {
        return pricePerLitre;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** A printable customer receipt. */
    public String toReceipt() {
        return String.format(
                "  ---- RECEIPT #%04d ----%n"
                + "  Time       : %s%n"
                + "  Pump       : %d%n"
                + "  Customer   : %s%n"
                + "  Litres     : %.2f L%n"
                + "  Price/litre: $%s%n"
                + "  TOTAL      : $%s%n",
                id, timestamp.format(TIME), pumpNumber, customerName, litres, pricePerLitre, totalPrice);
    }

    @Override
    public String toString() {
        return String.format("#%04d %s pump %d %-18s %8.2f L @ $%s = $%s",
                id, timestamp.format(TIME), pumpNumber, customerName, litres, pricePerLitre, totalPrice);
    }
}
