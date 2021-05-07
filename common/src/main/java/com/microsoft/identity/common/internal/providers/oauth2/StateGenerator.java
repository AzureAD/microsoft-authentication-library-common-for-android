package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * Abstract base class for state generation for authorization requests
 *
 * In the case of MSAL we will include the task id in the generated state in order to be able to correlate
 * The response to the original request in the command dispatcher.  This class will just deal with generating the state from provided set of parameters
 * It will not encode/decode
 */
public abstract class StateGenerator {

    public abstract String generate();

}
