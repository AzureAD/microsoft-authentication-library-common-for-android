package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.logging.Logger;

import lombok.Getter;
import lombok.NonNull;

/**
 * Encodes the Android Task ID (taskId) into the state sent as part of the authorization request
 * This allows the response to be correlated to the request.
 */
public class AndroidTaskStateGenerator extends DefaultStateGenerator {

    private static final String SPLITTER = ":";
    private static final String TAG = "AndroidTaskStateGenerator";

    @Getter
    private int taskId;

    public AndroidTaskStateGenerator(@NonNull final int taskId){
        this.taskId = taskId;
    }

    @Override
    public String generate() {
        String state = super.generate();
        state = String.format("%d%s%s", this.taskId, SPLITTER, state);
        return state;
    }

    public static int getTaskFromState(String state){
        String[] parts = state.split(SPLITTER);
        int returnValue = 0;
        if(parts.length >= 2){
            try {
                returnValue = Integer.parseInt(parts[0]);
            }catch(NumberFormatException ex) {
                Logger.error(TAG, "Unable to parse state", ex);
            }
        }
        return returnValue;
    }
}
