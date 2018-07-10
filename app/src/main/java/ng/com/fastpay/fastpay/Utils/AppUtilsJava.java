package ng.com.fastpay.fastpay.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import ng.com.fastpay.fastpay.model.IntentResult;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by Emem on 7/9/18.
 */

public class AppUtilsJava {

    @RequiresApi(26)
    public static void requestUssdUsingTelephonyManager(Context context, String ussd, int simSlot) {
        Log.e("ussd", "requesting for ussd " + ussd);
        TelephonyManager manager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        Handler handler = new Handler();/*Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Log.e("ussd ", message.toString());
            }
        };*/

        TelephonyManager.UssdResponseCallback callback = new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                super.onReceiveUssdResponse(telephonyManager, request, response); // what if i remove this line
                EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, response.toString()));
                Log.e("ussd", "Success with response : " + response + " \n request " + request);
            }

            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode); // what if i remove this line
                EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Failed with code " +failureCode));
                Log.e("ussd", "failed with code " + Integer.toString(failureCode) + "\n request " + request);
            }
        };

        try {
            EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Requesting ... "
            + manager.getNetworkOperatorName()));

            if(simSlot == 2) {
                manager.sendUssdRequest(ussd, callback, handler);

                return;
            }


            //requesting ussd for sim one only
            List<SubscriptionInfo> sims = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
            if(sims == null){
                EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "No SIM card detected"));
            }
            if(sims != null){
                for(SubscriptionInfo subInfo : sims){
                    int slotIndex = subInfo.getSimSlotIndex();
                    if(simSlot ==  slotIndex && simSlot == 1){
                        EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Requesting for SIM 1..."));
                        int subscriptionForSlot = subInfo.getSubscriptionId();
                        Log.e("TAG", "sim subscription id " + subscriptionForSlot);
                        TelephonyManager telephonyManager = manager.createForSubscriptionId(subscriptionForSlot);
                        try {
                            EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Sending USSD"));
                            telephonyManager.sendUssdRequest(ussd, callback, handler);
                            Log.e("ussd", "sending ussd successful");
                        } catch (SecurityException e) {
                            EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Permission denied "));
                            e.printStackTrace();
                        }
                    }
                    else if(simSlot ==  slotIndex && simSlot == 2){
                        EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Requesting for SIM 12 ..."));
                        int subscriptionForSlot = subInfo.getSubscriptionId();
                        Log.e("TAG", "sim subscription id " + subscriptionForSlot);

                        TelephonyManager telephonyManager = manager.createForSubscriptionId(subscriptionForSlot);
                        try {
                            EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Sending USSD"));
                            telephonyManager.sendUssdRequest(ussd, callback, handler);
                            Log.e("ussd", "sending ussd successful");
                        } catch (SecurityException e) {
                            EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK, "Permission denied "));
                            e.printStackTrace();
                        }
                    }
                }
            }

        } catch (SecurityException e) {
            EventBus.getDefault().post(new IntentResult(Activity.RESULT_OK,
                    "Permission failed"));
            Log.e("ussd", "permission failed");
            e.printStackTrace();
        }

        /*//why use this method by creating a subscription id
        TelephonyManager telephonyManager = manager.createForSubscriptionId(101);
        try {
            Log.e("ussd", "sending ussd ");
            telephonyManager.sendUssdRequest(ussd, callback, handler);
            Log.e("ussd", "sending ussd successful");
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        List<SubscriptionInfo> sims = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if(sims != null){
            for(SubscriptionInfo subInfo : sims){
                int slotIndex = subInfo.getSimSlotIndex();
                int subscriptionForSlot = subInfo.getSubscriptionId();

                Log.e("TAG", "sim subscription id " + subscriptionForSlot);

            }
        }*/


    }


}
