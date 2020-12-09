package com.microsoft.identity.client.ui.automation.sdk;

import com.microsoft.aad.adal.AuthenticationResult;
import android.text.TextUtils;
import org.junit.Assert;
import java.util.Map;
import androidx.annotation.NonNull;
import lombok.Getter;

/**
 * A wrapper class for the result obtained from acquire token interactively
 * or silently, it wraps all the parameters obtained from token and also
 * the exception, with methods to assert success or failures.
 */
@Getter
public class AuthResult {

    private String accessToken;
    private String idToken;
    private String userId;
    private String username;
    private String authority;
    private Exception exception;

    public AuthResult(@NonNull final AuthenticationResult authenticationResult) {
        this.accessToken = authenticationResult.getAccessToken();
        this.idToken = authenticationResult.getIdToken();
        this.userId = authenticationResult.getUserInfo().getUserId();
        this.username = authenticationResult.getUserInfo().getDisplayableId();
        this.authority = authenticationResult.getAuthority();
    }

    public AuthResult(@NonNull final Exception exception) {
        this.exception = exception;
    }

    public void assertSuccess() {
        if (exception != null) {
            throw new AssertionError(exception);
        }
        Assert.assertFalse(TextUtils.isEmpty(accessToken));
        Assert.assertFalse(TextUtils.isEmpty(idToken));
        Assert.assertFalse(TextUtils.isEmpty(userId));
        Assert.assertFalse(TextUtils.isEmpty(username));
//        Assert.assertFalse(TextUtils.isEmpty(authority));
    }

    public void assertFailure() {
        Assert.assertNotNull(exception);
    }
}
