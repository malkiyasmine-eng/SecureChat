package securechat.client;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;

@SuppressWarnings("serial")
class ChatPanel extends JPanel {

    private final JTextPane area = new JTextPane();
    private final StyledDocument doc = area.getStyledDocument();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final JLabel statusLabel = new JLabel("Status: connected");

    private final SimpleAttributeSet metaStyle = new SimpleAttributeSet();
    private final SimpleAttributeSet mineStyle = new SimpleAttributeSet();
    private final SimpleAttributeSet otherStyle = new SimpleAttributeSet();
    private final SimpleAttributeSet systemStyle = new SimpleAttributeSet();

    ChatPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        StyleConstants.setBold(metaStyle, true);
        StyleConstants.setForeground(mineStyle, new Color(0x0B5394));
        StyleConstants.setForeground(otherStyle, new Color(0x1B5E20));
        StyleConstants.setItalic(systemStyle, true);
        StyleConstants.setForeground(systemStyle, Color.GRAY);

        area.setEditable(false);
        area.setBackground(Color.WHITE);
        area.setForeground(Color.BLACK);

        JPanel top = new JPanel(new BorderLayout());
        top.add(statusLabel, BorderLayout.WEST);
        top.add(disconnectButton, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    void addSendListener(ActionListener l) {
        sendButton.addActionListener(l);
        input.addActionListener(l);
    }

    void addDisconnectListener(ActionListener l) {
        disconnectButton.addActionListener(l);
    }

    String getInputText() {
        return input.getText();
    }

    void clearInput() {
        input.setText("");
        input.requestFocusInWindow();
    }

    void focusInput() {
        input.requestFocusInWindow();
    }

    void setStatus(String text, Color color) {
        statusLabel.setText("Status: " + text);
        statusLabel.setForeground(color);
    }

    void clearConversation() {
        area.setText("");
    }

    void appendMessage(String user, String time, String text, boolean mine) {
        append("[" + time + "] " + user + (mine ? " (you)" : "") + ": ", metaStyle);
        append(text + "\n", mine ? mineStyle : otherStyle);
    }

    void appendSystem(String text) {
        append("* " + text + "\n", systemStyle);
    }

    private void append(String text, SimpleAttributeSet style) {
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException ignored) {
        }
        area.setCaretPosition(doc.getLength());
    }
}
