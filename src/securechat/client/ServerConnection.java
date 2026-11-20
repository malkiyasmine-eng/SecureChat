package securechat.client;

import securechat.common.CaesarCipher;
import securechat.common.Protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ServerConnection {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int shift;
    private String username;
    private volatile boolean closing = false;

    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sender-thread");
        t.setDaemon(true);
        return t;
    });

    void connect(String host, int port, String username, int shift) throws IOException {
        this.username = username;
        this.shift = shift;

        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            out.writeUTF(Protocol.JOIN);
            out.writeUTF(username);
            out.flush();

            String reply = in.readUTF();
            if (Protocol.ERR.equals(reply)) {
                throw new IOException(in.readUTF());
            }
            if (!Protocol.OK.equals(reply)) {
                throw new IOException("Unexpected reply from server: " + reply);
            }
            socket.setSoTimeout(0); // from now on, wait for messages without a time limit
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /** Starts the thread that listens for incoming messages. */
    void startReceiving(ReceiverThread.Listener listener) {
        new ReceiverThread(this, in, shift, listener).start();
    }

    /** Encrypts and queues a message for sending; returns immediately. */
    void sendMessage(String plainText) {
        final String encrypted = CaesarCipher.encrypt(plainText, shift);
        final String time = Protocol.now();
        sender.execute(() -> {
            try {
                out.writeUTF(Protocol.MSG);
                out.writeUTF(username);
                out.writeUTF(time);
                out.writeUTF(encrypted);
                out.flush();
            } catch (IOException e) {
                close(); // the ReceiverThread will notice and tell the user
            }
        });
    }

    /** Polite disconnect: tells the server, then closes. Returns immediately. */
    void disconnect() {
        closing = true;
        sender.execute(() -> {
            try {
                out.writeUTF(Protocol.LEAVE);
                out.flush();
            } catch (IOException ignored) {
            }
            close();
        });
        sender.shutdown();
    }

    boolean isClosing() {
        return closing;
    }

    void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
