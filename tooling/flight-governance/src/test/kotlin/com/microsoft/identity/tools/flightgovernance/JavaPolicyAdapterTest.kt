// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class JavaPolicyAdapterTest {
    @Test
    fun metadataAndCommentsAreSeparatedFromRuntimeShape() {
        val plain = parse("", "FEATURE(\"Example\", false);")
        val annotated = parse(
            "import com.microsoft.identity.common.java.flighting.FlightMeta;\n" +
                "import com.microsoft.identity.common.java.flighting.FlagType;",
            """
                /** Owner-reviewed existing feature. */
                @FlightMeta(owner = "team", type = FlagType.RELEASE, premortem = "docs/example.md")
                FEATURE(
                    "Example",
                    false);
            """.trimIndent()
        )
        assertEquals(plain.runtimeShape, annotated.runtimeShape)
        assertEquals(PolicyMetadata("team", "RELEASE", "docs/example.md"),
            annotated.declarations.single().metadata)
        assertTrue(plain.declarations.single().sameBehavior(annotated.declarations.single()))
        assertNotEquals(plain.runtimeShape, parse("", "FEATURE(\"Example\", true);").runtimeShape)
    }

    @Test
    fun symbolicMetadata_isReportedInsteadOfGuessed() {
        val input = parse("import com.microsoft.identity.common.java.flighting.*;", """
            @FlightMeta(owner = Owners.CURRENT, type = FlagType.CONFIG)
            FEATURE("Example", false);
        """.trimIndent())
        assertEquals(setOf("owner"), input.declarations.single().metadata?.invalidArguments)
        assertEquals("", input.declarations.single().metadata?.owner)
    }

    @Test
    fun unrelatedAnnotation_doesNotImpersonateTheMetadataContract() {
        val input = parse("", """
            @FlightMeta(owner = "unrelated", type = FlagType.RELEASE)
            FEATURE("Example", false);
        """.trimIndent())
        assertEquals(null, input.declarations.single().metadata)
    }

    private fun parse(imports: String, constants: String) = JavaPolicyAdapter().parse(
        FlightSource("app", "a".repeat(40), "Flights.java", """
            package example;
            $imports
            enum Flights implements IFlightConfig {
                $constants
                Flights(String key, Object value) {}
            }
        """.trimIndent())
    )
}
