package com.github.bati;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class Connection implements IConnection {
    private IConfigProvider configProvider;
    private LinkedList<IConnectionProto> connectionProtos;
    private LinkedList<IDataProto> dataProtos;
    private Status status;
    private IStatusListener statusListener;
    private OkHttpClient client;
    private SocketLister socketLister;

    public Connection(){
        connectionProtos = new LinkedList<IConnectionProto>();
        dataProtos = new LinkedList<>();
        status = Status.NotInitialize;
    }

    @Override
    public IConnection Setup(IConfigProvider config, IStatusListener statusListener) {
        this.configProvider = config;
        this.statusListener = statusListener;
        return this;
    }

    @Override
    public IConnection Insert(IConnectionProto proto) {
        this.connectionProtos.add(proto);
        return this;
    }

    @Override
    public IConnection Insert(IDataProto proto) {
        this.dataProtos.add(proto);
        return this;
    }

    private void notifyError(int code) {
        if(this.statusListener != null) {
            this.statusListener.onError(code);
        }
    }
    @Override
    public void connect() {
        if (status == Status.Connected || status == Status.Connecting) {
            return;
        }
        if (status == Status.NotInitialize) {
            notifyError(CodeNotInitialize);
            return;
        }
        if (status == Status.Released) {
            notifyError(CodeReleased);
            return;
        }

        if(client == null) {
            OkHttpClient.Builder builder = configProvider.webSocketClientBuilder();
            client = builder.build();
        }
        Request.Builder builder = new Request.Builder().url(configProvider.url());
        Map<String,String> headers = configProvider.headers();
        if (headers != null) {
            for (Map.Entry<String, String> entry: headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        socketLister = new SocketLister();
        WebSocket webSocket = client.newWebSocket(builder.build(), socketLister);
        socketLister.init(webSocket, this);
    }

    void updateStatus(Status status) {

    }

    void onFailure() {

    }

    private static class SocketLister extends WebSocketListener {
        private Connection connection;
        private WebSocket webSocket;
        private Status status;
        SocketLister() {
            status = Status.Connecting;
        }

        void syncStatus() {
            if(connection != null) {
                connection.updateStatus(status);
            }
        }
        public void release() {
            if (status == Status.Released) {
                return;
            }

            status = Status.DisConnecting;
            if (this.webSocket != null) {
//                TODO
                this.webSocket.close(100, "");
                this.webSocket = null;
                this.connection = null;
            }
        }

        void init(WebSocket webSocket, Connection connection) {
            this.webSocket = webSocket;
            this.connection = connection;
            connection.updateStatus(status);
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            if (status == Status.DisConnecting) {
                status = Status.Released;
                return;
            }
            status = Status.DisConnected;
            syncStatus();
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            if (status == Status.DisConnecting) {
                status = Status.Released;
                return;
            }
            status = Status.Failure;
            syncStatus();
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);
            if (status != Status.Connecting) {
                return;
            }
        }
    }

    @Override
    public void disconnect() {
        if (status == Status.Connected || status == Status.Connecting) {
            if(socketLister != null) {
                socketLister.release();
                socketLister = null;
            }
            status = Status.DisConnected;
            return;
        }
    }

    @Override
    public void release() {
        this.disconnect();
        if (status == Status.Released) {
            return;
        }
        status = Status.Released;
        this.configProvider = null;
        this.connectionProtos = null;
        this.dataProtos = null;
        this.statusListener = null;
    }

    @Override
    public Status status() {
        return status;
    }
}
