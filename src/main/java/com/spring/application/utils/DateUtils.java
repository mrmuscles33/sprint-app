package com.spring.application.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class DateUtils {

    public static final String D_M_Y = "dd/MM/yyyy";
    public static final String D_M_Y_H_M_S = "dd/MM/yyyy HH:mm:ss";
    public static final String YMD = "yyyyMMdd";
    public static final String YMDHMS = "yyyyMMddHHmmss";


    private DateUtils() {
        super();
    }

    /**
     * Convert a date to a string using the given format
     *
     * @param date   The date to convert
     * @param format The format to use
     * @return The date as a string
     */
    public static String toString(LocalDateTime date, String format) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }

    /**
     * Convert a date to a string using the given format
     *
     * @param date   The date to convert
     * @param format The format to use
     * @return The date as a string
     */
    public static String toString(LocalDate date, String format) {
        return toString(date.atStartOfDay(), format);
    }

    /**
     * Convert a date to a string using the default format (dd/MM/yyyy HH:mm:ss)
     *
     * @param date The date to convert
     * @return The date as a string
     */
    public static String toString(LocalDateTime date) {
        return toString(date, YMDHMS);
    }

    /**
     * Convert a date to a string using the default format (dd/MM/yyyy)
     *
     * @param date The date to convert
     * @return The date as a string
     */
    public static String toString(LocalDate date) {
        return toString(date.atStartOfDay(), YMD);
    }

    /**
     * Convert a string to a date using the given format
     *
     * @param date   The date to convert
     * @param format The format to use
     * @return The date
     */
    public static LocalDateTime toDateTime(String date, String format) {
        format = StringUtils.isEmpty(format) ? guessFormat(date) : format;
        return StringUtils.isEmpty(date) ? null : LocalDateTime.parse(date, DateTimeFormatter.ofPattern(format));
    }

    /**
     * Convert a string to a date and guess the format
     *
     * @param date The date to convert
     * @return The date
     */
    public static LocalDateTime toDateTime(String date) {
        return toDateTime(date, guessFormat(date));
    }

    /**
     * Convert a string to a date using the given format
     *
     * @param date   The date to convert
     * @param format The format to use
     * @return The date
     */
    public static LocalDate toDate(String date, String format) {
        LocalDateTime dateTime = toDateTime(date, format);
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    /**
     * Convert a string to a date and guess the format
     *
     * @param date The date to convert
     * @return The date
     */
    public static LocalDate toDate(String date) {
        LocalDateTime dateTime = toDateTime(date, guessFormat(date));
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    /**
     * Return the current date time
     *
     * @return The current date time
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Return the current date
     *
     * @return The current date
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * Return a date time
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @return The date time
     */
    public static LocalDateTime get(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
    }

    /**
     * Return a date
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @return The date
     */
    public static LocalDate get(int year, int month, int dayOfMonth) {
        return LocalDate.of(year, month, dayOfMonth);
    }

    /**
     * Check if a string is a valid date
     *
     * @param date   The date to check
     * @param format The format to use
     * @return True if the string is a valid date
     */
    public static boolean isDate(String date, String format) {
        try {
            toDateTime(date, format);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find the format of a date
     *
     * @param date The date to check
     * @return The format of the date
     */
    public static String guessFormat(String date) {
        return Stream.of(D_M_Y_H_M_S, D_M_Y, YMDHMS, YMD)
                .filter(format -> isDate(date, format))
                .findFirst()
                .orElse("");
    }

    /**
     * Check if a date is between two dates
     *
     * @param date  The date to check
     * @param start The start date
     * @param end   The end date
     * @return True if the date is between start and end, false otherwise
     */
    public static boolean isBetween(LocalDateTime date, LocalDateTime start, LocalDateTime end) {
        return date.isAfter(start) && date.isBefore(end);
    }
}
