package securechat.server;

import securechat.common.Protocol;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int port;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public ChatServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = Protocol.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port '" + args[0] + "', using " + port);
            }
        }
        new ChatServer(port).start();
    }

    public void start() {
        try {
            // new ServerSocket(port) with no address = listen on ALL network interfaces.
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            log("Cannot start server on port " + port + ": " + e.getMessage());
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        printBanner();

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                Thread t = new Thread(handler, "client-" + socket.getRemoteSocketAddress());
                t.start();
            } catch (IOException e) {
                if (running) {
                    log("Accept failed: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (ClientHandler h : clients) {
            h.close();
        }
    }

    /**
     * Adds a client if its username is free. Returns false if the name is already
     * used.
     */
    synchronized boolean register(ClientHandler handler) {
        for (ClientHandler h : clients) {
            if (h.getUsername().equalsIgnoreCase(handler.getUsername())) {
                return false;
            }
        }
        clients.add(handler);
        return true;
    }

    void remove(ClientHandler handler) {
        clients.remove(handler);
    }

    /**
     * Sends an (still encrypted) chat message to every connected client, sender
     * included.
     */
    void broadcastMessage(String username, String timestamp, String encrypted) {
        for (ClientHandler h : clients) {
            h.send(Protocol.MSG, username, timestamp, encrypted);
        }
    }

    /** Sends a plain notice such as "Ali joined the chat". */
    void broadcastSystem(String text) {
        for (ClientHandler h : clients) {
            h.send(Protocol.SYS, text);
        }
    }

    int clientCount() {
        return clients.size();
    }

    static void log(String text) {
        System.out.println("[" + LocalTime.now().format(LOG_TIME) + "] " + text);
    }

    private void printBanner() {
        log("SecureChat server listening on port " + port + " (all network interfaces)");
        log("Clients on your Wi-Fi can connect using one of these IPv4 addresses:");
        try {
            boolean any = false;
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        log("   " + addr.getHostAddress() + "   (" + ni.getDisplayName() + ")");
                        any = true;
                    }
                }
            }
            if (!any) {
                log("   (none found - are you connected to Wi-Fi? try 'ipconfig')");
            }
        } catch (IOException e) {
            log("   (could not list addresses: " + e.getMessage() + ")");
        }
        log("Press Ctrl+C to stop.");
    }
}
