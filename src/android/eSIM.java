package com.outsystems;

import android.content.Context;
import android.os.Build;
import android.telephony.euicc.EuiccManager;

import androidx.annotation.RequiresApi;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class eSIM extends CordovaPlugin {
    //private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("eSimAdd")) {
            String esimMatchingID = args.getString(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                this.eSimAdd(esimMatchingID, callbackContext);
            } else {
                callbackContext.error("Error: Android version should be greater or equal to 9");
            }
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void eSimAdd(String esimMatchingID, CallbackContext callbackContext) {
        EuiccManager mgr = (EuiccManager) this.cordova.getContext().getSystemService(Context.EUICC_SERVICE);
        boolean isEnabled = mgr.isEnabled();
        if (!isEnabled) {
            callbackContext.success("eSIM compatible device!");
            return;
        } else {
            callbackContext.error("Error: Device is not eSIM compatible");
        }

    }
}
