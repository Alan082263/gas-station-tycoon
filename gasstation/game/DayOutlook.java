package gasstation.game;

import java.time.DayOfWeek;

/**
 * The morning's news: what day it is, what the sky is doing, and whatever the
 * world has decided to throw at you today.
 *
 * <p>You see this <em>before</em> you set your price and order fuel, so a cup
 * final is an opportunity and a tanker strike is a problem you can plan around.
 */
public class DayOutlook {

    private final int day;
    private final DayOfWeek dayOfWeek;
    private final Weather weather;
    private final RandomEvent event;

    DayOutlook(int day, DayOfWeek dayOfWeek, Weather weather, RandomEvent event) {
        this.day = day;
        this.dayOfWeek = dayOfWeek;
        this.weather = weather;
        this.event = event;
    }

    public int getDay() { return day; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }

    public Weather getWeather() { return weather; }

    /** The day's event, or {@code null} if it is an ordinary day. */
    public RandomEvent getEvent() { return event; }

    public boolean hasEvent() { return event != null; }

    /** True if no tanker can reach you today. */
    public boolean deliveriesBlocked() {
        return event != null && event.blocksDelivery();
    }

    /** A friendly name for the day, e.g. "Day 4 (Thursday)". */
    public String getLabel() {
        String name = dayOfWeek.toString();
        return "Day " + day + " (" + name.charAt(0) + name.substring(1).toLowerCase() + ")";
    }
}
