package com.microsoft.identity.common.internal.controllers;

public interface Command<T> {
    T execute() throws Exception;
}
