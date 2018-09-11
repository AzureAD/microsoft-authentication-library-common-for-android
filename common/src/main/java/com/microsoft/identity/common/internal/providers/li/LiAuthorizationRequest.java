package com.microsoft.identity.common.internal.providers.li;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;

public class LiAuthorizationRequest extends AuthorizationRequest {


    /**
     * Constructor of AuthorizationRequest.
     *
     * @param builder
     */
    protected LiAuthorizationRequest(Builder builder) {
        super(builder);
        mState = generateEncodedState();
    }

    @Override
    public String getAuthorizationEndpoint() {
        return "https://www.linkedin.com/oauth/v2/authorization";
    }


    public static class Builder extends AuthorizationRequest.Builder<LiAuthorizationRequest.Builder> {


        @Override
        public LiAuthorizationRequest.Builder self() {
            return this;
        }

        public LiAuthorizationRequest build() {
            return new LiAuthorizationRequest(this);
        }
    }

}
