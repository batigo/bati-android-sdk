package com.github.bati;

public interface IStatusListener {
    void onStatusUpdate(IConnection.Status status);
    void onError(int code);
}
