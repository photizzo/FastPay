package ng.com.fastpay.fastpay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.content.edit
import ng.com.fastpay.fastpay.Utils.Constants.USSD_TEXT
import java.util.*
import android.app.Activity
import ng.com.fastpay.fastpay.model.IntentResult
import org.greenrobot.eventbus.EventBus




/**
 * Created by Emem on 7/4/18.
 */
class USSDService:AccessibilityService() {
    val TAG = USSDService::class.java.simpleName


    override fun onInterrupt() {
        Log.e(TAG, "on interrupt ")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.e(TAG, "onAccessibilityEvent ${ event?.className}")
        Log.e(TAG, "onAccessibilityEvent type ${ event?.eventType}")
        Log.e(TAG, "onAccessibilityEvent source ${ event?.source}")

        val source = event!!.source
        Log.e(TAG, "source class name ${source.className}")

        /* if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !event.getClassName().equals("android.app.AlertDialog")) { // android.app.AlertDialog is the standard but not for all phones  */
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !(event.className).contains("AlertDialog")) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && (source == null || source.className != "android.widget.TextView")) {
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && TextUtils.isEmpty(source!!.text)) {
            return
        }

        val eventText: List<CharSequence>
        eventText = if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event!!.text
        } else {
            Collections.singletonList(source!!.text)
        }

        val text = processUSSDText(eventText)

        if (TextUtils.isEmpty(text)) return

        EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, text!!))
        Log.e(TAG, text)
        // Close dialog
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) // This works on 4.1+ only
        // Handle USSD response here


    }

    private fun processUSSDText(eventText: List<CharSequence>): String? {
        for (s in eventText) {
            val text = s.toString()
            // Return text if text is the expected ussd response
            if (true) {
                return text
            }
        }
        return null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e(TAG, "onServiceConnected")
        EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "onServiceConnected"))
        val info = AccessibilityServiceInfo()
        info.flags = AccessibilityServiceInfo.DEFAULT
        info.packageNames = arrayOf("com.android.phone")
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        Log.e(TAG, "service started")
    }
}