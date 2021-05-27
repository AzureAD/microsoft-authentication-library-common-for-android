// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.json.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JType;

import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.TypeRule;

/**
 * An implementation of {@link TypeRule} that will use a {@link MapRule} to generate a field of a
 * Map data type if the type of the field in schema was defined as an "object" AND the field did not
 * have any properties on it. Otherwise, this rule will just delegate the operation to its parent.
 */
public class AuthClientJsonSchemaTypeRule extends TypeRule {

    private final AuthClientJsonSchemaRuleFactory ruleFactory;

    protected AuthClientJsonSchemaTypeRule(final AuthClientJsonSchemaRuleFactory ruleFactory) {
        super(ruleFactory);
        this.ruleFactory = ruleFactory;
    }

    @Override
    public JType apply(String nodeName, JsonNode node, JsonNode parent, JClassContainer jClassContainer, Schema schema) {
        if (node != null &&
                node.has("type")
                && node.get("type") != null
                && node.get("type").isTextual()
                && node.get("type").asText().equals("object")
                && (!node.has("properties") || node.path("properties").size() == 0)) {
            return ruleFactory.getMapRule().apply(nodeName, node, parent, jClassContainer, schema);
        } else {
            return super.apply(nodeName, node, parent, jClassContainer, schema);
        }
    }
}
