package gasstation;

import java.math.BigDecimal;

/**
 * Runs a day at the station, exercising every rule in the model.
 *
 * <p>Compile and run from the folder <em>above</em> {@code gasstation}:
 * <pre>
 *   javac gasstation/*.java
 *   java gasstation.StationDemo
 * </pre>
 */
public class StationDemo {

    public static void main(String[] args) {
        GasStation station = new GasStation("Riverside Fuels", 10_000, 1_500, new BigDecimal("1.729"));
        station.hireAttendant(new Attendant("Jamie", 19));
        station.hireAttendant(new Attendant("Kofi", 22));

        banner("OPENING UP");
        System.out.println(station.getStatus());
        System.out.println("  Attendants on shift: " + station.getAttendants());
        System.out.printf("Tank is below the %.0f%% reserve, so the first sale will trigger a delivery.%n",
                station.getReserveFraction() * 100);

        banner("A NORMAL MORNING");
        serve(station, new Customer("Maria", 45.0));
        serve(station, new Customer("Dev", 62.5));
        System.out.println(station.getStatus());

        banner("A RECEIPT");
        Customer priya = new Customer("Priya", 38.25);
        serve(station, priya);
        System.out.print(priya.getReceipts().get(0).toReceipt());
        System.out.println("  Priya has spent $" + priya.getTotalSpent() + " here in total.");

        banner("BOTH PUMPS BUSY");
        Pump pumpOne = station.getPumps().get(0);
        Pump pumpTwo = station.getPumps().get(1);
        Customer slowOne = new Customer("Tom", 50);
        Customer slowTwo = new Customer("Yuki", 30);
        pumpOne.beginFuelling(slowOne);
        pumpTwo.beginFuelling(slowTwo);
        System.out.println(station.getStatus());
        serve(station, new Customer("Ines", 40)); // no pump free -> turned away

        // Tom and Yuki finish their fills at the pumps they are holding.
        try {
            System.out.println("  " + station.fuel(pumpOne, slowOne, slowOne.getRequestedLitres()));
            System.out.println("  " + station.fuel(pumpTwo, slowTwo, slowTwo.getRequestedLitres()));
        } catch (OutOfFuelException e) {
            System.out.println("  Refused: " + e.getMessage());
        } finally {
            pumpOne.finishFuelling();
            pumpTwo.finishFuelling();
        }
        System.out.println(station.getStatus());

        banner("THE PRICE GOES UP");
        station.setPricePerLitre(new BigDecimal("1.849"));
        System.out.println("  New price: $" + station.getPricePerLitre() + "/L");
        serve(station, new Customer("Ade", 45));

        banner("A TANKER EMPTIES THE STATION");
        // Deliveries dry up, and a haulier wants more than is left in the ground.
        station.setDeliveriesAvailable(false);
        serve(station, new Customer("Northline Haulage", 9_700));   // drains the tank
        System.out.println(station.getStatus());
        serve(station, new Customer("Late Larry", 60));             // nothing left, no delivery coming

        banner("THE OWNER CALLS THE SUPPLIER");
        station.setDeliveriesAvailable(true);
        System.out.printf("  Delivery of %.1f L arrived.%n", station.orderDelivery());
        serve(station, new Customer("Late Larry", 60));             // served this time

        banner("END OF DAY");
        System.out.println(station.getSalesReport());
    }

    /** Serves one customer and reports what happened, handling both refusals. */
    private static void serve(GasStation station, Customer customer) {
        try {
            Transaction sale = station.serve(customer);
            System.out.println("  " + sale);
        } catch (NoPumpAvailableException e) {
            System.out.println("  " + customer.getName() + " waits: " + e.getMessage());
        } catch (OutOfFuelException e) {
            System.out.printf("  Sorry %s - %s.%n", customer.getName(), e.getMessage());
        }
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " " + "=".repeat(Math.max(0, 46 - title.length())));
    }
}
