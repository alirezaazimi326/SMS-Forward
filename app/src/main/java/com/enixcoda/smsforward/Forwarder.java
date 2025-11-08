package com.enixcoda.smsforward;

import android.telephony.SmsManager;
import android.util.Log;

/**
 * Sends SMS messages to the configured forwarding target.
 */
public final class Forwarder {
    private static final int MAX_SMS_LENGTH = 120;

    private Forwarder() {
        // Utility class
    }

    public static void sendSMS(String number, String content) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, content, null, null);
    }

    public static void forwardViaSMS(String senderNumber, String forwardContent, String forwardNumber) {
        String forwardPrefix = String.format("From %s:\n", senderNumber);

        try {
            if ((forwardPrefix + forwardContent).getBytes().length > MAX_SMS_LENGTH) {
                sendSMS(forwardNumber, forwardPrefix);
                sendSMS(forwardNumber, forwardContent);
            } else {
                sendSMS(forwardNumber, forwardPrefix + forwardContent);
            }
        } catch (RuntimeException e) {
            Log.w(Forwarder.class.toString(), e.toString());
        }
    }
}
