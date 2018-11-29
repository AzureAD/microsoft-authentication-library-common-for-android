package com.microsoft.identity.common.internal.util;

import android.util.Pair;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QueryParamsAdapter extends TypeAdapter<List<Pair<String, String>>> {

    @Override
    public void write(JsonWriter out, List<Pair<String, String>> queryParams) throws IOException {
        out.beginObject();

        for(Pair<String, String> pair : queryParams){
            out.name(pair.first);
            out.value(pair.second);
        }
        out.endObject();
    }

    @Override
    public List<Pair<String, String>> read(JsonReader in) throws IOException {
        in.beginObject();
        List<Pair<String, String>> result = new ArrayList<>();
        while (in.hasNext()){
            String key = in.nextName();
            String value = in.nextString();
            Pair<String, String> pair = new Pair<>(key, value);
            result.add(pair);
        }
        in.endObject();
        return result;
    }
}
