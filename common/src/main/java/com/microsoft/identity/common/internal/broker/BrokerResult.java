package com.microsoft.identity.common.internal.broker;

/**
 * Encapsulates the possible responses from the broker.  Both successful response and error response.
 */
public class BrokerResult {

    private BrokerTokenResult mTokenResult;
    private BrokerErrorResult mErrorResult;
    private Boolean mSuccess = false;

    /**
     * Constructor for create successful broker result
     * @param tokenResult
     */
    public BrokerResult(BrokerTokenResult tokenResult){
        mTokenResult = tokenResult;
        mSuccess = true;
        mErrorResult = null;
    }

    /**
     * Constructor for creating an unsuccessful broker response
     * @param errorResult
     */
    public BrokerResult(BrokerErrorResult errorResult){
        mTokenResult = null;
        mSuccess = false;
        mErrorResult = errorResult;
    }

    /**
     * Indicates whether the broker request was successful or not
     * @return
     */
    public Boolean getSucceeded(){
        return mSuccess;
    }

    /**
     * Gets the token result associated with a successful request
     * @return
     */
    public BrokerTokenResult getTokenResult(){
        return mTokenResult;
    }

    /**
     * Gets the error result associated with a failed request
     * @return
     */
    public BrokerErrorResult getErrorResult(){
        return mErrorResult;
    }

}
