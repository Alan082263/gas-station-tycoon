package gasstation.gui;

import gasstation.GasStation;
import gasstation.game.Business;
import gasstation.game.DayOutlook;
import gasstation.game.DayResult;
import gasstation.game.DaySimulator;
import gasstation.game.GameConfig;
import gasstation.game.Market;
import gasstation.game.Money;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A JavaFX front end over the same game the console version plays.
 *
 * <p>This is a second, independent orchestrator sitting alongside
 * {@link gasstation.game.GasStationTycoon}: same {@link GameConfig},
 * {@link Business}, {@link Market}, {@link GasStation} and
 * {@link DaySimulator}, wired to buttons and dialogs ({@link ActionDialogs})
 * instead of a terminal prompt. Nothing in {@code gasstation} or
 * {@code gasstation.game} changes to support this.
 *
 * <pre>
 *   ./run-gui.sh
 * </pre>
 */
public class GasStationTycoonFX extends Application {

    private Stage stage;
    private BorderPane root;

    private GameConfig config;
    private Business business;
    private Market market;
    private GasStation station;
    private DaySimulator simulator;
    private final List<DayResult> history = new ArrayList<>();
    private long seed;
    private DayOutlook outlook;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.root = new BorderPane();
        stage.setTitle("Gas Station Tycoon");
        stage.setScene(new Scene(root, 720, 560));
        stage.show();
        showIntro();
    }

    // ------------------------------------------------------------------
    // Intro / new game
    // ------------------------------------------------------------------

    private void showIntro() {
        Label title = new Label("GAS STATION TYCOON");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 28));

        Label blurb = new Label(
                "You have inherited a two-pump forecourt and a loan you did not ask for.\n"
                + "Set your price, buy fuel, spend on the place, then open up and see how\n"
                + "many drivers liked your price.");
        blurb.setWrapText(true);

        ToggleGroup difficulty = new ToggleGroup();
        RadioButton easy = new RadioButton("Easy");
        RadioButton normal = new RadioButton("Normal");
        RadioButton hard = new RadioButton("Hard");
        easy.setToggleGroup(difficulty);
        normal.setToggleGroup(difficulty);
        hard.setToggleGroup(difficulty);
        normal.setSelected(true);
        HBox difficultyBox = new HBox(16, easy, normal, hard);

        TextField nameField = new TextField(GameConfig.normal().getStationName());
        TextField daysField = new TextField(String.valueOf(GameConfig.normal().getDays()));
        TextField seedField = new TextField();
        seedField.setPromptText("blank = random");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Difficulty:"), difficultyBox);
        form.addRow(1, new Label("Station name:"), nameField);
        form.addRow(2, new Label("Days:"), daysField);
        form.addRow(3, new Label("Seed:"), seedField);

        Button start = new Button("Start");
        start.setDefaultButton(true);
        start.setOnAction(e -> {
            GameConfig chosen = easy.isSelected() ? GameConfig.easy()
                    : hard.isSelected() ? GameConfig.hard()
                    : GameConfig.normal();
            if (!nameField.getText().isBlank()) {
                chosen.setStationName(nameField.getText().trim());
            }
            try {
                int days = Integer.parseInt(daysField.getText().trim());
                chosen.setDays(days);
            } catch (NumberFormatException ex) {
                // keep the difficulty's default
            }
            if (!seedField.getText().isBlank()) {
                try {
                    chosen.setSeed(Long.parseLong(seedField.getText().trim()));
                } catch (NumberFormatException ex) {
                    // keep the random seed
                }
            }
            newGame(chosen);
        });

        VBox layout = new VBox(20, title, blurb, form, start);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setPadding(new Insets(30));
        root.setCenter(layout);
    }

    private void newGame(GameConfig chosenConfig) {
        this.config = chosenConfig;
        this.seed = config.getSeed() != 0 ? config.getSeed() : new Random().nextLong();
        Random random = new Random(seed);

        this.station = new GasStation(config.getStationName(),
                config.getTankCapacityLitres(), config.getStartingFuelLitres(),
                config.getStartingPumpPrice(), config.getStartingPumps());
        this.station.setDeliveriesAvailable(false);
        this.station.setReserveFraction(0.0);

        this.business = new Business(config.getStartingCash(), config.getStartingDebt());
        this.market = new Market(config, random);
        this.simulator = new DaySimulator(config, market, random);
        this.history.clear();

        dawnNextDay(1);
    }

    private void dawnNextDay(int day) {
        this.outlook = simulator.dawn(day);
        business.beginDay();
        showDashboard();
    }

    // ------------------------------------------------------------------
    // The dashboard: the morning's news and the actions you can take
    // ------------------------------------------------------------------

    private void showDashboard() {
        Label header = new Label(String.format("%s  -  %s, %s",
                outlook.getLabel(), config.getDays() + " day run", outlook.getWeather().getLabel()));
        header.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));

        GridPane stats = new GridPane();
        stats.setHgap(20);
        stats.setVgap(6);
        stats.addRow(0,
                statLabel("Cash", Money.format(business.getCash())),
                statLabel("Debt", Money.format(business.getDebt())),
                statLabel("Net worth", Money.format(business.netWorth(station, market.getWholesalePerLitre()))));
        stats.addRow(1,
                statLabel("Tank", String.format("%,.0f / %,.0f L",
                        station.getTank().getLevelLitres(), station.getTank().getCapacityLitres())),
                statLabel("Pumps", station.getPumpCount() + " (" + station.getPumpCount() * config.getFillsPerPumpPerDay() + " fills/day)"),
                new Region());
        stats.addRow(2,
                statLabel("Your price", "$" + station.getPricePerLitre() + "/L"),
                statLabel("Wholesale", "$" + market.getWholesalePerLitre() + "/L"),
                statLabel("Margin", "$" + station.getPricePerLitre().subtract(market.getWholesalePerLitre()) + "/L"));

        VBox top = new VBox(10, header, stats);

        if (outlook.hasEvent()) {
            Label newsTitle = new Label((outlook.getEvent().isGoodNews() ? "GOOD NEWS: " : "NEWS: ")
                    + outlook.getEvent().getTitle().toUpperCase());
            newsTitle.setFont(Font.font(null, FontWeight.BOLD, 13));
            Label newsBody = new Label(outlook.getEvent().getDescription());
            newsBody.setWrapText(true);
            VBox news = new VBox(4, newsTitle, newsBody);
            news.setPadding(new Insets(10));
            news.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 6;");
            top.getChildren().add(news);
        }
        if (station.getTank().getLevelLitres() < 600) {
            Label warning = new Label("! The tank is nearly dry. Order fuel or you will turn cars away.");
            warning.setStyle("-fx-text-fill: #b00020;");
            top.getChildren().add(warning);
        }
        top.setPadding(new Insets(20));

        Button setPrice = new Button("Set pump price");
        setPrice.setOnAction(e -> { ActionDialogs.showSetPrice(stage, config, station, market); showDashboard(); });

        Button orderFuel = new Button("Order fuel");
        orderFuel.setOnAction(e -> { ActionDialogs.showOrderFuel(stage, config, station, business, market, outlook); showDashboard(); });

        Button upgrades = new Button("Upgrades");
        upgrades.setOnAction(e -> { ActionDialogs.showUpgrades(stage, business, station, config); showDashboard(); });

        Button repayLoan = new Button("Repay the loan");
        repayLoan.setOnAction(e -> { ActionDialogs.showRepayLoan(stage, business, config); showDashboard(); });

        Button help = new Button("How this works");
        help.setOnAction(e -> ActionDialogs.showHelp(stage, config));

        Button open = new Button("OPEN FOR BUSINESS");
        open.setDefaultButton(true);
        open.setOnAction(e -> runDay());

        Button giveUp = new Button("Give up");
        giveUp.setOnAction(e -> confirmGiveUp());

        VBox actions = new VBox(10, setPrice, orderFuel, upgrades, repayLoan, help, open, giveUp);
        actions.setPadding(new Insets(20));
        actions.setPrefWidth(220);

        root.setCenter(top);
        root.setRight(actions);
        root.setBottom(null);
    }

    private Label statLabel(String name, String value) {
        Label label = new Label(name + ": " + value);
        label.setFont(Font.font("Monospaced", 13));
        return label;
    }

    private void confirmGiveUp() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hand back the keys and end this run?", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(stage);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                showEnding("You hand back the keys. The forecourt stands empty.", false);
            }
        });
    }

    // ------------------------------------------------------------------
    // Trading, and the report afterwards
    // ------------------------------------------------------------------

    private void runDay() {
        DayResult result = simulator.runDay(outlook, station, business);
        history.add(result);
        showDayReport(result);
    }

    private void showDayReport(DayResult r) {
        Label title = new Label("CLOSING TIME - " + outlook.getLabel());
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("%d cars served", r.getServedCustomers()));
        if (r.getTotalTurnedAway() > 0) {
            summary.append(String.format(", %d turned away (%d queue, %d no fuel)",
                    r.getTotalTurnedAway(), r.getTurnedAwayQueue(), r.getTurnedAwayNoFuel()));
        }
        summary.append(String.format("%n%,.0f litres sold at $%s", r.getLitresSold(), r.getPumpPrice()));

        GridPane breakdown = new GridPane();
        breakdown.setHgap(20);
        breakdown.setVgap(4);
        int row = 0;
        breakdown.addRow(row++, new Label("Fuel sales"), new Label(Money.format(r.getFuelRevenue())));
        if (r.getShopRevenue().signum() > 0) {
            breakdown.addRow(row++, new Label("Shop & extras"), new Label(Money.format(r.getShopRevenue())));
        }
        if (r.getFuelPurchases().signum() > 0) {
            breakdown.addRow(row++, new Label("Fuel bought"), new Label(Money.format(r.getFuelPurchases().negate())));
        }
        breakdown.addRow(row++, new Label("Running costs"), new Label(Money.format(r.getRunningCosts().negate())));
        if (r.getEventCosts().signum() > 0) {
            breakdown.addRow(row++, new Label("One-off bills"), new Label(Money.format(r.getEventCosts().negate())));
        }
        breakdown.addRow(row++, new Label("Cash change"), new Label(Money.format(r.getCashChange())));

        Label after = new Label(String.format("Cash %s   Debt %s (+%s interest)   Net worth %s%nTank %,.0f L left.",
                Money.format(r.getCashAfter()), Money.format(r.getDebtAfter()),
                Money.format(r.getInterest()), Money.format(r.getNetWorthAfter()), r.getFuelInTankAfter()));

        Button continueButton = new Button("Continue");
        continueButton.setDefaultButton(true);
        continueButton.setOnAction(e -> afterDayReport(r));

        VBox layout = new VBox(16, title, new Label(summary.toString()), breakdown, after, continueButton);
        layout.setPadding(new Insets(24));
        root.setCenter(layout);
        root.setRight(null);
    }

    private void afterDayReport(DayResult r) {
        if (business.isBankrupt()) {
            showEnding(String.format("You ended day %d %s in the red. The bank takes the keys.",
                    r.getDay(), Money.format(r.getCashAfter().abs())), false);
        } else if (r.getDay() >= config.getDays()) {
            BigDecimal netWorth = business.netWorth(station, market.getWholesalePerLitre());
            boolean won = netWorth.compareTo(config.getTargetNetWorth()) >= 0;
            showEnding(String.format("After %d days: net worth %s against a target of %s.%n%s",
                    config.getDays(), Money.format(netWorth), Money.format(config.getTargetNetWorth()),
                    verdict(netWorth, config.getTargetNetWorth(), won)), won);
        } else {
            dawnNextDay(r.getDay() + 1);
        }
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

    // ------------------------------------------------------------------
    // Ending
    // ------------------------------------------------------------------

    private void showEnding(String message, boolean won) {
        Label banner = new Label(won ? "W I N" : "G A M E   O V E R");
        banner.setFont(Font.font("Monospaced", FontWeight.BOLD, 26));

        Label body = new Label(message);
        body.setWrapText(true);

        TableView<DayResult> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(history));
        table.getColumns().add(column("Day", r -> "Day " + r.getDay()));
        table.getColumns().add(column("Price", r -> "$" + r.getPumpPrice()));
        table.getColumns().add(column("Cars", r -> String.valueOf(r.getServedCustomers())));
        table.getColumns().add(column("Litres", r -> String.format("%,.0f L", r.getLitresSold())));
        table.getColumns().add(column("Revenue", r -> Money.format(r.getTotalRevenue())));
        table.getColumns().add(column("Cash", r -> Money.format(r.getCashAfter())));
        table.getColumns().add(column("Net worth", r -> Money.format(r.getNetWorthAfter())));
        table.getColumns().add(column("Event", r -> r.getEvent() == null ? "" : r.getEvent().getTitle()));
        table.setPrefHeight(260);

        Label totals = new Label(String.format(
                "%,.0f litres sold across %d days for %s.%n%d deliveries taken, %s paid in interest.%nSeed %d.",
                station.getTotalLitresSold(), history.size(), Money.format(station.getTotalRevenue()),
                station.getDeliveryCount(), Money.format(business.getTotalInterestPaid()), seed));

        Button playAgain = new Button("Play again");
        playAgain.setDefaultButton(true);
        playAgain.setOnAction(e -> showIntro());

        VBox layout = new VBox(14, banner, body, table, totals, playAgain);
        layout.setPadding(new Insets(24));
        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);

        root.setCenter(scroll);
        root.setRight(null);
    }

    private TableColumn<DayResult, String> column(String title, java.util.function.Function<DayResult, String> extractor) {
        TableColumn<DayResult, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new SimpleStringProperty(extractor.apply(data.getValue())));
        return col;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
