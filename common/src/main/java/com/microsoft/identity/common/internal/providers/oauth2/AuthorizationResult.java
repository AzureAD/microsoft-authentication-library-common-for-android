package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * A class to return the result of the authorization request to the calling code (ADAL or MSAL Controller classes)
 * This class should have a generic status in terms of : Cancelled, TimedOut, Error,  etc...
 * this class should also contain the AuthorizationResponse which contains the details returned from the
 * In the case of an error/exception this class should return the associated exception
 */
public abstract class AuthorizationResult {
}
