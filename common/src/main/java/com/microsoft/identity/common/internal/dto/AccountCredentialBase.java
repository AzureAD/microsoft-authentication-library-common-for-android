package com.microsoft.identity.common.internal.dto;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Objects to support the [de]/serialization of extra fields.
 */
public abstract class AccountCredentialBase {

    private transient Map<String, JSONObject> mAdditionalFields = new HashMap<>();

    public Map<String, JSONObject> getAdditionalFields() {
        return mAdditionalFields;
    }

    public void setAdditionalFields(Map<String, JSONObject> additionalFields) {
        this.mAdditionalFields = additionalFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountCredentialBase that = (AccountCredentialBase) o;

        return mAdditionalFields != null ? mAdditionalFields.equals(that.mAdditionalFields) : that.mAdditionalFields == null;
    }

    @Override
    public int hashCode() {
        return mAdditionalFields != null ? mAdditionalFields.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AccountCredentialBase{" +
                "mAdditionalFields=" + mAdditionalFields +
                '}';
    }
}
