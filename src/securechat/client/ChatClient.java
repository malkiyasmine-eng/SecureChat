package securechat.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the client application.
 *
 * Run: java -cp out securechat.client.ChatClient
 */
public class ChatClient {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {

        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
