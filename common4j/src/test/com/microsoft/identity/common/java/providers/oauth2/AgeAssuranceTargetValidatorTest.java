// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.identity.common.java.exception.ClientException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class AgeAssuranceTargetValidatorTest {
    public static String claims(final String... properties) {
        final JsonArray values = new JsonArray();
        for (final String property : properties) {
            values.add(property);
        }
        final JsonObject information = new JsonObject();
        information.add("additionalProperties", values);
        final JsonObject idToken = new JsonObject();
        idToken.add("xms_agvs", information);
        final JsonObject request = new JsonObject();
        request.add("id_token", idToken);
        return request.toString();
    }

    public static String token(final Object receipt) {
        return new PlainJWT(new JWTClaimsSet.Builder()
                .subject("test-subject")
                .claim("xms_agvs", 0)
                .claim("xms_agvt", receipt)
                .build()).serialize();
    }

    @Test
    public void noExplicitIdTargetDoesNotInspectToken() throws Exception {
        for (final String request : Arrays.asList(null, "", "{}", "invalid-json",
                claims("other-property"), claims(" target-mc"),
                claims("target-mc").replace("id_token", "access_token"),
                claims("target-mc").replace("id_token", "ID_TOKEN"),
                claims("target-mc").replace("xms_agvs", "XMS_AGVS"),
                claims("target-mc").replace("additionalProperties", "AdditionalProperties"))) {
            AgeAssuranceTargetValidator.validate(request, "not-a-token");
        }
    }

    @Test
    public void signedExactSetIgnoresCaseOrderAndOutcome() throws Exception {
        AgeAssuranceTargetValidator.validate(
                claims("target-mc", "TARGET-SM", "target-MC", "unrelated"),
                token("-sm,+MC"));
        AgeAssuranceTargetValidator.validate(claims("target-mc"), token("-mc"));
    }

    @Test
    public void identitiesRemainOpaqueAndAllIsLiteral() throws Exception {
        AgeAssuranceTargetValidator.validate(
                claims("target-future.v2/é:_id"), token("-FUTURE.V2/É:_ID"));
        AgeAssuranceTargetValidator.validate(claims("target-all"), token("+ALL"));
        assertMismatch(claims("target-all"), token("+mc,+sm"));
        assertMismatch(claims("target-mc"), token("+all"));
    }

    @Test
    public void missingNonStringAndMalformedReceiptsFailEvenForUnknownStatus() throws Exception {
        for (final Object receipt : Arrays.asList(null, 0, true,
                Collections.singletonList("+mc"), Collections.singletonMap("target", "mc"),
                "", "mc", "+", "-", "++mc", "+-mc", "--mc", "-+mc",
                " +mc", "+ mc", "+mc ", "+m c", "+mc,", ",+mc", "+mc,,+sm",
                "+mc,+MC", "+mc,-mc", "+mc,+sm", "+sm", "+mc\n", "+mc\u00a0")) {
            assertMismatch(claims("target-mc"), token(receipt));
        }
        assertMismatch(claims("target-mc", "target-sm"), token("+mc"));
        assertMismatch(claims("target-mc"), null);
        assertMismatch(claims("target-mc"), "not-a-token");
    }

    @Test
    public void malformedExplicitTargetsAreNotSilentlyIgnored() throws Exception {
        for (final String property : Arrays.asList(
                "target-", "target-+mc", "target--mc", "target-mc,sm",
                "target-mc ", "target-m c", "target-\nmc")) {
            assertMismatch(claims(property), token("+mc"));
        }
    }

    @Test
    public void concurrentRequestsDoNotShareTargets() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Callable<Void> mc = () -> {
                for (int i = 0; i < 50; i++) {
                    AgeAssuranceTargetValidator.validate(claims("target-mc"), token("-mc"));
                    assertMismatch(claims("target-mc"), token("+sm"));
                }
                return null;
            };
            final Callable<Void> sm = () -> {
                for (int i = 0; i < 50; i++) {
                    AgeAssuranceTargetValidator.validate(claims("target-sm"), token("+sm"));
                    assertMismatch(claims("target-sm"), token("-mc"));
                }
                return null;
            };
            for (final Future<Void> result : executor.invokeAll(Arrays.asList(mc, sm))) {
                result.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertMismatch(final String request, final String token) throws Exception {
        try {
            AgeAssuranceTargetValidator.validate(request, token);
            fail("Expected target mismatch");
        } catch (final ClientException exception) {
            assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, exception.getErrorCode());
            assertEquals("The ID token age-assurance targets do not match the requested targets.",
                    exception.getMessage());
            assertNull(exception.getCause());
        }
    }
}
