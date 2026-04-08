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
package com.microsoft.identity.deviceregistration;

import static com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException.INTERNAL_ERROR_CODE;
import static com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException.INVALID_PACKAGE_ERROR_CODE;
import static com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker.CORRELATION_ID;
import static com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker.PROTOCOL_DATA;
import static com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker.PROTOCOL_EXCEPTION_ERROR_CAUSE_MESSAGE;
import static com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker.PROTOCOL_EXCEPTION_ERROR_CODE;
import static com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker.PROTOCOL_EXCEPTION_ERROR_MESSAGE;
import static com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker.PROTOCOL_EXCEPTION_STACK_TRACE_STRING;
import static com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker.PROTOCOL_NAME;


import android.os.Bundle;

import com.microsoft.identity.deviceregistration.testprotocols.TestHappyPathV0Parameters;
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecordWithAccount;
import com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException;
import com.microsoft.identity.deviceregistration.java.exception.DrsErrorResponseException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


import java.util.UUID;

import lombok.SneakyThrows;

@RunWith(RobolectricTestRunner.class)
public class AndroidDeviceRegistrationProtocolPackerTest {

    private static final String TEST_CORRELATION_ID = UUID.randomUUID().toString();
    private static final String TEST_PROTOCOL_NAME = "fake.protocol.name";
    private static final String TEST_DATA = "data.sample.1";
    private static final String TEST_TENANT = "test.tenant.id";
    private static final String TEST_UPN = "test.upn";
    private static final String TEST_DEVICE_ID = "test.device.id";
    private static final String TEST_ACCOUNT_NAME = "test.account.name";

    private static final DeviceRegistrationRecord DEVICE_REGISTRATION_RECORD
            = new DeviceRegistrationRecord(TEST_TENANT, TEST_UPN, TEST_DEVICE_ID, false, false);

    private static final DeviceRegistrationRecordWithAccount DEVICE_REGISTRATION_RECORD_WITH_ACCOUNT
            = new DeviceRegistrationRecordWithAccount(TEST_ACCOUNT_NAME, TEST_TENANT, TEST_UPN, TEST_DEVICE_ID, false, false);


    private static final TestHappyPathV0Parameters TEST_PROTOCOL = new TestHappyPathV0Parameters(
            TEST_DATA,
            TEST_DATA,
            true,
            false,
            123.456f,
            9f,
            DEVICE_REGISTRATION_RECORD,
            DEVICE_REGISTRATION_RECORD_WITH_ACCOUNT
    );

    private static final String TEST_ERROR_MESSAGE = "Test error message";
    private static final String TEST_ERROR_CAUSE_MESSAGE = "Fake cause";
    private static final NullPointerException NULL_POINTER_EXCEPTION = new NullPointerException(TEST_ERROR_CAUSE_MESSAGE);
    private static final DeviceRegistrationException DEVICE_REGISTRATION_EXCEPTION =
            new DeviceRegistrationException(INTERNAL_ERROR_CODE, TEST_ERROR_MESSAGE, NULL_POINTER_EXCEPTION);

    private static final AndroidDeviceRegistrationProtocolPacker packer = new AndroidDeviceRegistrationProtocolPacker();

    @Test
    public void testPackingNull() {
        Assert.assertThrows(NullPointerException.class, () -> {
            packer.pack(null);
        });
    }

    @Test
    public void testPackExceptionNull() {
        Assert.assertThrows(NullPointerException.class, () -> {
            packer.packThrowable(null);
        });
    }

    @Test
    public void testUnpackNameNull() {
        Assert.assertThrows(NullPointerException.class, () -> {
            packer.unpackName(null);
        });
    }

    @Test
    public void testUnpackDataNull() {
        Assert.assertThrows(NullPointerException.class, () -> {
            packer.unpackData(null);
        });
    }

    @Test
    public void testUnpackNameWithEmptyBundle() {
        final DeviceRegistrationException deviceRegistrationException
                = Assert.assertThrows(DeviceRegistrationException.class, () -> {
            packer.unpackName(new Bundle());
        });
        Assert.assertEquals(
                INVALID_PACKAGE_ERROR_CODE,
                deviceRegistrationException.getErrorCode()
        );
        Assert.assertEquals(
                "Bundle does not contain " + PROTOCOL_NAME + " key or is empty.",
                deviceRegistrationException.getMessage()
        );
    }

    @SneakyThrows
    @Test
    public void testUnpackDataWithEmptyBundle() {
        final DeviceRegistrationException deviceRegistrationException
                = Assert.assertThrows(DeviceRegistrationException.class, () -> {
            packer.unpackData(new Bundle());
        });
        Assert.assertEquals(
                INVALID_PACKAGE_ERROR_CODE,
                deviceRegistrationException.getErrorCode()
        );
        Assert.assertEquals(
                "Bundle does not contain " + PROTOCOL_DATA + " key or is empty.",
                deviceRegistrationException.getMessage()
        );
    }

    @SneakyThrows
    @Test
    public void testUnpackDataWithEmptyData() {
        final DeviceRegistrationException deviceRegistrationException =
                Assert.assertThrows(DeviceRegistrationException.class, () -> {
                    final Bundle bundle = new Bundle();
                    bundle.putString(PROTOCOL_NAME, TEST_PROTOCOL.getProtocolName());
                    packer.unpackData(bundle);
                });
        Assert.assertEquals(
                INVALID_PACKAGE_ERROR_CODE,
                deviceRegistrationException.getErrorCode()
        );
        Assert.assertEquals(
                "Bundle does not contain " + PROTOCOL_DATA + " key or is empty.",
                deviceRegistrationException.getMessage()
        );
    }

    @SneakyThrows
    @Test
    public void testUnpackData() {
        final Bundle bundle = new Bundle();
        bundle.putString(PROTOCOL_NAME, PROTOCOL_NAME);
        bundle.putByteArray(PROTOCOL_DATA, TEST_DATA.getBytes());
        Assert.assertArrayEquals(
                TEST_DATA.getBytes(),
                packer.unpackData(bundle)
        );
    }

    @SneakyThrows
    @Test
    public void testUnpackDataWithException() {

        final DeviceRegistrationException deviceRegistrationException =
                Assert.assertThrows(DeviceRegistrationException.class, () -> {
                    final Bundle bundle = packer.packThrowable(DEVICE_REGISTRATION_EXCEPTION);
                    packer.unpackData(bundle);
                });
        Assert.assertEquals(
                DEVICE_REGISTRATION_EXCEPTION.getMessage(),
                deviceRegistrationException.getMessage())
        ;
        Assert.assertEquals(
                DEVICE_REGISTRATION_EXCEPTION.getErrorCode(),
                deviceRegistrationException.getErrorCode()
        );
        Assert.assertEquals(
                DEVICE_REGISTRATION_EXCEPTION.getCause().getMessage(),
                deviceRegistrationException.getCause().getMessage()
        );
        Assert.assertNotNull(deviceRegistrationException.getStackTraceString());
    }

    @SneakyThrows
    @Test
    public void testUnpackName() {
        final Bundle bundle = new Bundle();
        bundle.putString(PROTOCOL_NAME, TEST_PROTOCOL_NAME);
        Assert.assertEquals(
                TEST_PROTOCOL_NAME,
                packer.unpackName(bundle)
        );
    }

    @Test
    public void testUnpackCorrelationId() {
        final Bundle bundle = new Bundle();
        bundle.putString(CORRELATION_ID, TEST_CORRELATION_ID);
        Assert.assertEquals(
                TEST_CORRELATION_ID,
                packer.unpackCorrelationId(bundle)
        );
    }

    @Test
    public void testUnpackCorrelationIdNull() {
        final Bundle bundle = new Bundle();
        Assert.assertNotNull(
                packer.unpackCorrelationId(bundle)
        );
    }

    @Test
    public void testPackingProtocol() {
        final Bundle protocolBundle = packer.pack(TEST_PROTOCOL);
        Assert.assertEquals(
                TEST_PROTOCOL.getProtocolName(),
                protocolBundle.getString(PROTOCOL_NAME)
        );
        Assert.assertArrayEquals(
                TEST_PROTOCOL.serialize(),
                protocolBundle.getByteArray(PROTOCOL_DATA)

        );
    }

    @Test
    public void testPackBaseException() {

        final Bundle bundle = packer.packThrowable(
                DEVICE_REGISTRATION_EXCEPTION
        );
        Assert.assertEquals(
                DEVICE_REGISTRATION_EXCEPTION.getErrorCode(),
                bundle.getString(PROTOCOL_EXCEPTION_ERROR_CODE)
        );
        Assert.assertEquals(
                DEVICE_REGISTRATION_EXCEPTION.getMessage(),
                bundle.getString(PROTOCOL_EXCEPTION_ERROR_MESSAGE)
        );
        Assert.assertEquals(
                DEVICE_REGISTRATION_EXCEPTION.getCause().getMessage(),
                bundle.getString(PROTOCOL_EXCEPTION_ERROR_CAUSE_MESSAGE)
        );
        Assert.assertNotNull(bundle.getString(PROTOCOL_EXCEPTION_STACK_TRACE_STRING));

    }

    @Test
    public void testPackThrowable() {
        final String errorMessage = "UnexpectedError";
        final Bundle bundle = packer.packThrowable(
                new Throwable(errorMessage)
        );
        Assert.assertEquals(
                ClientException.UNKNOWN_ERROR,
                bundle.getString(PROTOCOL_EXCEPTION_ERROR_CODE)
        );
        Assert.assertEquals(
                errorMessage,
                bundle.getString(PROTOCOL_EXCEPTION_ERROR_MESSAGE)
        );
        Assert.assertNotNull(bundle.getString(PROTOCOL_EXCEPTION_STACK_TRACE_STRING));
    }

    @Test
    public void testUnPackThrowable() {
        final Bundle bundle = new Bundle();
        final String errorCode = "errorCode";
        final String errorMessage = "errorMessage";
        final String errorCause = "errorCause";

        bundle.putString(PROTOCOL_EXCEPTION_ERROR_CODE, errorCode);
        bundle.putString(PROTOCOL_EXCEPTION_ERROR_MESSAGE, errorMessage);
        bundle.putString(PROTOCOL_EXCEPTION_ERROR_CAUSE_MESSAGE, errorCause);

        final BaseException exception = Assert.assertThrows(BaseException.class, () -> {
            packer.unpackData(bundle);
        });

        Assert.assertEquals(errorMessage, exception.getMessage());
        Assert.assertEquals(errorCode, exception.getErrorCode());
        Assert.assertEquals(errorCause, exception.getCause().getMessage());
    }

    @Test
    public void testPackDRSException() {
        final String drsError = "{\"code\": \"invalid_request\", \"message\": \"Error: 'invalid_tenant' Description: 'Tenant 'rapong.onmicrosoft.com' not found. This may happen if there are no active subscriptions for the tenant. Check to make sure you have the correct tenant ID. Check with your subscription administrator'\", \"operation\": \"Discovery\", \"requestid\": \"07171fdd-1059-41af-97b1-8e6ca61a02a7\", \"subcode\": \"invalid_tenant\", \"time\": \"08-25-2020 20:38:52Z\"}";
        final Bundle bundle = packer.packThrowable(
                DrsErrorResponseException.getFromErrorResponse(drsError)
        );
        Assert.assertEquals(
                "invalid_request",
                bundle.getString(PROTOCOL_EXCEPTION_ERROR_CODE)
        );
        Assert.assertEquals(
                drsError,
                bundle.getString(PROTOCOL_EXCEPTION_ERROR_MESSAGE)
        );
        Assert.assertNotNull(bundle.getString(PROTOCOL_EXCEPTION_STACK_TRACE_STRING));

    }

    @Test
    public void testUnPackDRSException() {
        final Bundle bundle = new Bundle();
        final String errorMessage = "{\"code\": \"invalid_request\", \"message\": \"Error: 'invalid_tenant' Description: 'Tenant 'rapong.onmicrosoft.com' not found. This may happen if there are no active subscriptions for the tenant. Check to make sure you have the correct tenant ID. Check with your subscription administrator'\", \"operation\": \"Discovery\", \"requestid\": \"07171fdd-1059-41af-97b1-8e6ca61a02a7\", \"subcode\": \"invalid_tenant\", \"time\": \"08-25-2020 20:38:52Z\"}";

        bundle.putString(PROTOCOL_EXCEPTION_ERROR_CODE, DrsErrorResponseException.DEFAULT_ERROR_CODE);
        bundle.putString(PROTOCOL_EXCEPTION_ERROR_MESSAGE, errorMessage);

        final DrsErrorResponseException exception = Assert.assertThrows(DrsErrorResponseException.class, () -> {
            packer.unpackData(bundle);
        });

        Assert.assertEquals(errorMessage, exception.getMessage());
        Assert.assertEquals("invalid_request", exception.getErrorCode());
        Assert.assertNull(exception.getCause());
    }
}
