package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class OpenIdConnectPromptParameterTest  {

    @Test
    public void testPromptBehaviorNull(){
       final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior(null);
        assertEquals(promptParameter, OpenIdConnectPromptParameter.NONE);
    }

    @Test
    public void testPromptBehaviorAuto(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("Auto");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.NONE);
    }

    @Test
    public void testPromptBehaviorAlways(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("Always");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.NONE);
    }

    @Test
    public void testPromptBehaviorRefreshSession(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("REFRESH_SESSION");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.NONE);
    }

    @Test
    public void testPromptBehaviorForcePrompt(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("FORCE_PROMPT");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.LOGIN);
    }
}
