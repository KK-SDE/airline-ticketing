package com.airlineticketing.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeatLabelUtils {

    private static final Pattern SEAT_PATTERN = Pattern.compile("^(\\d{1,3})([A-Z])$");

    public static final Comparator<String> SEAT_ORDER = Comparator
            .comparingInt(SeatLabelUtils::rowOf)
            .thenComparing(label -> label.charAt(label.length() - 1));

    private SeatLabelUtils() {
    }

    public static String normalize(String label) {
        return label.trim().toUpperCase(Locale.ROOT);
    }

    public static int capacity(int rows, String letters) {
        return rows * letters.length();
    }

    public static boolean isValid(String label, int rows, String letters) {
        Matcher matcher = SEAT_PATTERN.matcher(label);
        if (!matcher.matches()) {
            return false;
        }
        int row = Integer.parseInt(matcher.group(1));
        return row >= 1 && row <= rows && letters.contains(matcher.group(2));
    }

    public static List<String> allLabels(int rows, String letters) {
        List<String> labels = new ArrayList<>(capacity(rows, letters));
        for (int row = 1; row <= rows; row++) {
            for (char letter : letters.toCharArray()) {
                labels.add(row + String.valueOf(letter));
            }
        }
        return labels;
    }

    private static int rowOf(String label) {
        return Integer.parseInt(label.substring(0, label.length() - 1));
    }
}
