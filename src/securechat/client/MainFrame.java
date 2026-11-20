package securechat.client;

import securechat.common.Protocol;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.Color;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("serial")
class MainFrame extends JFrame {

    private static final String CARD_CONNECT = "connect";
    private static final String CARD_CHAT = "chat";
    private static final Color GREEN = new Color(0x1B5E20);
    private static final Color RED = new Color(0xB71C1C);
    private static final Color ORANGE = new Color(0xE65100);

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final ConnectionPanel connectionPanel = new ConnectionPanel();
    private final ChatPanel chatPanel = new ChatPanel();

    private ServerConnection connection;
    private String myUsername = "";

    MainFrame() {
        super("SecureChat");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        root.add(connectionPanel, CARD_CONNECT);
        root.add(chatPanel, CARD_CHAT);
        setContentPane(root);

        connectionPanel.addConnectListener(e -> onConnectClicked());
        chatPanel.addSendListener(e -> onSendClicked());
        chatPanel.addDisconnectListener(e -> onDisconnectClicked());

        setSize(560, 520);
        setMinimumSize(new java.awt.Dimension(420, 400));
        setLocationRelativeTo(null);
    }

    private void onConnectClicked() {
        final String host = connectionPanel.getHost();
        final String username = connectionPanel.getUsername();
        final int port;
        final int shift;

        if (host.isEmpty()) {
            fail("Please enter the server IPv4 address.");
            return;
        }
        try {
            port = Integer.parseInt(connectionPanel.getPortText());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            fail("The port must be a number between 1 and 65535.");
            return;
        }
        try {
            shift = Integer.parseInt(connectionPanel.getKeyText());
        } catch (NumberFormatException e) {
            fail("The shared key must be a whole number (for example 3).");
            return;
        }
        if (!Protocol.isValidUsername(username)) {
            fail("Please enter a username (1-" + Protocol.MAX_USERNAME_LENGTH + " characters).");
            return;
        }

        connectionPanel.setBusy(true);
        connectionPanel.setStatus("connecting to " + host + ":" + port + " ...", ORANGE);

        // The connection can take seconds, so it runs OFF the Swing thread.
        final ServerConnection conn = new ServerConnection();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                conn.connect(host, port, username, shift);
                return null;
            }

            @Override
            protected void done() { // runs back on the Swing thread
                try {
                    get();
                    onConnected(conn, username);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    connectionPanel.setBusy(false);
                    connectionPanel.setStatus("failed - " + cause.getMessage(), RED);
                } catch (InterruptedException e) {
                    connectionPanel.setBusy(false);
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void onConnected(ServerConnection conn, String username) {
        connection = conn;
        myUsername = username;
        chatPanel.clearConversation();
        chatPanel.setStatus("connected as " + username, GREEN);
        chatPanel.appendSystem("Connected. Messages are Caesar-encrypted on the network.");
        cards.show(root, CARD_CHAT);
        chatPanel.focusInput();

        conn.startReceiving(new ReceiverThread.Listener() {
            // These three methods run on the ReceiverThread -> hop to the Swing thread.
            @Override
            public void onMessage(String user, String time, String text) {
                SwingUtilities.invokeLater(() -> {
                    if (connection == conn) {
                        chatPanel.appendMessage(user, time, text, user.equalsIgnoreCase(myUsername));
                    }
                });
            }

            @Override
            public void onSystem(String text) {
                SwingUtilities.invokeLater(() -> {
                    if (connection == conn) {
                        chatPanel.appendSystem(text);
                    }
                });
            }

            @Override
            public void onDisconnected(String reason) {
                SwingUtilities.invokeLater(() -> {
                    if (connection != conn) {
                        return; // user already left on purpose
                    }
                    connection = null;
                    showConnectionScreen("disconnected - " + reason, RED);
                });
            }
        });
    }

    private void onSendClicked() {
        String text = chatPanel.getInputText().trim();
        if (text.isEmpty() || connection == null) {
            return;
        }
        if (text.length() > Protocol.MAX_MESSAGE_LENGTH) {
            JOptionPane.showMessageDialog(this,
                    "Message too long (max " + Protocol.MAX_MESSAGE_LENGTH + " characters).",
                    "SecureChat", JOptionPane.WARNING_MESSAGE);
            return;
        }
        connection.sendMessage(text); // returns immediately; network work is in another thread
        chatPanel.clearInput();
    }

    private void onDisconnectClicked() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        showConnectionScreen("not connected", Color.DARK_GRAY);
    }

    private void showConnectionScreen(String status, Color color) {
        connectionPanel.setBusy(false);
        connectionPanel.setStatus(status, color);
        cards.show(root, CARD_CONNECT);
    }

    private void fail(String message) {
        connectionPanel.setStatus(message, RED);
    }
}
