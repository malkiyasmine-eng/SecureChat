package securechat.server;

import securechat.common.Protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

class ClientHandler implements Runnable {

    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;

    private final Socket socket;
    private final ChatServer server;
    private final String address;
    private final Object writeLock = new Object();
    private DataInputStream in;
    private DataOutputStream out;
    private volatile String username = "?";
    private boolean registered = false;

    ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.address = String.valueOf(socket.getRemoteSocketAddress());
    }

    String getUsername() {
        return username;
    }

    @Override
    public void run() {
        ChatServer.log("Connection from " + address);
        String reason = "disconnected";
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            if (!handshake()) {
                return;
            }
            socket.setSoTimeout(0); // after joining, wait for messages as long as needed

            send(Protocol.OK);
            ChatServer.log(username + " joined from " + address + " (" + server.clientCount() + " online)");
            server.broadcastSystem(username + " joined the chat");

            readLoop();
        } catch (EOFException e) {
            reason = "connection closed";
        } catch (SocketTimeoutException e) {
            reason = "timed out";
        } catch (IOException e) {
            reason = "I/O error: " + e.getMessage();
        } catch (RuntimeException e) {
            reason = "unexpected error: " + e;
        } finally {
            cleanup(reason);
        }
    }

    /** Reads the JOIN frame. Returns true if the client may enter the chat. */
    private boolean handshake() throws IOException {
        socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
        String type = in.readUTF();
        if (!Protocol.JOIN.equals(type)) {
            reject("Expected JOIN");
            return false;
        }
        String name = in.readUTF().trim();
        if (!Protocol.isValidUsername(name)) {
            reject("Invalid username (1-" + Protocol.MAX_USERNAME_LENGTH + " characters)");
            return false;
        }
        username = name;
        if (!server.register(this)) {
            reject("Username '" + name + "' is already in use");
            return false;
        }
        registered = true;
        return true;
    }

    private void readLoop() throws IOException {
        while (true) {
            String type = in.readUTF();
            if (Protocol.MSG.equals(type)) {
                in.readUTF(); // username sent by the client: ignored, we trust our own record
                String timestamp = in.readUTF();
                String encrypted = in.readUTF();
                if (encrypted.isEmpty()
                        || encrypted.length() > Protocol.MAX_ENCRYPTED_LENGTH
                        || timestamp.length() > Protocol.MAX_TIMESTAMP_LENGTH) {
                    ChatServer.log("Dropped invalid message from " + username);
                    continue; // frame was fully read, so the stream is still in sync
                }
                // The server never decrypts: it just relays the encrypted text.
                server.broadcastMessage(username, timestamp, encrypted);
            } else if (Protocol.LEAVE.equals(type)) {
                return;
            } else {
                // Unknown type: we no longer know where the next frame starts, so stop.
                ChatServer.log("Malformed frame '" + type + "' from " + username + " - closing");
                return;
            }
        }
    }

    private void reject(String why) {
        send(Protocol.ERR, why);
        ChatServer.log("Rejected " + address + ": " + why);
    }

    /**
     * Sends one frame (several strings) atomically. Safe to call from any thread.
     */
    void send(String... fields) {
        synchronized (writeLock) {
            if (out == null) {
                return;
            }
            try {
                for (String f : fields) {
                    out.writeUTF(f);
                }
                out.flush();
            } catch (IOException e) {
                close(); // the reading thread will notice and clean up
            }
        }
    }

    void close() {
        try {
            socket.close(); // also closes the streams
        } catch (IOException ignored) {
        }
    }

    private void cleanup(String reason) {
        close();
        if (registered) {
            server.remove(this);
            ChatServer.log(username + " left (" + reason + ", " + server.clientCount() + " online)");
            server.broadcastSystem(username + " left the chat");
        } else {
            ChatServer.log("Connection from " + address + " closed (" + reason + ")");
        }
    }
}
