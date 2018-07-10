package ng.com.fastpay.fastpay

import android.app.Application
import android.provider.Telephony
import android.content.IntentFilter
import ng.com.fastpay.fastpay.receivers.SmSBroadcastReceiver


/**
 * Created by Emem on 7/10/18.
 */
class App:Application() {
    val smsBroadcastReceiver by lazy {
        SmSBroadcastReceiver()
    }
    override fun onCreate() {
        super.onCreate()
        registerReceiver(smsBroadcastReceiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))

    }

    override fun onTerminate() {
        unregisterReceiver(smsBroadcastReceiver)
        super.onTerminate()
    }
}