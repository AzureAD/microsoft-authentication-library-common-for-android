package com.microsoft.identity.common.internal.broker;

import com.google.gson.annotations.SerializedName;

public class BrokerRequest {

    private class SerializedNames {
        public final static String AUTHORITY = "account.authority";
        public final static String SCOPE = "account.scope";
        public final static String REDIRECT = "account.redirect";
        public final static String CLIENT_ID = "account.clientid.key";
        public final static String VERSION_KEY = "msal.version.key";
        public final static String USER_ID = "account.userinfo.userid";
        public final static String EXTRA_QUERY_STRING_PARAMETER = "account.extra.query.param";
        public final static String CORRELATION_ID = "account.correlationid";
        public final static String LOGIN_HINT = "account.login.hint";
        public final static String NAME = "account.name";
        public final static String PROMPT = "account.prompt";
        public final static String CLAIMS = "account.claims";
        public final static String FORCE_REFRESH = "force.refresh";
        public final static String APPLICATION_NAME = "application.name";
    }

    @SerializedName(SerializedNames.AUTHORITY)
    private String mAuthority;
    @SerializedName(SerializedNames.SCOPE)
    private String mScope;
    @SerializedName(SerializedNames.REDIRECT)
    private String mRedirect;
    @SerializedName(SerializedNames.CLIENT_ID)
    private String mClientId;
    @SerializedName(SerializedNames.VERSION_KEY)
    private String mVersion;
    @SerializedName(SerializedNames.USER_ID)
    private String mUserId;
    @SerializedName(SerializedNames.EXTRA_QUERY_STRING_PARAMETER)
    private String mExtraQueryStringParameter;
    @SerializedName(SerializedNames.CORRELATION_ID)
    private String mCorrelationId;
    @SerializedName(SerializedNames.LOGIN_HINT)
    private String mLoginHint;
    @SerializedName(SerializedNames.NAME)
    private String mName;
    @SerializedName(SerializedNames.PROMPT)
    private String mPrompt;
    @SerializedName(SerializedNames.CLAIMS)
    private String mClaims;
    @SerializedName(SerializedNames.FORCE_REFRESH)
    private Boolean mForceRefresh;
    @SerializedName(SerializedNames.APPLICATION_NAME)
    private String mApplicationName;

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String authority) {
        this.mAuthority = authority;
    }

    public String getScope() {
        return mScope;
    }

    public void setScope(String authority) {
        this.mScope = authority;
    }

    public String getRedirect() {
        return mRedirect;
    }

    public void setRedirect(String redirect) {
        this.mRedirect = redirect;
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String clientId) {
        this.mClientId = clientId;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        this.mVersion = version;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        this.mUserId = userId;
    }

    public String getExtraQueryStringParameter() {
        return mExtraQueryStringParameter;
    }

    public void setExtraQueryStringParameter(String extraQueryStringParameter) {
        this.mExtraQueryStringParameter = extraQueryStringParameter;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(String correlationId) {
        this.mCorrelationId = correlationId;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public void setLoginHint(String loginHint) {
        this.mLoginHint = loginHint;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getPrompt() {
        return mPrompt;
    }

    public void setPrompt(String prompt) {
        this.mPrompt = prompt;
    }

    public String getClaims() {
        return mClaims;
    }

    public void setClaims(String claims) {
        this.mClaims = claims;
    }

    public Boolean getForceRefresh() {
        return mForceRefresh;
    }

    public void setForceRefresh(Boolean forceRefresh) {
        this.mForceRefresh = forceRefresh;
    }

    public String getApplicationName() {
        return mApplicationName;
    }

    public void setApplicationName(String applicationName) {
        this.mApplicationName = applicationName;
    }


}
