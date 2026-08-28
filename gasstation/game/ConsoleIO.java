package gasstation.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Terminal input and output, with the fiddly bits handled once: bad numbers are
 * re-prompted rather than crashing the game, blank input takes a default, and
 * the end of input (Ctrl-D, or a script piping commands in) is reported through
 * {@link #isClosed()} instead of throwing.
 */
public class ConsoleIO {

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private boolean closed;

    public void println(String line) {
        System.out.println(line);
    }

    public void println() {
        System.out.println();
    }

    public void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    /** True once input has run out - the game should wind up politely. */
    public boolean isClosed() {
        return closed;
    }

    /**
     * @return the line typed with surrounding space removed, or {@code null} at
     *         the end of input
     */
    public String ask(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try {
            String line = reader.readLine();
            if (line == null) {
                closed = true;
                println();
                return null;
            }
            return line.trim();
        } catch (IOException e) {
            closed = true;
            return null;
        }
    }

    /**
     * Asks for a whole number in a range, re-prompting until it gets one.
     * Blank input, or the end of input, gives {@code fallback}.
     */
    public int askInt(String prompt, int min, int max, int fallback) {
        while (!closed) {
            String input = ask(prompt);
            if (input == null || input.isEmpty()) {
                return fallback;
            }
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    printf("  Please enter a number between %d and %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                println("  That is not a number.");
            }
        }
        return fallback;
    }

    /**
     * Asks for a decimal in a range, re-prompting until it gets one. Accepts a
     * leading '$' so typing the price the way you say it still works.
     */
    public double askDouble(String prompt, double min, double max, double fallback) {
        while (!closed) {
            String input = ask(prompt);
            if (input == null || input.isEmpty()) {
                return fallback;
            }
            String cleaned = input.replace("$", "").replace(",", "").trim();
            try {
                double value = Double.parseDouble(cleaned);
                if (value < min || value > max) {
                    printf("  Please enter a value between %.2f and %.2f.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                println("  That is not a number.");
            }
        }
        return fallback;
    }

    /** Yes/no, defaulting to no on blank input. */
    public boolean confirm(String prompt) {
        String input = ask(prompt + " [y/N] ");
        return input != null && (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes"));
    }

    /** Waits for the player to read the screen. */
    public void pause() {
        ask("  -- press Enter --");
    }
}
