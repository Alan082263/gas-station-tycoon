import gasstation.*;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Plain-Java self-checks for the domain model - no JUnit, no dependencies.
 * Run with: java SelfTest
 */
public class SelfTest {

    static int failures = 0;

    static void check(String label, boolean ok) {
        System.out.printf("%-58s %s%n", label, ok ? "PASS" : "FAIL");
        if (!ok) failures++;
    }

    static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    static boolean throwsISE(Runnable r) {
        try { r.run(); return false; } catch (IllegalStateException e) { return true; }
    }

    public static void main(String[] args) throws Exception {
        // --- construction validation
        check("negative tank capacity rejected", throwsIAE(() -> new FuelTank(-1, 0)));
        check("initial level above capacity rejected", throwsIAE(() -> new FuelTank(100, 200)));
        check("zero-litre customer rejected", throwsIAE(() -> new Customer("Zed", 0)));
        check("negative-litre customer rejected", throwsIAE(() -> new Customer("Zed", -5)));
        check("zero price rejected",
                throwsIAE(() -> new GasStation("S", 100, 50, BigDecimal.ZERO)));
        check("reserve fraction > 1 rejected", throwsIAE(() -> {
            new GasStation("S", 100, 50, new BigDecimal("1.00")).setReserveFraction(1.5);
        }));

        // --- tank never overfills
        FuelTank tank = new FuelTank(100, 90);
        check("addFuel caps at capacity", tank.addFuel(50) == 10.0 && tank.getLevelLitres() == 100.0);
        check("refillToCapacity on full tank adds 0", tank.refillToCapacity() == 0.0);

        // --- auto-refill fires on the reserve threshold
        GasStation s = new GasStation("Test", 1000, 300, new BigDecimal("2.000"));
        check("reserve is 20% of capacity", s.getReserveLitres() == 200.0);
        Transaction t = s.serve(new Customer("A", 150)); // 300-150=150 < 200 -> delivery first
        check("delivery triggered by threshold", s.getDeliveryCount() == 1);
        check("level after delivery + sale", Math.abs(s.getTank().getLevelLitres() - 850.0) < 1e-6);
        check("total priced exactly (150 @ 2.000)", t.getTotalPrice().compareTo(new BigDecimal("300.00")) == 0);

        // --- rounding: 1.729 * 33.33 = 57.627... -> 57.63
        GasStation r = new GasStation("Round", 1000, 900, new BigDecimal("1.729"));
        r.setDeliveriesAvailable(false);
        Transaction rt = r.serve(new Customer("B", 33.33));
        check("half-up cent rounding", rt.getTotalPrice().compareTo(new BigDecimal("57.63")) == 0);
        check("revenue matches sale", r.getTotalRevenue().compareTo(new BigDecimal("57.63")) == 0);

        // --- out of fuel
        GasStation dry = new GasStation("Dry", 1000, 40, new BigDecimal("1.500"));
        dry.setDeliveriesAvailable(false);
        try {
            dry.serve(new Customer("C", 60));
            check("out-of-fuel throws", false);
        } catch (OutOfFuelException e) {
            check("out-of-fuel throws", true);
            check("exception reports available litres", Math.abs(e.getAvailableLitres() - 40.0) < 1e-6);
        }
        check("no sale recorded after refusal", dry.getSales().isEmpty());
        check("pump released after failed sale", !dry.getPumps().get(0).isInUse());

        // --- request bigger than the whole tank, deliveries on
        GasStation big = new GasStation("Big", 1000, 1000, new BigDecimal("1.500"));
        try {
            big.serve(new Customer("D", 1200));
            check("over-capacity request throws", false);
        } catch (OutOfFuelException e) {
            check("over-capacity request throws", true);
        }

        // --- both pumps busy
        GasStation busy = new GasStation("Busy", 1000, 900, new BigDecimal("1.500"));
        busy.getPumps().get(0).beginFuelling(new Customer("E", 10));
        busy.getPumps().get(1).beginFuelling(new Customer("F", 10));
        check("no pump available", busy.findAvailablePump().equals(Optional.empty()));
        try {
            busy.serve(new Customer("G", 10));
            check("third customer refused", false);
        } catch (NoPumpAvailableException e) {
            check("third customer refused", true);
        }
        check("double-claiming a pump throws",
                throwsISE(() -> busy.getPumps().get(0).beginFuelling(new Customer("H", 10))));

        // --- fuelling on a pump you do not hold
        GasStation mis = new GasStation("Mis", 1000, 900, new BigDecimal("1.500"));
        Customer notAtPump = new Customer("I", 10);
        check("fuelling an unclaimed pump throws", throwsISE(() -> {
            try { mis.fuel(mis.getPumps().get(0), notAtPump, 10); }
            catch (OutOfFuelException e) { throw new AssertionError(e); }
        }));

        // --- defensive copies
        check("pump list unmodifiable", throwsUOE(() -> mis.getPumps().clear()));
        check("sales list unmodifiable", throwsUOE(() -> mis.getSales().clear()));

        System.out.println();
        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
    }

    static boolean throwsUOE(Runnable r) {
        try { r.run(); return false; } catch (UnsupportedOperationException e) { return true; }
    }
}
