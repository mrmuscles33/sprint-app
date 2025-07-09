package com.spring.application.utils;

public class NumberUtils {

    private NumberUtils() {
        super();
    }

    /**
     * Convert an object to an integer
     * If the object is null, return the default value
     *
     * @param obj          The object to convert
     * @param defaultValue The default value
     * @return The integer
     */
    public static int toInt(Object obj, int defaultValue) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        try {
            return Integer.parseInt(StringUtils.toString(obj));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convert an object to an integer
     * If the object is null, return 0
     *
     * @param obj The object to convert
     * @return The integer
     */
    public static int toInt(Object obj) {
        return toInt(obj, 0);
    }

    /**
     * Convert an object to a double
     * If the object is null, return the default value
     *
     * @param obj          The object to convert
     * @param defaultValue The default value
     * @return The double
     */
    public static double toDouble(Object obj, double defaultValue) {
        if (obj instanceof Double) {
            return (Double) obj;
        }
        try {
            return Double.parseDouble(StringUtils.toString(obj));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convert an object to a double
     * If the object is null, return 0
     *
     * @param obj The object to convert
     * @return The double
     */
    public static double toDouble(Object obj) {
        return toDouble(obj, 0.0);
    }

    /**
     * Check if an object is an integer
     *
     * @param obj The object to check
     * @return True if the object is an integer, false otherwise
     */
    public static boolean isInt(Object obj) {
        try {
            Integer.parseInt(StringUtils.toString(obj));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if an object is a double
     *
     * @param obj The object to check
     * @return True if the object is a double, false otherwise
     */
    public static boolean isDouble(Object obj) {
        try {
            Double.parseDouble(StringUtils.toString(obj));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if a value is between min and max
     *
     * @param value The value to check
     * @param min   The min value
     * @param max   The max value
     * @return True if the value is between min and max, false otherwise
     */
    public static boolean in(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Check if a value is between min and max
     *
     * @param value The value to check
     * @param min   The min value
     * @param max   The max value
     * @return True if the value is between min and max, false otherwise
     */
    public static boolean in(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
