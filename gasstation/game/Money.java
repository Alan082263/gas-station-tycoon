package gasstation.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Small helpers for the money arithmetic the game does constantly.
 *
 * <p>Cash amounts are kept to 2 decimal places; pump and wholesale prices to 3
 * (a station really does price fuel at $1.729 a litre).
 */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private static final DecimalFormat CASH = new DecimalFormat("#,##0.00");

    private Money() {
        // utility class
    }

    /** Rounds to cents. */
    public static BigDecimal cash(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /** Rounds to cents. */
    public static BigDecimal cash(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Rounds to a tenth of a cent, the way fuel is priced. */
    public static BigDecimal perLitre(double amount) {
        return BigDecimal.valueOf(amount).setScale(3, RoundingMode.HALF_UP);
    }

    /** "$1,234.50", or "-$1,234.50" for negatives. */
    public static String format(BigDecimal amount) {
        String digits = CASH.format(amount.abs());
        return (amount.signum() < 0 ? "-$" : "$") + digits;
    }

    /** Right-aligned in a column of the given width. */
    public static String format(BigDecimal amount, int width) {
        return String.format("%" + width + "s", format(amount));
    }
}
