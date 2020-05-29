package com.microsoft.identity.common.internal.util;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ParcelableUtil {

    public static byte[] marshall(@NonNull final Parcelable parceable) {
        final Parcel parcel = Parcel.obtain();
        parceable.writeToParcel(parcel, 0);
        final byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static Parcel unmarshall(byte[] bytes) {
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return parcel;
    }

    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        final Parcel parcel = unmarshall(bytes);
        T result = creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }
}
