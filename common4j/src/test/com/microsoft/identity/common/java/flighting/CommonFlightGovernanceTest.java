// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.

package com.microsoft.identity.common.java.flighting;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies governance metadata for every CommonFlight declaration.
 */
public class CommonFlightGovernanceTest {
    private static final Pattern FLIGHT_PATTERN = Pattern.compile(
            "@FlightMeta\\s*\\(([^)]*)\\)\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(\\\"([^\\\"]+)\\\"");

    @Test
    public void everyCommonFlightHasCompleteGovernanceMetadata() throws IOException {
        String source = readFlightSource();
        Matcher matcher = FLIGHT_PATTERN.matcher(source);
        Map<String, String> metadataByFlight = new HashMap<>();

        while (matcher.find()) {
            String metadata = matcher.group(1);
            String flightName = matcher.group(2);
            String flightKey = matcher.group(3);
            Matcher ownerMatcher = Pattern.compile("owner\\s*=\\s*\\\"([^\\\"]*)\\\"").matcher(metadata);
            assertTrue(ownerMatcher.find());
            assertTrue(ownerMatcher.group(1).trim().length() > 0);
            assertTrue(metadata.matches(".*type\\s*=\\s*FlagType\\.(RELEASE|KILL_SWITCH|CONFIG).*"));
            Matcher legacyMatcher = Pattern.compile("legacy\\s*=\\s*(true|false)").matcher(metadata);
            boolean isLegacy = legacyMatcher.find() && "true".equals(legacyMatcher.group(1));
            Matcher premortemMatcher = Pattern.compile("premortem\\s*=\\s*\\\"([^\\\"]+)\\\"").matcher(metadata);
            if (!isLegacy) {
                assertTrue(premortemMatcher.find());
                String premortem = premortemMatcher.group(1);
                assertTrue(premortem.matches("docs/flight-premortems/[^/]+\\.md"));
                assertTrue(new File(premortem).isFile());
            }
            metadataByFlight.put(flightName, flightKey);
        }

        assertEquals(CommonFlight.values().length, metadataByFlight.size());
        for (CommonFlight flight : CommonFlight.values()) {
            assertEquals(flight.getKey(), metadataByFlight.get(flight.name()));
        }
    }

    @Test
    public void nonLegacyPremortemMustMatchFlightKey() {
        assertTrue(isValidPremortem("EnablePasskeyRegistration", "docs/flight-premortems/EnablePasskeyRegistration.md"));
        assertTrue(!isValidPremortem("EnablePasskeyRegistration", "docs/flight-premortems/OtherFlight.md"));
        assertTrue(!isValidPremortem("EnablePasskeyRegistration", ""));
    }

    private boolean isValidPremortem(String flightKey, String path) {
        return path.matches("docs/flight-premortems/" + Pattern.quote(flightKey) + "\\.md");
    }

    private String readFlightSource() throws IOException {
        File source = new File("src/main/com/microsoft/identity/common/java/flighting/CommonFlight.java");
        return new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
    }
}