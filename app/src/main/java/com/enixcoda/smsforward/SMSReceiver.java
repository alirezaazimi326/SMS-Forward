package com.enixcoda.smsforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.Keep;

/**
 * BroadcastReceiver to handle incoming SMS messages and forward them via various methods.
 */
@Keep
public class SMSReceiver extends BroadcastReceiver {

    /**
     * Called when an SMS message is received.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SMSReceiver", "onReceive: action " + intent.getAction());
        if (!intent.getAction().equals(android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
            return;

        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.w("SMSReceiver", "onReceive: Missing extras bundle");
            return;
        }

        final Object[] pduObjects = (Object[]) bundle.get("pdus");
        if (pduObjects == null) {
            Log.w("SMSReceiver", "onReceive: Missing PDU objects");
            return;
        }

        final String format = bundle.getString("format");

        for (Object messageObj : pduObjects) {
            SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) messageObj, format);
            String senderNumber = currentMessage.getDisplayOriginatingAddress();
            String rawMessageContent = currentMessage.getDisplayMessageBody();
            if (ForwardingConfig.isAllowedSender(senderNumber)) {
                Log.i("SMSReceiver", "onReceive: Forwarding trusted message from " + senderNumber);
                Forwarder.forwardViaSMS(senderNumber, rawMessageContent, ForwardingConfig.getTargetNumber());
            } else {
                Log.d("SMSReceiver", "onReceive: Ignored message from " + senderNumber);
            }
        }
    }
}
