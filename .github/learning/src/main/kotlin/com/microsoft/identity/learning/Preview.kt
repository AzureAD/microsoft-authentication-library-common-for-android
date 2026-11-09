// Copyright (c) Microsoft Corporation. All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.microsoft.identity.learning

import com.google.gson.JsonObject

internal const val PREVIEW_LABEL = "learning-live-preview"

private fun JsonObject.child(name: String): JsonObject =
    get(name)?.obj() ?: throw LearningFailure("Missing preview event field: $name")

internal fun authorizePreview(policy: Policy, approvedSha: String, eventText: String) {
    val event = Json.parse(eventText).obj()
    ensure(event.text("action") == "labeled" && event.child("label").text("name") == PREVIEW_LABEL,
        "Live preview requires the explicit preview-label event")
    ensure(event.child("repository").text("full_name") == policy.repository, "Preview event repository mismatch")
    val sender = event.child("sender")
    val actor = Actor(sender.number("id"), sender.text("type"))
    ensure(policy.human(actor) && actor.id in policy.ownerIds, "Only an approved human owner can request live preview")
    val pull = event.child("pull_request")
    ensure(pull.number("number") > 0, "Invalid preview PR")
    val head = pull.child("head")
    val base = pull.child("base")
    ensure(head.child("repo").text("full_name") == policy.repository &&
        base.child("repo").text("full_name") == policy.repository && base.text("ref") == policy.base,
        "Live preview requires a same-repository PR targeting the approved base")
    ensure(sha(approvedSha) && head.text("sha") == approvedSha,
        "Preview revision is not the explicitly approved commit")
}
