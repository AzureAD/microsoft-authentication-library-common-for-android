package com.microsoft.identity.common.internal.commands.parameters;

import android.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * Interface indicating that a class carries a set of string/string parameters.
 */
public interface IHasExtraParameters {
    /**
     * @return a list of pairs of String, String parameters.
     */
    Iterable<Map.Entry<String, String>> getExtraParameters();
    /**
     * @return a list of pairs of String, String parameters.
     */
    void setExtraParameters(Iterable<Map.Entry<String, String>> params);
}
