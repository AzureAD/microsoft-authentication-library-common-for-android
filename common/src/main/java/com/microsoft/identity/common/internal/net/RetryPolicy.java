package com.microsoft.identity.common.internal.net;

import java.io.IOException;
import java.util.concurrent.Callable;

interface RetryPolicy<T> {
    T attempt (Callable<T> response) throws IOException;
}
