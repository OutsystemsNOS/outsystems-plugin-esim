package com.outsystems;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;
import androidx.annotation.RequiresApi;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;


public class eSIM extends CordovaPlugin {

    Integer resultCode;
    Integer detailedCode;
    Intent resultIntent;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("eSimAdd")) {
            String activationCode = args.getString(0);
            if (activationCode != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    this.eSimAdd(activationCode, callbackContext);
                } else {
                    callbackContext.error("Error: Android version should be greater or equal to 9");
                }
            } else {
                callbackContext.error("Error: Invalid input parameters");
            }
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void eSimAdd(String activationCode, CallbackContext callbackContext) {
        EuiccManager mgr = (EuiccManager) this.cordova.getContext().getSystemService(Context.EUICC_SERVICE);
        boolean isEnabled = mgr.isEnabled();
        if (!isEnabled) {
            callbackContext.error("Error: Device is not eSIM compatible");
            return;
        } else {
            // Register receiver.
            final String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
            final String LPA_DECLARED_PERMISSION = cordova.getContext().getPackageName() + ".lpa.permission.BROADCAST";
            BroadcastReceiver receiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (!ACTION_DOWNLOAD_SUBSCRIPTION.equals(intent.getAction())) {
                                return;
                            }
                            resultCode = getResultCode();
                            detailedCode = intent.getIntExtra(
                                    EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE,
                                    0 /* defaultValue*/);
                            resultIntent = intent;
                            //Missing check the resultCode!
                            callbackContext.success("eSIM compatible device!");
                        }
                    };
//            cordova.getContext().registerReceiver(receiver,
//                    new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION),
//                    "" /* broadcastPermission*/,
//                    null /* handler */);
            cordova.getContext().registerReceiver(receiver,
                                new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION),
                                LPA_DECLARED_PERMISSION /* broadcastPermission*/,
                                null /* handler */);

            // Download subscription asynchronously.
            DownloadableSubscription sub = DownloadableSubscription
                    .forActivationCode(activationCode /* encodedActivationCode*/);
            Intent intent = new Intent(ACTION_DOWNLOAD_SUBSCRIPTION);
            PendingIntent callbackIntent = PendingIntent.getBroadcast(
                    cordova.getContext(), 0 /* requestCode */, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mgr.downloadSubscription(sub, true /* switchAfterDownload */,
                    callbackIntent);
        }
    }
}