package ng.com.fastpay.fastpay.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat.getExtras
import android.os.Bundle
import android.provider.Telephony
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import ng.com.fastpay.fastpay.model.IntentResult
import org.greenrobot.eventbus.EventBus


/**
 * Created by Emem on 7/10/18.
 */
class SmSBroadcastReceiver:BroadcastReceiver() {

    val TAG = SmSBroadcastReceiver::class.java.simpleName
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "SMS action recieved" ))
            Log.e(TAG, "SMS action received")
            var smsSender = ""
            var smsBody = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsSender = smsMessage.displayOriginatingAddress
                    smsBody += smsMessage.messageBody
                }
            } else {
                val smsBundle = intent?.extras
                if (smsBundle != null) {
                    val pdus = smsBundle.get("pdus") as Array<Any>
                    if (pdus == null) {
                        // Display some error to the user
                        EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "SmsBundle had no pdus key " ))
                        Log.e(TAG, "SmsBundle had no pdus key")
                        return
                    }
                    val messages = arrayOfNulls<SmsMessage>(pdus.size)
                    for (i in messages.indices) {
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        smsBody += messages[i]?.messageBody
                    }
                    smsSender = messages[0]!!.originatingAddress
                }
            }

            EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "$smsSender -  $smsBody"))

        }
    }
}