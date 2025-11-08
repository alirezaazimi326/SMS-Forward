package com.enixcoda.smsforward;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Central place that keeps the hard coded forwarding configuration.
 */
public final class ForwardingConfig {
    private static final String TARGET_NUMBER = "09388481156";
    private static final String ALLOWED_CANONICAL = "989907001156";
    private static final Set<String> DISPLAY_ALLOWED_SENDERS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("09907001156", "+989907001156"))
    );

    private ForwardingConfig() {
    }

    public static String getTargetNumber() {
        return TARGET_NUMBER;
    }

    public static Set<String> getDisplayAllowedSenders() {
        return DISPLAY_ALLOWED_SENDERS;
    }

    public static boolean isAllowedSender(String rawNumber) {
        String canonical = canonicalize(rawNumber);
        return !TextUtils.isEmpty(canonical) && canonical.equals(ALLOWED_CANONICAL);
    }

    public static String canonicalize(String rawNumber) {
        if (rawNumber == null) {
            return "";
        }

        String digitsOnly = rawNumber.replaceAll("[^0-9+]", "");
        if (digitsOnly.startsWith("+")) {
            digitsOnly = digitsOnly.substring(1);
        }

        if (digitsOnly.startsWith("00")) {
            digitsOnly = digitsOnly.substring(2);
        }

        if (digitsOnly.startsWith("0") && digitsOnly.length() == 11) {
            return "98" + digitsOnly.substring(1);
        }

        if (digitsOnly.startsWith("98")) {
            if (digitsOnly.length() == 12) {
                return digitsOnly;
            }
            if (digitsOnly.length() == 14 && digitsOnly.startsWith("9800")) {
                return digitsOnly.substring(2);
            }
        }

        if (digitsOnly.startsWith("9") && digitsOnly.length() == 10) {
            return "98" + digitsOnly;
        }

        return digitsOnly;
    }
}
