package com.enixcoda.smsforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.Keep;

/**
 * BroadcastReceiver to handle incoming SMS messages and forward them via various methods.
 */
@Keep
public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.w("SMSReceiver", "onReceive: no extras found in intent");
            return;
        }

        final Object[] pduObjects = (Object[]) bundle.get("pdus");
        if (pduObjects == null) {
            Log.w("SMSReceiver", "onReceive: PDU bundle empty");
            return;
        }

        for (Object messageObj : pduObjects) {
            SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) messageObj, (String) bundle.get("format"));
            if (currentMessage == null) {
                continue;
            }

            String senderNumber = currentMessage.getDisplayOriginatingAddress();
            String rawMessageContent = currentMessage.getDisplayMessageBody();

            if (!isAllowedSender(context, senderNumber)) {
                Log.d("SMSReceiver", "onReceive: ignoring message from disallowed sender " + senderNumber);
                continue;
            }

            Log.d("SMSReceiver", "onReceive: forwarding message from " + senderNumber + " to " + ForwardingConfig.TARGET_NUMBER);
            Forwarder.forwardViaSMS(senderNumber, rawMessageContent, ForwardingConfig.TARGET_NUMBER);
        }
    }

    private boolean isAllowedSender(Context context, String senderNumber) {
        if (senderNumber == null) {
            return false;
        }

        for (String allowed : ForwardingConfig.ALLOWED_SENDERS) {
            if (PhoneNumberUtils.compare(context, senderNumber, allowed)) {
                return true;
            }
        }

        return false;
    }
}
