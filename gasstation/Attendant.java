package gasstation;

import java.util.Objects;

/**
 * A young attendant working the forecourt: under 25.
 */
public class Attendant {

    /** Attendants must be younger than this to be hired. */
    public static final int MAX_AGE = 25;

    private final String name;
    private final int age;

    /**
     * @param name the attendant's name
     * @param age  their age; must be at least 0 and under {@value #MAX_AGE}
     */
    public Attendant(String name, int age) {
        this.name = Objects.requireNonNull(name, "name");
        if (age < 0 || age >= MAX_AGE) {
            throw new IllegalArgumentException(
                    "Attendant must be under " + MAX_AGE + ", was " + age);
        }
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", name, age);
    }
}
