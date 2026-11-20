package securechat.client;

import securechat.common.CaesarCipher;
import securechat.common.Protocol;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

class ReceiverThread extends Thread {

    interface Listener {
        void onMessage(String username, String timestamp, String plainText);

        void onSystem(String text);

        void onDisconnected(String reason);
    }

    private final DataInputStream in;
    private final int shift;
    private final Listener listener;
    private final ServerConnection connection;

    ReceiverThread(ServerConnection connection, DataInputStream in, int shift, Listener listener) {
        super("receiver-thread");
        setDaemon(true);
        this.connection = connection;
        this.in = in;
        this.shift = shift;
        this.listener = listener;
    }

    @Override
    public void run() {
        String reason;
        try {
            while (true) {
                String type = in.readUTF();
                if (Protocol.MSG.equals(type)) {
                    String user = in.readUTF();
                    String time = in.readUTF();
                    String encrypted = in.readUTF();
                    listener.onMessage(user, time, CaesarCipher.decrypt(encrypted, shift));
                } else if (Protocol.SYS.equals(type)) {
                    listener.onSystem(in.readUTF());
                } else {
                    throw new IOException("Malformed frame from server: " + type);
                }
            }
        } catch (EOFException e) {
            reason = "The server closed the connection.";
        } catch (IOException e) {
            reason = "Connection lost: " + e.getMessage();
        } catch (RuntimeException e) {
            reason = "Unexpected error: " + e;
        }
        if (connection.isClosing()) {
            reason = null; // we closed it ourselves
        }
        connection.close();
        listener.onDisconnected(reason);
    }
}
