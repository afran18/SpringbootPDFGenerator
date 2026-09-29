package com.ararts.quotation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts a rupee amount to words using the Indian system (Thousand, Lakh, Crore).
 * 125000.50 -> "Rupees One Lakh Twenty Five Thousand and Fifty Paise Only"
 */
public final class AmountInWords {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"};
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    private AmountInWords() {
    }

    public static String rupees(BigDecimal amount) {
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP).abs();
        long rupees = value.longValue();
        int paise = value.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        StringBuilder sb = new StringBuilder("Rupees ");
        sb.append(rupees == 0 ? "Zero" : toWords(rupees));
        if (paise > 0) {
            sb.append(" and ").append(twoDigits(paise)).append(" Paise");
        }
        return sb.append(" Only").toString();
    }

    private static String toWords(long n) {
        StringBuilder sb = new StringBuilder();

        long crore = n / 10_000_000L;
        n %= 10_000_000L;
        long lakh = n / 100_000L;
        n %= 100_000L;
        long thousand = n / 1_000L;
        n %= 1_000L;

        if (crore > 0) sb.append(toWords(crore)).append(" Crore ");
        if (lakh > 0) sb.append(twoDigits((int) lakh)).append(" Lakh ");
        if (thousand > 0) sb.append(twoDigits((int) thousand)).append(" Thousand ");
        if (n > 0) sb.append(threeDigits((int) n));

        return sb.toString().trim();
    }

    private static String threeDigits(int n) {
        StringBuilder sb = new StringBuilder();
        if (n >= 100) {
            sb.append(ONES[n / 100]).append(" Hundred");
            n %= 100;
            if (n > 0) sb.append(" ");
        }
        if (n > 0) sb.append(twoDigits(n));
        return sb.toString();
    }

    private static String twoDigits(int n) {
        if (n < 20) return ONES[n];
        return TENS[n / 10] + (n % 10 > 0 ? " " + ONES[n % 10] : "");
    }
}
