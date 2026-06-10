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
package com.microsoft.identity.deviceregistration.java.protocol.packer;

import static com.microsoft.identity.common.java.AuthenticationConstants.CHARSET_UTF8;
import static com.microsoft.identity.common.java.exception.ClientException.JSON_PARSE_FAILURE;

import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecordWithAccount;
import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.protocol.IDeviceRegistrationProtocol;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory;

import java.io.IOException;
import java.util.UUID;

import lombok.NonNull;

/**
 * Moshi-based serializer for device registration protocol types in common4j.
 * Uses hardcoded broker4j FQCNs as polymorphic discriminator labels to ensure
 * wire compatibility between common4j (client-side) and broker4j (server-side).
 *
 * <p>Moshi serializes/deserializes {@link IDeviceRegistrationRecord} using common4j types
 * ({@link DeviceRegistrationRecord}, {@link DeviceRegistrationRecordWithAccount}).
 * Broker callers should convert broker4j records to common4j records before serialization,
 * and convert common4j records back to broker4j after deserialization.
 * The wire format (JSON discriminator labels) uses hardcoded broker4j FQCNs for
 * backward compatibility.</p>
 */
public class DeviceRegistrationProtocolMoshiSerializer<DeviceRegistrationProtocol extends IDeviceRegistrationProtocol>
        implements IDeviceRegistrationProtocolSerializer<DeviceRegistrationProtocol> {

    public static final String TAG = DeviceRegistrationProtocolMoshiSerializer.class.getSimpleName();
    public static final String RECORD_TYPE_KEY = "record_type";

    // Hardcoded broker4j FQCNs for wire compatibility with existing broker serialization.
    public static final String RECORD_WITHOUT_ACCOUNT =
            "com.microsoft.identity.broker4j.workplacejoin.api.DeviceRegistrationRecord";
    public static final String RECORD_WITH_ACCOUNT =
            "com.microsoft.identity.broker4j.workplacejoin.api.DeviceRegistrationRecordWithAccount";

    private static final Moshi moshi = new Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(IDeviceRegistrationRecord.class, RECORD_TYPE_KEY)
                    .withSubtype(DeviceRegistrationRecord.class, RECORD_WITHOUT_ACCOUNT)
                    .withSubtype(DeviceRegistrationRecordWithAccount.class, RECORD_WITH_ACCOUNT))
            .add(new UUIDAdapter())
            .build();

    private static final class UUIDAdapter {
        @ToJson
        public String toJson(final UUID uuid) {
            return uuid.toString();
        }

        @FromJson
        public UUID fromJson(final String json) {
            return UUID.fromString(json);
        }
    }

    private final JsonAdapter<DeviceRegistrationProtocol> deviceRegistrationProtocolJsonAdapter;

    public DeviceRegistrationProtocolMoshiSerializer(
            final Class<DeviceRegistrationProtocol> deviceRegistrationProtocolType) {
        deviceRegistrationProtocolJsonAdapter = moshi.adapter(deviceRegistrationProtocolType);
    }

    /**
     * Serialize a {@link DeviceRegistrationProtocol protocol} into a byte array.
     *
     * @param protocol to serialize
     * @return a byte array with the serialized protocol.
     */
    @Override
    public byte[] serialize(@NonNull final DeviceRegistrationProtocol protocol) {
        return deviceRegistrationProtocolJsonAdapter.toJson(protocol).getBytes(CHARSET_UTF8);
    }

    /**
     * Deserializes a byte array into a {@link DeviceRegistrationProtocol protocol} object.
     *
     * @param serializedProtocol byte array with the serialized object.
     * @return a {@link DeviceRegistrationProtocol protocol} based on provided byte array.
     */
    @NonNull
    @Override
    public DeviceRegistrationProtocol deserialize(@NonNull final byte[] serializedProtocol) throws ClientException {
        final String methodTag = TAG + ":deserialize";
        final String jsonString = new String(serializedProtocol, CHARSET_UTF8);
        final DeviceRegistrationProtocol protocol;
        try {
            protocol = deviceRegistrationProtocolJsonAdapter.fromJson(jsonString);
        } catch (final IOException e) {
            Logger.error(methodTag, "Failed to convert json to DeviceRegistrationProtocol", e);
            throw new ClientException(JSON_PARSE_FAILURE, e.getMessage(), e);
        }
        if (protocol == null) {
            Logger.error(methodTag, "The protocol cannot be unpacked", null);
            throw new ClientException(JSON_PARSE_FAILURE, "Protocol is null");
        }
        return protocol;
    }
}
