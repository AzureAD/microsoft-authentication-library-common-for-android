// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class FlightInventoryTest {
    private fun source(body: String, label: String = "fixture") = FlightSource(
        label,
        "a".repeat(40),
        "Features.java",
        """
            package example;
            import org.json.JSONObject;
            enum Features implements IFlightConfig {
                $body
                Features(String key, Object value) {}
            }
        """.trimIndent()
    )

    @Test
    fun parser_preservesMixedCaseMultilineTypedValuesAndAnnotations() {
        val input = source("""
            @FlightMeta(owner = "owning-team", type = FlagType.RELEASE,
                premortem = "docs/premortem/example.md")
            FEATURE_v2(
                "ExampleFeature",
                false),
            LIMIT("Limit", -17),
            LARGE("Large", 9L),
            RATE("Rate", 0.25),
            TEXT("Text", "quoted\\value"),
            JSON("Json", new JSONObject("{\"nested\":{\"enabled\":true}}")),
            UNKNOWN("Unknown", Defaults.TIMEOUT),
            NESTED("Nested", new JSONObject(makeConfig("a,b", 3)));
        """.trimIndent())
        val records = FlightSourceParser().parse(input)
        assertEquals(8, records.size)
        val feature = records.first()
        assertEquals("FEATURE_v2", feature.constant)
        assertEquals("ExampleFeature", feature.key)
        assertEquals(DefaultType.BOOLEAN, feature.default.type)
        assertEquals(false, feature.default.value)
        assertTrue(feature.line > 0)
        assertEquals("\"owning-team\"", feature.annotations.single().arguments["owner"])
        assertEquals("FlagType.RELEASE", feature.annotations.single().arguments["type"])
        assertEquals(-17, records[1].default.value)
        assertEquals(DefaultType.LONG, records[2].default.type)
        assertEquals(9L, records[2].default.value)
        assertEquals(0.25, records[3].default.value)
        assertEquals("quoted\\value", records[4].default.value)
        val json = records[5].default.value
        assertTrue(json is JSONObject)
        assertTrue((json as JSONObject).getJSONObject("nested").getBoolean("enabled"))
        assertEquals(DefaultType.UNRESOLVED, records[6].default.type)
        assertEquals("Defaults.TIMEOUT", records[6].default.expression)
        assertEquals(DefaultType.UNRESOLVED, records[7].default.type)
        assertTrue(records[7].default.expression.contains("makeConfig(\"a,b\", 3)"))
    }

    @Test
    fun snapshot_keepsSharedKeysAndAuditsExactExclusions() {
        val first = source("""
            ACTIVE("Shared", true),
            TEST_ONLY("TestOnly", false);
        """.trimIndent())
        val second = source("ACTIVE(\"Shared\", false);", "other")
        val excludedId = "fixture:example.Features#TEST_ONLY"
        val snapshot = createSnapshot(listOf(first, second), setOf(excludedId))
        val summary = snapshot.getJSONObject("summary")
        assertEquals(2, summary.getInt("declarationCount"))
        assertEquals(1, summary.getInt("uniqueKeyCount"))
        assertEquals(1, summary.getInt("excludedCount"))
        val declarations = snapshot.getJSONArray("declarations")
        assertNotEquals(
            declarations.getJSONObject(0).getJSONObject("default").getBoolean("value"),
            declarations.getJSONObject(1).getJSONObject("default").getBoolean("value")
        )
        assertEquals(excludedId, snapshot.getJSONArray("excludedDeclarations").getJSONObject(0).getString("id"))
        assertEquals(
            canonicalJson(snapshot),
            canonicalJson(createSnapshot(listOf(second, first), setOf(excludedId)))
        )
        expectInvalid { createSnapshot(listOf(first), setOf("fixture:example.Features#MISSING")) }
        expectInvalid { createSnapshot(listOf(first, first), emptySet()) }
    }

    @Test
    fun parser_rejectsMalformedOrUnsupportedDeclarations() {
        expectInvalid { FlightSourceParser().parse(source("BROKEN(\"key\", true;")) }
        expectInvalid { FlightSourceParser().parse(source("KEY(Constants.KEY, true);")) }
        expectInvalid { FlightSourceParser().parse(source("KEY(\"key\", true, 3);")) }
        expectInvalid {
            FlightSourceParser().parse(source("KEY(\"key\", true);").copy(content = "class NotAnEnum {}"))
        }
    }

    @Test
    fun parser_marksInvalidJsonAndSymbolicUnaryValuesUnresolved() {
        val records = FlightSourceParser().parse(source("""
            JSON("Json", new JSONObject("not-json")),
            SYMBOLIC("Symbolic", -Defaults.TIMEOUT);
        """.trimIndent()))
        assertTrue(records.all { it.default.type == DefaultType.UNRESOLVED })
        assertTrue(records.all { !it.default.unresolvedReason.isNullOrBlank() })
    }

    @Test
    fun parser_normalizesFormattingWithoutLosingOriginalExpression() {
        val record = FlightSourceParser().parse(source("LIMIT(\"Limit\", 1_000);")).single()
        assertEquals("1_000", record.default.expression)
        assertEquals("1000", record.default.normalizedExpression)
        assertEquals(1000, record.default.value)
    }

    private fun expectInvalid(action: () -> Unit) {
        val failure = runCatching(action).exceptionOrNull()
        assertTrue("Expected an explicit invalid-input failure, got $failure", failure is IllegalArgumentException)
    }
}
