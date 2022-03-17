package com.github.bati;

import java.util.Map;

import okhttp3.OkHttpClient;

// you can user url or http heads for authentication
// Every time connect to server, we call the uri() and headers() function
public interface IConfigProvider {
    String url();
    Map<String, String> headers();
    boolean autoReconnect();
    long pingIntervalSeconds();
    OkHttpClient.Builder webSocketClientBuilder();
}
