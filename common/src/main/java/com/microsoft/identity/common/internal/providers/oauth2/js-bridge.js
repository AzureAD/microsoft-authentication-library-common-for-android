// filepath: c:\repos\android-complete\common\common\src\main\java\com\microsoft\identity\common\internal\providers\oauth2\js-bridge.js
//
// WebAuthn JavaScript Bridge for Android Credential Manager
//
// This JavaScript code is injected into WebViews to intercept WebAuthn API calls
// (navigator.credentials.create/get) and bridge them to the Android Credential Manager.
// It handles the communication protocol between the web page and the native Android code.
//
// ⚠️ IMPORTANT: MINIFICATION REQUIRED ⚠️
// Any modifications to this file MUST be minified and the minified output MUST be
// updated in PasskeyWebListener.kt in the constant WEB_AUTHN_INTERFACE_JS_MINIFIED.
//
// Steps to update:
// 1. Make changes to this file (js-bridge.js)
// 2. Minify the code using a JavaScript minifier (e.g., https://www.toptal.com/developers/javascript-minifier, https://minify-js.com/)
// 3. Update the WEB_AUTHN_INTERFACE_JS_MINIFIED constant in PasskeyWebListener.kt
// 4. Test thoroughly to ensure the minified version works correctly
//
// Failure to update the minified version will result in the changes NOT being
// reflected in the actual WebView injection.
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
var __webauthn_interface__;
var __webauthn_hooks__;
(function (__webauthn_hooks__) {

    //Adding event listener to the interface for replies by default
    __webauthn_interface__.addEventListener('message', onReply);
    // pendingResolveGet/Create is the thunk to resolve an outstanding get request.
    var pendingResolveGet = null;
    var pendingResolveCreate = null;
    // pendingRejectGet/Create is the thunk to fail an outstanding request.
    var pendingRejectGet = null;
    var pendingRejectCreate = null;
    // create overrides 'navigator.credentials.create' which proxies webauthn requests
    // to the create embedder
    function create(request) {
        if (!("publicKey" in request)) {
            return __webauthn_hooks__.originalCreateFunction(request);
        }
        var ret = new Promise(function (resolve, reject) {
            pendingResolveCreate = resolve;
            pendingRejectCreate = reject;
        });
        var temppk = request.publicKey;
        if (temppk.hasOwnProperty('challenge')) {
            var str = CM_base64url_encode(temppk.challenge);
            temppk.challenge = str;
        }
        if (temppk.hasOwnProperty('user') && temppk.user.hasOwnProperty('id')) {
            var encodedString = CM_base64url_encode(temppk.user.id);
            temppk.user.id = encodedString;
        }
        // Encode all excludeCredentials ids if provided
        if (temppk.hasOwnProperty('excludeCredentials') && Array.isArray(temppk.excludeCredentials) && temppk.excludeCredentials.length > 0) {
            for (var i = 0; i < temppk.excludeCredentials.length; i++) {
                var cred = temppk.excludeCredentials[i];
                if (cred && cred.hasOwnProperty('id')) {
                    cred.id = CM_base64url_encode(cred.id);
                }
            }
        }
        var jsonObj = {"type":"create", "request":temppk}

        var json = JSON.stringify(jsonObj);
        __webauthn_interface__.postMessage(json);
        return ret;
    }
    __webauthn_hooks__.create = create;
    // get overrides `navigator.credentials.get` and proxies any WebAuthn
    // requests to the get embedder.
    function get(request) {
        if (!("publicKey" in request)) {
            return __webauthn_hooks__.originalGetFunction(request);
        }
        var ret = new Promise(function (resolve, reject) {
            pendingResolveGet = resolve;
            pendingRejectGet = reject;
        });
        var temppk = request.publicKey;
        if (temppk.hasOwnProperty('challenge')) {
            var str = CM_base64url_encode(temppk.challenge);
            temppk.challenge = str;
        }
        var jsonObj = {"type":"get", "request":temppk}

        var json = JSON.stringify(jsonObj);
        __webauthn_interface__.postMessage(json);
        return ret;
    }
    __webauthn_hooks__.get = get;

    // The embedder gives replies back here, caught by the event listener.
    function onReply(msg) {
        console.log(msg.data);
        var reply = JSON.parse(msg.data);
        if(reply.type === "get") {
            onReplyGet(reply);
        } else if (reply.type === "create") {
            onReplyCreate(reply);
        } else {
            console.log("Incorrect response format for reply: " + reply.type);
        }
    }

    // Resolves what is expected for get, called when the embedder is ready
    function onReplyGet(reply) {
        if (pendingResolveGet === null || pendingRejectGet === null) {
            console.log("Reply failure: Resolve: " + pendingResolveCreate +
                    " and reject: " + pendingRejectCreate);
            return;
        }
        if (reply.status != 'success') {
            var reject = pendingRejectGet;
            pendingResolveGet = null;
            pendingRejectGet = null;
            reject(new DOMException(reply.data.domExceptionMessage, reply.data.domExceptionName));
            return;
        }
        var cred = credentialManagerDecode(reply.data);
        var resolve = pendingResolveGet;
        pendingResolveGet = null;
        pendingRejectGet = null;
        resolve(cred);
    }
    __webauthn_hooks__.onReplyGet = onReplyGet;
    // This a specific decoder for expected types contained in PublicKeyCredential json
    function CM_base64url_decode(value) {
        var m = value.length % 4;
        return Uint8Array.from(atob(value.replace(/-/g, '+')
            .replace(/_/g, '/')
            .padEnd(value.length + (m === 0 ? 0 : 4 - m), '=')), function (c)
            { return c.charCodeAt(0); }).buffer;
    }
    __webauthn_hooks__.CM_base64url_decode = CM_base64url_decode;
    function CM_base64url_encode(buffer) {
        return btoa(Array.from(new Uint8Array(buffer), function (b)
        { return String.fromCharCode(b); }).join(''))
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+${'$'}/, '');
    }
    __webauthn_hooks__.CM_base64url_encode = CM_base64url_encode;
    // Resolves what is expected for create, called when the embedder is ready
    function onReplyCreate(reply) {
        if (pendingResolveCreate === null || pendingRejectCreate === null) {
            console.log("Reply failure: Resolve: " + pendingResolveCreate +
            " and reject: " + pendingRejectCreate);
            return;
        }

        if (reply.status != 'success') {
            var reject = pendingRejectCreate;
            pendingResolveCreate = null;
            pendingRejectCreate = null;
            reject(new DOMException(reply.data.domExceptionMessage, reply.data.domExceptionName));
            return;
        }
        var cred = credentialManagerDecode(reply.data);
        var resolve = pendingResolveCreate;
        pendingResolveCreate = null;
        pendingRejectCreate = null;
        resolve(cred);
    }
    __webauthn_hooks__.onReplyCreate = onReplyCreate;
    /**
     * This decodes the output from the credential manager flow to parse back into URL format. Both
     * get and create flows ultimately return a PublicKeyCredential object.
     * @param json_result
     */
    function credentialManagerDecode(decoded_reply) {
        decoded_reply.rawId = CM_base64url_decode(decoded_reply.rawId);
        decoded_reply.response.clientDataJSON = CM_base64url_decode(decoded_reply.response.clientDataJSON);
        if (decoded_reply.response.hasOwnProperty('attestationObject')) {
            decoded_reply.response.attestationObject = CM_base64url_decode(decoded_reply.response.attestationObject);
        }
        if (decoded_reply.response.hasOwnProperty('authenticatorData')) {
            decoded_reply.response.authenticatorData = CM_base64url_decode(decoded_reply.response.authenticatorData);
        }
        if (decoded_reply.response.hasOwnProperty('signature')) {
            decoded_reply.response.signature = CM_base64url_decode(decoded_reply.response.signature);
        }
        if (decoded_reply.response.hasOwnProperty('userHandle')) {
            decoded_reply.response.userHandle = CM_base64url_decode(decoded_reply.response.userHandle);
        }
        decoded_reply.getClientExtensionResults = function getClientExtensionResults() { return {}; };
        decoded_reply.response.getTransports = function getTransports() {
            if (decoded_reply.response.hasOwnProperty('transports')) { return decoded_reply.response.transports; }
            return [];
        };
        return decoded_reply;
    }
})(__webauthn_hooks__ || (__webauthn_hooks__ = {}));
__webauthn_hooks__.originalGetFunction = navigator.credentials.get;
__webauthn_hooks__.originalCreateFunction = navigator.credentials.create;
navigator.credentials.get = __webauthn_hooks__.get;
navigator.credentials.create = __webauthn_hooks__.create;
// Some sites test that `typeof window.PublicKeyCredential` is
// `function`.
window.PublicKeyCredential = (function () { });
window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable =
    function () {
        return Promise.resolve(false);
    };
