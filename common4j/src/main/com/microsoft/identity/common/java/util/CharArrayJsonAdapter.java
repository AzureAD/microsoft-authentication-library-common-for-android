package com.microsoft.identity.common.java.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class CharArrayJsonAdapter extends TypeAdapter<char[]> {
    @Override
    public void write(JsonWriter out, char[] val) throws IOException {
        out.value(new String(val));
    }

    @Override
    public char[] read(JsonReader in) throws IOException {
        return in.nextString().toCharArray();
    }
}
