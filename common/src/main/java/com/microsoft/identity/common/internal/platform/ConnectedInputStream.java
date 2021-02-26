package com.microsoft.identity.common.internal.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ConnectedInputStream extends InputStream {
    private final OutputStream stream;

    @Override
    public int read() throws IOException {
        return 0;
    }
}
