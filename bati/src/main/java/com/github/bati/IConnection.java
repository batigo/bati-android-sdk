package com.github.bati;

public interface IConnection {
    IConnection Setup(IConfigProvider config, IStatusListener statusListener);
    IConnection Insert(IConnectionProto proto);
    IConnection Insert(IDataProto proto);

    void connect();
    void disconnect();
    void release();
    Status status();

    enum Status {
        NotInitialize(0),
        Initialized(1),
        Connecting(2),
        Connected(3),
        DisConnecting(4),
        DisConnected(5),
        Failure(6),
        Released(-10);
        final int value;
        Status(int value) {
            this.value = value;
        }
    }

    int CodeNotInitialize = -1;
    int CodeReleased = -2;
    int CodeAuthFail = 401;
}
