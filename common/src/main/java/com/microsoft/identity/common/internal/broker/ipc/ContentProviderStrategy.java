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
package com.microsoft.identity.common.internal.broker.ipc;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.AUTHORITY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.CONTENT_SCHEME;
import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.CONNECTION_ERROR;
import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.CONTENT_PROVIDER;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.util.ParcelableUtil;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;

/**
 * A strategy for communicating with the targeted broker via Content Provider.
 */
public class ContentProviderStrategy implements IIpcStrategy {

    private static final String TAG = ContentProviderStrategy.class.getName();
    private final Context mContext;

    public ContentProviderStrategy(final Context context) {
        mContext = context;
    }

    @Override
    @Nullable
    public Bundle communicateToBroker(final @NonNull BrokerOperationBundle brokerOperationBundle)
            throws BrokerCommunicationException {
        final String methodTag = TAG + ":communicateToBroker";
        final String operationName = brokerOperationBundle.getOperation().name();

        Logger.info(methodTag, "Broker operation name: " + operationName +  " brokerPackage: "+brokerOperationBundle.getTargetBrokerAppPackageName());

        final Uri uri = getContentProviderURI(
                brokerOperationBundle.getTargetBrokerAppPackageName(),
                brokerOperationBundle.getContentProviderPath()
        );
        Logger.info(methodTag, "Request to BrokerContentProvider for uri path " +
                brokerOperationBundle.getContentProviderPath()
        );

        String marshalledRequestString = null;

        final Bundle requestBundle = brokerOperationBundle.getBundle();
        if (requestBundle != null) {
            byte[] marshalledBytes = ParcelableUtil.marshall(requestBundle);
            marshalledRequestString = Base64.encodeToString(marshalledBytes, 0);
        }

        final Cursor cursor = mContext.getContentResolver().query(
                uri,
                null,
                marshalledRequestString,
                null,
                null
        );

        if (cursor != null) {
            try {
                final Bundle resultBundle = cursor.getExtras();

                if (resultBundle == null) {
                    final String message = "Received an empty bundle. This means the operation is not supported on the other side. " +
                            "If you're using a newer feature, please bump the minimum protocol version.";
                    Logger.error(methodTag, message, null);
                    throw new BrokerCommunicationException(OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE, getType(), message, null);
                }

                Logger.info(methodTag, "Received successful result from Broker Content Provider.");
                return resultBundle;
            } catch (final RuntimeException exception) {
                final String message = "Failed to get result from Broker Content Provider";
                Logger.error(methodTag, message, exception);
                throw new BrokerCommunicationException(CONNECTION_ERROR, getType(), message, null);
            } finally {
                cursor.close();
            }
        } else {
            final String message = "Failed to get result from Broker Content Provider, cursor is null";
            Logger.error(methodTag, message, null);
            throw new BrokerCommunicationException(CONNECTION_ERROR, getType(), message, null);
        }
    }

    @Override
    public Type getType() {
        return CONTENT_PROVIDER;
    }

    /**
     * Returns a content provider URI for the given path.
     */
    private Uri getContentProviderURI(final @NonNull String targetedBrokerPackageName,
                                      final @NonNull String path) {
        final String authority = getContentProviderAuthority(targetedBrokerPackageName);
        return Uri.parse(CONTENT_SCHEME + authority + path);
    }

    /**
     * Returns content provider authority.
     */
    private String getContentProviderAuthority(final @NonNull String targetedBrokerPackageName) {
        return targetedBrokerPackageName + "." + AUTHORITY;
    }

    /**
     * Returns true if the target package name supports this content provider strategy.
     */
    public boolean isBrokerContentProviderAvailable(final @NonNull String targetedBrokerPackageName) {
        final String methodTag = TAG + ":isBrokerContentProviderAvailable";
        final String contentProviderAuthority = getContentProviderAuthority(targetedBrokerPackageName);

        final List<ProviderInfo> providers = mContext.getPackageManager()
                .queryContentProviders(null, 0, 0);

        if (providers == null) {
            Logger.error(methodTag, "Content Provider not found.", null);
            return false;
        }

        for (final ProviderInfo providerInfo : providers) {
            if (providerInfo.authority != null && providerInfo.authority.equals(contentProviderAuthority)) {
                return true;
            }
        }
        return false;
    }
}
