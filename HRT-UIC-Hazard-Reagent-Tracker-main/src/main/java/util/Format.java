package util;

/** Small helper so 500.0 is shown as 500. */
public class Format {
    public static String qty(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
