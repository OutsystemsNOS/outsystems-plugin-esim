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
import org.apache.cordova.LOG;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.IntentSender.SendIntentException;

import android.util.Log;


public class eSIM extends CordovaPlugin {

    Integer resultCode;
    Integer operationCode;
    Integer errorCode;
    String smdxReason;
    String smdxSubject;
    Intent resultIntent;
    final String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
    final String ACTION_USER_RESOLUTION = "user_resolution";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("eSimAdd")) {
            String activationCode = args.getString(0);
            if (activationCode != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    this.eSimAdd(activationCode, callbackContext);
                } else {
                    callbackContext.error("{\"ErrorCode\" : 4, \"ErrorMessage\" : \"Android version should be greater or equal to 9\"}");
                }
            } else {
                callbackContext.error("{\"ErrorCode\" : 5, \"ErrorMessage\" : \"Invalid input parameters\"}");
            }
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void eSimAdd(String activationCode, CallbackContext callbackContext) {
        EuiccManager mgr = (EuiccManager) this.cordova.getContext().getSystemService(Context.EUICC_SERVICE);
        if (!mgr.isEnabled()) {
            callbackContext.error("{\"ErrorCode\" : 3, \"ErrorMessage\" : \"Device is not eSIM compatible\"}");
            return;
        } else {
            // Register receiver.
            final String LPA_DECLARED_PERMISSION = cordova.getActivity().getApplicationContext().getPackageName() + ".lpa.permission.BROADCAST";
            
            Log.d("ESimPluginLog", LPA_DECLARED_PERMISSION);
            
            BroadcastReceiver receiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if(ACTION_USER_RESOLUTION.equals(intent.getAction())){
                                callbackContext.success("OK, eSIM profile received successfully, User Interaction was required to download profile!");
                                return;
                            }else if (!ACTION_DOWNLOAD_SUBSCRIPTION.equals(intent.getAction())) {
                                return;
                            }
                            resultCode = getResultCode();
                            resultIntent = intent;
                            
                            Log.d("ESimPluginLog", ""+resultCode);
                            switch(resultCode){
                                default:
                                    callbackContext.success("OK, eSIM profile received successfully!");
                                    break;
                                case 1:
                                    try{
                                        Intent nIntent = new Intent(ACTION_USER_RESOLUTION);
                                        PendingIntent callbackIntent2 = PendingIntent.getBroadcast(
                                                cordova.getContext(), 1 /* requestCode */, nIntent,
                                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                                        mgr.startResolutionActivity(cordova.getActivity(),1,resultIntent,callbackIntent2);
                                    }catch(SendIntentException e){
                                        LOG.e("ESimPluginLog", "Failed to start User resolution Activity", e);
                                    }
                                    break;
                                case 2:
                                    operationCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_OPERATION_CODE,0 /* defaultValue*/);
                                    errorCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE,0 /* defaultValue*/);
                                    Log.d("ESimPluginLog", ""+operationCode);
                                    Log.d("ESimPluginLog", ""+errorCode);
                                    
                                    String operation;
                                    String error = "";
                                    switch(operationCode){
                                        default:
                                            operation = "Internal system";
                                            break;
                                        case 8:
                                            operation = "Failing to execute an APDU command";
                                            break;
                                        case 5:
                                            operation = "Download profile";
                                            break;
                                        case 3:
                                            operation = "eUICC card";
                                            break;
                                        case 7:
                                            operation = "eUICC returned an error defined in GSMA (SGP.22 v2.2) while running one of the ES10x functions";
                                            break;
                                        case 11:
                                            operation = "HTTP";
                                            break;
                                        case 6:
                                            operation = "Subscription's metadata";
                                            break;
                                        case 2:
                                            operation = "SIM slot failed to switch slot, failed to access the physical slot etc";
                                            break;
                                        case 9:
                                            operation = "SMDX(SMDP/SMDS)";
                                            break;
                                        case 10:
                                            operation = "GSMA";
                                            Log.d("ESimPluginLog", smdxReason);
                                            Log.d("ESimPluginLog", smdxSubject);
                                            smdxReason = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE);
                                            smdxSubject = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE);
                                            error = ",Reason:"+smdxReason+",Subject:"+smdxSubject;
                                            break;
                                        case 4:
                                            operation = "Generic switching profile";
                                            break;
                                    }
                                    
                                    switch(errorCode){
                                        default:
                                            break;
                                        case 10011:
                                            error = "Address is missing e.g SMDS/SMDP address is missing"+error;
                                            break;
                                        case 10000:
                                            error = "Operation such as downloading/switching to another profile failed due to device being carrier locked"+error;
                                            break;
                                        case 10012:
                                            error = "Certificate needed for authentication is not valid or missing. E.g SMDP/SMDS authentication failed"+error;
                                            break;
                                        case 10014:
                                            error = "Failure to create a connection"+error;
                                            break;
                                        case 10010:
                                            error = "Failed to load profile onto eUICC due to Profile Policy Rules"+error;
                                            break;
                                        case 10004:
                                            error = "There is no more space available on the eUICC for new profiles"+error;
                                            break;
                                        case 10006:
                                            error = "eUICC is missing or defective on the device"+error;
                                            break;
                                        case 10003:
                                            error = "The profile's carrier is incompatible with the LPA"+error;
                                            break;
                                        case 10009:
                                            error = "Failure to load the profile onto the eUICC card"+error;
                                            break;
                                        case 10001:
                                            error = "The activation code is invalid"+error;
                                            break;
                                        case 10002:
                                            error = "The confirmation code is invalid"+error;
                                            break;
                                        case 10017:
                                            error = "Failure due to target port is not supported"+error;
                                            break;
                                        case 10015:
                                            error = "Response format is invalid. e.g SMDP/SMDS response contains invalid json, header or/and ASN1"+error;
                                            break;
                                        case 10013:
                                            error = "No profiles available"+error;
                                            break;
                                        case 10016:
                                            error = "The operation is currently busy, try again later"+error;
                                            break;
                                        case 10008:
                                            error = "No SIM card is available in the device"+error;
                                            break;
                                        case 10007:
                                            error = "The eUICC card(hardware) version is incompatible with the software"+error;
                                            break;
                                        case 10005:
                                            error = "Timed out while waiting for an operation to complete. i.e restart, disable, switch reset etc"+error;
                                            break;
                                    }
                                    
                                    callbackContext.error("{\"ErrorCode\" : 7, \"ErrorMessage\" : \"Operation:"+operation+",Error:"+error+"\"}");
                                    break;
                                
                            }
                        }
                    };
//            cordova.getContext().registerReceiver(receiver,
//                    new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION),
//                    "" /* broadcastPermission*/,
//                    null /* handler */);
            IntentFilter downloadAndResolution = new IntentFilter();
            downloadAndResolution.addAction(ACTION_DOWNLOAD_SUBSCRIPTION);
            downloadAndResolution.addAction(ACTION_USER_RESOLUTION);
            cordova.getContext().registerReceiver(receiver,
                                downloadAndResolution);

            // Download subscription asynchronously.
            DownloadableSubscription sub = DownloadableSubscription
                    .forActivationCode(activationCode /* encodedActivationCode*/);
            Intent intent = new Intent(ACTION_DOWNLOAD_SUBSCRIPTION);
            PendingIntent callbackIntent = PendingIntent.getBroadcast(
                    cordova.getContext(), 0 /* requestCode */, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            mgr.downloadSubscription(sub, true /* switchAfterDownload */,
                    callbackIntent);
        }
    }
}
