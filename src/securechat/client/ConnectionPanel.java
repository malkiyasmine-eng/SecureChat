package securechat.client;

import securechat.common.Protocol;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

@SuppressWarnings("serial")
class ConnectionPanel extends JPanel {

    private final JTextField ipField = new JTextField(16);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 16);
    private final JTextField userField = new JTextField(16);
    private final JTextField keyField = new JTextField(String.valueOf(Protocol.DEFAULT_SHIFT), 16);
    private final JButton connectButton = new JButton("Connect");
    private final JLabel statusLabel = new JLabel("Status: not connected");

    ConnectionPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("SecureChat");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        add(title, c);

        JLabel subtitle = new JLabel("Caesar-encrypted chat (educational demo)");
        c.gridy = 1;
        add(subtitle, c);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        addRow(c, 2, "Server IPv4 address:", ipField);
        addRow(c, 3, "Port:", portField);
        addRow(c, 4, "Username:", userField);
        addRow(c, 5, "Shared key (shift):", keyField);

        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(connectButton, c);

        c.gridy = 7;
        c.fill = GridBagConstraints.NONE;
        add(statusLabel, c);

        ipField.setToolTipText("Example: 192.168.1.15 (shown by 'ipconfig' on the server PC)");
        keyField.setToolTipText("All users must type the same number");
    }

    private void addRow(GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        add(new JLabel(label), c);
        c.gridx = 1;
        add(field, c);
    }

    String getHost() {
        return ipField.getText().trim();
    }

    String getPortText() {
        return portField.getText().trim();
    }

    String getUsername() {
        return userField.getText().trim();
    }

    String getKeyText() {
        return keyField.getText().trim();
    }

    void addConnectListener(ActionListener l) {
        connectButton.addActionListener(l);

        ipField.addActionListener(l);
        portField.addActionListener(l);
        userField.addActionListener(l);
        keyField.addActionListener(l);
    }

    void setBusy(boolean busy) {
        connectButton.setEnabled(!busy);
        ipField.setEnabled(!busy);
        portField.setEnabled(!busy);
        userField.setEnabled(!busy);
        keyField.setEnabled(!busy);
    }

    void setStatus(String text, Color color) {
        statusLabel.setText("Status: " + text);
        statusLabel.setForeground(color);
    }
}
