package ng.com.fastpay.fastpay

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import ng.com.fastpay.fastpay.Utils.Constants.REQUEST_PERMISSIONS_REQUEST_CODE
import ng.com.fastpay.fastpay.Utils.Constants.USSD_RECIEVER_KEY
import ng.com.fastpay.fastpay.Utils.showAppSnackbar
import ng.com.fastpay.fastpay.model.IntentResult
import ng.com.fastpay.fastpay.service.USSDService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = MainActivity::class.java.simpleName
    val ussdReceiver = USSDResultReceiver(Handler())
    val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_dial.setOnClickListener {
            dialCode(edittext_code.text.toString(), 0)
        }

        button_dial_2.setOnClickListener {
            dialCode(edittext_code.text.toString(), 1)
        }

        button_random.setOnClickListener {
            sendSMS("I love you sent programmatically")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        if (!isCallPermissionGranted()) {
            requestPermissions()
        }
    }


    override fun onPause() {
        super.onPause()
    }

    private fun dialCode(code: String, simSlot: Int = 0) {
        val intent = Intent(this, USSDService::class.java)
        intent.putExtra(USSD_RECIEVER_KEY, ussdReceiver)
        startService(intent)

        if (code.isEmpty()) {
            textview_log.text = "ussd code is empty"
            return
        }
        val asterisks = code.substring(0, 1)
        val hash = code.substring(code.length - 1, code.length)
        val code = code.substring(1, code.length - 1)
        val encodedHash = Uri.encode(hash)
        val ussd = "$asterisks$code$encodedHash"
        Log.e(TAG, "ussd $ussd")

        if (isCallPermissionGranted())
            if (Build.VERSION.SDK_INT > 25) dialCodeOreo(ussd, simSlot)

            /*else if (Build.VERSION.SDK_INT > 21) {
                *//*val sims = SubscriptionManager.from(this).activeSubscriptionInfoList
                for (subInfo in sims) {
                    val slotIndex = subInfo.simSlotIndex
                    val subscriptionForSlot = subInfo.subscriptionId
                    Log.e("TAG", "sim index  $slotIndex")
                    Log.e("TAG", "sim subscription id $subscriptionForSlot")
                }*//*
            }*/
            else if(Build.VERSION.SDK_INT >= 23) {
                val simSelected = simSlot
                val telecomManager = this.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if(isCallPermissionGranted()){
                    val phoneAccountHandleList = telecomManager?.callCapablePhoneAccounts
                    val intent = Intent(Intent.ACTION_CALL)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.data = Uri.parse("tel:$ussd")
                    intent.putExtra("com.android.phone.force.slot", true)
                    if (simSelected == 0) {   //0 for sim1
                        intent.putExtra("com.android.phone.extra.slot", 0) //0 or 1 according to sim.......
                        if (phoneAccountHandleList != null && phoneAccountHandleList.size > 0)
                            intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandleList.get(0))
                    } else {    //0 for sim1
                        intent.putExtra("com.android.phone.extra.slot", 1); //0 or 1 according to sim.......
                        if (phoneAccountHandleList != null && phoneAccountHandleList.size > 1)
                            intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandleList.get(1))
                    }
                    startActivityForResult(intent, 1)
                } else {
                    val intent = Intent(Intent.ACTION_CALL)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.data = Uri.parse("tel:$ussd")
                    startActivityForResult(intent, 1)
                }

            }
        else requestPermissions()
    }

    @RequiresApi(26)
    private fun dialCodeOreo(ussd: String, simSlot: Int) {
        Log.e(TAG, "about to dial code oreo")
        requestUssdUsingTelephonyManager(ussd, simSlot)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
        // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_PERMISSIONS_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {

                }
                Activity.RESULT_CANCELED -> {
                    //Log.i(TAG, "User chose not to make required location settings changes.");
                    showAppSnackbar("Call permission turned off", Snackbar.LENGTH_INDEFINITE)
                }
            }// Log.i(TAG, "User agreed to make required location settings changes.");
        }

    }

    private fun isCallPermissionGranted(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun isTelephonyPermissionGranted(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun isSmsPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CALL_PHONE)

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            //Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(getString(R.string.permission_call_phone), android.R.string.ok, View.OnClickListener {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE
                                , Manifest.permission.READ_SMS),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            })
        } else {
            //Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE
                            , Manifest.permission.READ_SMS),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun showSnackbar(mainTextStringId: String, actionStringId: Int,
                             listener: View.OnClickListener) {
        showAppSnackbar(mainTextStringId, Snackbar.LENGTH_INDEFINITE, getString(actionStringId), listener)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.e(TAG, "text")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doThis(intentServiceResult: IntentResult) {
        textview_log.text = intentServiceResult.message
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }


    @RequiresApi(26)
    fun requestUssdUsingTelephonyManager(ussd: String, simSlot: Int) {
        Log.e("ussd", "requesting for ussd $ussd")
        val manager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val handler = Handler()
        val callback = object : TelephonyManager.UssdResponseCallback() {
            override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                super.onReceiveUssdResponse(telephonyManager, request, response) // what if i remove this line
                EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, response.toString()))
                textview_log.text = "Success with response : $response \n request $request"
                Log.e("ussd", "Success with response : $response \n request $request")
            }

            override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode) // what if i remove this line
                EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Failed with code $failureCode"))
                textview_log.text = "failed with code " + Integer.toString(failureCode) + "\n request " + request
                Log.e("ussd", "failed with code " + Integer.toString(failureCode) + "\n request " + request)
            }

        }

        EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Starting ... "))

        try {
            textview_log.text = "Requesting" + manager.networkOperatorName
            //EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Requesting" + manager.networkOperatorName))
            manager.sendUssdRequest(ussd, callback, handler)
            textview_log.text = "Requesting ... ..." + manager.networkOperatorName
            //EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Requesting ... ... " + manager.networkOperatorName))
            //requesting ussd for sim one only
            /*val sims = SubscriptionManager.from(this).activeSubscriptionInfoList
            if (sims == null) {
                EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "No SIM card detected"))
            }
            if (sims != null) {
                for (subInfo in sims) {
                    val slotIndex = subInfo.simSlotIndex
                    if (simSlot == slotIndex && simSlot == 1) {
                        EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Requesting for SIM 1..."))
                        val subscriptionForSlot = subInfo.subscriptionId
                        Log.e("TAG", "sim subscription id $subscriptionForSlot")
                        val telephonyManager = manager.createForSubscriptionId(subscriptionForSlot)
                        try {
                            EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Sending USSD"))
                            telephonyManager.sendUssdRequest(ussd, callback, handler)
                            Log.e("ussd", "sending ussd successful")
                        } catch (e: SecurityException) {
                            EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Permission denied "))
                            e.printStackTrace()
                        }

                    } else if (simSlot == slotIndex && simSlot == 2) {
                        EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Requesting for SIM 12 ..."))
                        val subscriptionForSlot = subInfo.subscriptionId
                        Log.e("TAG", "sim subscription id $subscriptionForSlot")

                        val telephonyManager = manager.createForSubscriptionId(subscriptionForSlot)
                        try {
                            EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Sending USSD"))
                            telephonyManager.sendUssdRequest(ussd, callback, handler)
                            Log.e("ussd", "sending ussd successful")
                        } catch (e: SecurityException) {
                            EventBus.getDefault().post(IntentResult(Activity.RESULT_OK, "Permission denied "))
                            e.printStackTrace()
                        }

                    }
                }
            }*/

        } catch (e: SecurityException) {
            EventBus.getDefault().post(IntentResult(Activity.RESULT_OK,
                    "Permission failed"))
            Log.e("ussd", "permission failed")
            e.printStackTrace()
        }


    }

    fun sendSMS(message: String) {
        SmsManager.getDefault().sendTextMessage("08141549102", null, message, null, null)

    }

    inner class USSDResultReceiver(handler: Handler) : ResultReceiver(handler) {

        override fun onReceiveResult(status: Int, resultData: Bundle?) {
            if (resultData == null) {
                return
            } else {

            }

        }
    }

}
