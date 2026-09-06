package gasstation.gui;

import gasstation.GasStation;
import gasstation.game.Business;
import gasstation.game.DayOutlook;
import gasstation.game.GameConfig;
import gasstation.game.Market;
import gasstation.game.Money;
import gasstation.game.Upgrade;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.math.BigDecimal;

/**
 * The five things you can do before opening the pumps, each as a small modal
 * window over the dashboard. These are the GUI equivalent of the private
 * {@code setPrice()/orderFuel()/showUpgrades()/repayLoan()/printHelp()}
 * methods in the console game - same rules, same underlying calls into
 * {@link Business} and {@link GasStation}, just read from controls instead of
 * {@code ConsoleIO} prompts.
 */
public final class ActionDialogs {

    private ActionDialogs() {
    }

    public static void showSetPrice(Window owner, GameConfig config, GasStation station, Market market) {
        TextInputDialog dialog = new TextInputDialog(station.getPricePerLitre().toPlainString());
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Set pump price");
        dialog.setHeaderText(String.format(
                "The going rate around here is $%s/L. You pay $%s/L wholesale.",
                config.getReferencePrice(), market.getWholesalePerLitre()));
        dialog.setContentText("New price per litre ($0.10 - $9.99):");

        dialog.showAndWait().ifPresent(text -> {
            try {
                double price = Double.parseDouble(text.trim());
                if (price < 0.10 || price > 9.99) {
                    showError(owner, "Price must be between $0.10 and $9.99.");
                    return;
                }
                station.setPricePerLitre(Money.perLitre(price));
            } catch (NumberFormatException e) {
                showError(owner, "That is not a number.");
            }
        });
    }

    public static void showOrderFuel(Window owner, GameConfig config, GasStation station,
                                      Business business, Market market, DayOutlook outlook) {
        if (outlook.deliveriesBlocked()) {
            showInfo(owner, "No delivery today", "No tanker is coming today - " + outlook.getEvent().getTitle() + ".");
            return;
        }
        BigDecimal wholesale = market.getWholesalePerLitre();
        double headroom = station.getTank().getHeadroomLitres();
        double affordable = business.affordableLitres(wholesale, config.getDeliveryFee(), station);

        if (affordable < 1) {
            showInfo(owner, "Order fuel", "You cannot afford a delivery today.");
            return;
        }

        TextField amount = new TextField();
        amount.setPromptText("Litres");
        Button maxButton = new Button("Max");
        maxButton.setOnAction(e -> amount.setText(String.format("%.0f", affordable)));

        VBox content = new VBox(8,
                new Label(String.format("Tank: %,.0f / %,.0f L - room for %,.0f L.",
                        station.getTank().getLevelLitres(), station.getTank().getCapacityLitres(), headroom)),
                new Label(String.format("Cash %s. At $%s/L plus %s delivery you can afford %,.0f L.",
                        Money.format(business.getCash()), wholesale, Money.format(config.getDeliveryFee()), affordable)),
                new HBox(8, new Label("Litres to order:"), amount, maxButton));
        content.setPadding(new Insets(10));

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Order fuel");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(button -> {
            if (button != ButtonType.OK || amount.getText().isBlank()) {
                return;
            }
            double litres;
            try {
                litres = Double.parseDouble(amount.getText().replace(",", "").trim());
            } catch (NumberFormatException e) {
                showError(owner, "That is not a number.");
                return;
            }
            if (litres <= 0) {
                return;
            }
            if (litres > affordable) {
                showError(owner, String.format("You can only manage %,.0f L today.", affordable));
                return;
            }
            double delivered = business.buyFuel(litres, wholesale, config.getDeliveryFee(), station);
            BigDecimal bill = Money.cash(wholesale.multiply(BigDecimal.valueOf(delivered)).add(config.getDeliveryFee()));
            StringBuilder message = new StringBuilder(String.format(
                    "%,.0f L delivered for %s. Tank now %,.0f L. Cash %s.",
                    delivered, Money.format(bill), station.getTank().getLevelLitres(), Money.format(business.getCash())));
            if (business.getCash().compareTo(config.getDailyFixedCosts()) < 0) {
                message.append("\n\n! That is everything you had. If today goes badly you are finished.");
            }
            showInfo(owner, "Delivery arrived", message.toString());
        });
    }

    public static void showUpgrades(Window owner, Business business, GasStation station, GameConfig config) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Upgrades");

        Label cashLabel = new Label();
        VBox rows = new VBox(6);
        rows.setPadding(new Insets(10));

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            cashLabel.setText("Cash: " + Money.format(business.getCash()));
            rows.getChildren().clear();
            for (Upgrade upgrade : Upgrade.values()) {
                boolean owned = business.hasUpgrade(upgrade);
                Label label = new Label(String.format("%-20s %9s  %s%s",
                        upgrade.getLabel(), Money.format(upgrade.getCost()), upgrade.getDescription(),
                        owned ? "  [OWNED]" : ""));
                Button buy = new Button("Buy");
                buy.setDisable(owned || !business.canAfford(upgrade.getCost()));
                buy.setOnAction(e -> {
                    if (business.buyUpgrade(upgrade, station)) {
                        refresh[0].run();
                    }
                });
                HBox row = new HBox(10, label, buy);
                row.setSpacing(10);
                rows.getChildren().add(row);
            }
        };
        refresh[0].run();

        Button done = new Button("Done");
        done.setOnAction(e -> dialog.close());

        VBox root = new VBox(10, cashLabel, rows, done);
        root.setPadding(new Insets(12));
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
    }

    public static void showRepayLoan(Window owner, Business business, GameConfig config) {
        if (business.getDebt().signum() <= 0) {
            showInfo(owner, "Repay the loan", "You owe the bank nothing. Enjoy it.");
            return;
        }
        BigDecimal dailyInterest = Money.cash(business.getDebt().doubleValue() * config.getDailyInterestRate());
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Repay the loan");
        dialog.setHeaderText(String.format("You owe %s, costing %s a day in interest. You have %s.",
                Money.format(business.getDebt()), Money.format(dailyInterest), Money.format(business.getCash())));
        dialog.setContentText("Repay how much? $");

        dialog.showAndWait().ifPresent(text -> {
            double amount;
            try {
                amount = Double.parseDouble(text.replace("$", "").replace(",", "").trim());
            } catch (NumberFormatException e) {
                showError(owner, "That is not a number.");
                return;
            }
            if (amount <= 0) {
                return;
            }
            BigDecimal repaid = business.repayDebt(Money.cash(amount));
            if (repaid.signum() <= 0) {
                showInfo(owner, "Repay the loan", "Nothing repaid.");
            } else {
                showInfo(owner, "Repay the loan",
                        String.format("Repaid %s. You now owe %s.", Money.format(repaid), Money.format(business.getDebt())));
            }
        });
    }

    public static void showHelp(Window owner, GameConfig config) {
        String text = String.format(
                "Drivers know what fuel costs. Around here the going rate is $%s.%n"
                + "Undercut it and far more cars stop; go over it and they drive on to%n"
                + "the next forecourt. The catch is that every litre you sell cheap is%n"
                + "margin you never get back, and your pumps can only serve so many%n"
                + "cars a day (%d per pump).%n%n"
                + "Wholesale prices drift day to day. Fill the tank when fuel is cheap%n"
                + "and you are buying tomorrow's margin today - if you have the space.%n%n"
                + "Fixed costs are %s a day whether you sell anything or not, and the%n"
                + "loan charges %.2f%% a day. Standing still loses.%n%n"
                + "You are scored on net worth: cash, plus the fuel in the ground,%n"
                + "minus what you owe.",
                config.getReferencePrice(), config.getFillsPerPumpPerDay(),
                Money.format(config.getDailyFixedCosts()), config.getDailyInterestRate() * 100);
        showInfo(owner, "How this works", text);
    }

    // ------------------------------------------------------------------

    private static void showInfo(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label content = new Label(message);
        content.setWrapText(true);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(420);
        alert.getDialogPane().setContent(scroll);
        VBox.setVgrow(content, Priority.ALWAYS);
        alert.showAndWait();
    }

    private static void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle("Can't do that");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
