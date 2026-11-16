//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers.responses.v2

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.microsoft.identity.common.java.exception.ClientException

/**
 * A single `_links` entry. HAL allows a relation to carry either one link object or an array of
 * link objects; both shapes are normalized to a list by [HalResource].
 */
internal data class HalLink(
    val href: String,
    val name: String? = null,
    val templated: Boolean = false
)

/**
 * Generic HAL+JSON splitter. Splits a JSON object into its plain properties, its `_links`
 * (relation -> one or more [HalLink]), and its `_embedded`
 * (relation -> one or more nested resources, kept as raw property maps so that [embeddedResources]
 * can re-split them on demand).
 */
internal class HalResource private constructor(
    val properties: Map<String, Any?>,
    val links: Map<String, List<HalLink>>,
    val embedded: Map<String, List<Map<String, Any?>>>
) {
    /**
     * Returns the property [key] as a [String], or `null` if absent or not a string.
     */
    fun string(key: String): String? = properties[key] as? String

    /**
     * Returns the property [key] as an [Int], or `null` if absent or not a whole number.
     */
    fun int(key: String): Int? = when (val value = properties[key]) {
        is Int -> value
        is Number -> runCatching {
            java.math.BigDecimal(value.toString()).intValueExact()
        }.getOrNull()
        else -> null
    }

    /**
     * Splits every embedded resource under relation [rel] into its own [HalResource], recursively
     * applying the same `_links`/`_embedded` split.
     */
    fun embeddedResources(rel: String): List<HalResource> = embedded[rel].orEmpty().map { split(it) }

    /**
     * Returns the href of the first link for relation [rel], or `null` if the relation is absent.
     */
    fun href(rel: String): String? = links[rel]?.firstOrNull()?.href

    companion object {
        private const val LINKS_KEY = "_links"
        private const val EMBEDDED_KEY = "_embedded"
        private const val CURIES_RELATION = "curies"
        private const val HREF_KEY = "href"
        private const val NAME_KEY = "name"
        private const val TEMPLATED_KEY = "templated"

        /**
         * Parses [json] into a [HalResource].
         */
        @Throws(ClientException::class)
        fun from(json: String): HalResource {
            if (json.isBlank()) {
                throw ClientException(
                    ClientException.JSON_PARSE_FAILURE,
                    "Native Auth V2 HAL body must not be blank."
                )
            }

            val element = try {
                JsonParser.parseString(json)
            } catch (e: JsonParseException) {
                throw ClientException(
                    ClientException.JSON_PARSE_FAILURE,
                    "Native Auth V2 HAL body is not valid JSON.",
                    e
                )
            }

            if (!element.isJsonObject) {
                throw ClientException(
                    ClientException.JSON_PARSE_FAILURE,
                    "Native Auth V2 HAL body must be a JSON object."
                )
            }

            @Suppress("UNCHECKED_CAST")
            val rawObject = toKotlinValue(element) as Map<String, Any?>
            return split(rawObject)
        }

        private fun split(map: Map<String, Any?>): HalResource {
            val properties = LinkedHashMap<String, Any?>()
            map.forEach { (key, value) ->
                if (key != LINKS_KEY && key != EMBEDDED_KEY) {
                    properties[key] = value
                }
            }
            return HalResource(
                properties = properties,
                links = parseLinks(map[LINKS_KEY]),
                embedded = parseEmbedded(map[EMBEDDED_KEY])
            )
        }

        private fun parseLinks(value: Any?): Map<String, List<HalLink>> {
            val linksMap = value as? Map<*, *> ?: return emptyMap()
            val result = LinkedHashMap<String, List<HalLink>>()
            linksMap.forEach { (rel, linkValue) ->
                val relation = rel as? String ?: return@forEach
                // curies are HAL link-relation documentation entries, never followable hrefs.
                if (relation == CURIES_RELATION) return@forEach
                val links = toHalLinkList(linkValue)
                if (links.isNotEmpty()) {
                    result[relation] = links
                }
            }
            return result
        }

        private fun toHalLinkList(value: Any?): List<HalLink> = when (value) {
            is List<*> -> value.mapNotNull { toHalLink(it) }
            is Map<*, *> -> listOfNotNull(toHalLink(value))
            else -> emptyList()
        }

        private fun toHalLink(value: Any?): HalLink? {
            val map = value as? Map<*, *> ?: return null
            val href = map[HREF_KEY] as? String ?: return null
            val name = map[NAME_KEY] as? String
            val templated = map[TEMPLATED_KEY] as? Boolean ?: false
            return HalLink(href = href, name = name, templated = templated)
        }

        private fun parseEmbedded(value: Any?): Map<String, List<Map<String, Any?>>> {
            val embeddedMap = value as? Map<*, *> ?: return emptyMap()
            val result = LinkedHashMap<String, List<Map<String, Any?>>>()
            embeddedMap.forEach { (rel, resourceValue) ->
                val relation = rel as? String ?: return@forEach
                val resources = toResourceList(resourceValue)
                if (resources.isNotEmpty()) {
                    result[relation] = resources
                }
            }
            return result
        }

        private fun toResourceList(value: Any?): List<Map<String, Any?>> = when (value) {
            is List<*> -> value.mapNotNull { toResourceMap(it) }
            is Map<*, *> -> listOfNotNull(toResourceMap(value))
            else -> emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        private fun toResourceMap(value: Any?): Map<String, Any?>? =
            if (value is Map<*, *>) value as Map<String, Any?> else null

        private fun toKotlinValue(element: com.google.gson.JsonElement): Any? = when {
            element.isJsonNull -> null
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isBoolean -> primitive.asBoolean
                    primitive.isNumber -> primitive.asNumber
                    else -> primitive.asString
                }
            }
            element.isJsonArray -> element.asJsonArray.map { toKotlinValue(it) }
            element.isJsonObject -> element.asJsonObject.entrySet()
                .associate { it.key to toKotlinValue(it.value) }
            else -> null
        }
    }
}
