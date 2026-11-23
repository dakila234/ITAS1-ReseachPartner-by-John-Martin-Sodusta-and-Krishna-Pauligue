package researchpartners;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Join screen: server address, port and nickname. */
class ConnectView extends JPanel {

    interface Joined {
        void joined(ClientConnection connection, String nick, String host, int port);
    }

    private final Joined callback;
    private final JTextField hostField = new JTextField("localhost", 16);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 16);
    private final JTextField nickField = new JTextField(16);
    private final JButton joinButton = new JButton("Join the team");
    private final JLabel statusLabel = new JLabel(" ");

    ConnectView(Joined callback) {
        super(new GridBagLayout());
        this.callback = callback;
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        JLabel title = new JLabel("Research Partners - join a team");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        title.setForeground(Theme.TEAL);
        add(title, c);

        c.gridwidth = 1;
        addRow(c, 1, "Server address:", hostField);
        addRow(c, 2, "Port:", portField);
        addRow(c, 3, "Nickname:", nickField);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(joinButton, c);
        c.gridy = 5;
        statusLabel.setForeground(java.awt.Color.RED);
        add(statusLabel, c);

        joinButton.addActionListener(e -> connect());
        nickField.addActionListener(e -> connect());
    }

    private void addRow(GridBagConstraints c, int row, String label, JTextField field) {
        c.gridy = row;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(field, c);
    }

    private void connect() {
        final String host = hostField.getText().trim();
        final String nick = nickField.getText().trim();
        final int port;

        if (host.isEmpty()) {
            showError("Enter the server address.");
            return;
        }
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showError("Port must be a number from 1 to 65535.");
            return;
        }
        String nickProblem = Protocol.validateNickname(nick);
        if (nickProblem != null) {
            showError(nickProblem);
            return;
        }

        setBusy(true);
        statusLabel.setForeground(java.awt.Color.DARK_GRAY);
        statusLabel.setText("Connecting...");

        // Networking runs off the Swing thread so the window never freezes.
        new SwingWorker<ClientConnection, Void>() {
            @Override
            protected ClientConnection doInBackground() throws Exception {
                ClientConnection conn = new ClientConnection();
                conn.connect(host, port, nick);
                return conn;
            }

            @Override
            protected void done() {
                try {
                    callback.joined(get(), nick, host, port);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showError(friendly(cause));
                    setBusy(false);
                }
            }
        }.execute();
    }

    private static String friendly(Throwable t) {
        String m = t.getMessage();
        if (t instanceof java.net.ConnectException) {
            return "Could not reach the server. Is it running, and are the address and port right?";
        }
        if (t instanceof java.net.UnknownHostException) {
            return "Unknown server address: " + m;
        }
        if (t instanceof java.net.SocketTimeoutException) {
            return "The server did not answer in time.";
        }
        return m == null ? "Connection failed." : m;
    }

    private void showError(String message) {
        statusLabel.setForeground(java.awt.Color.RED);
        statusLabel.setText(message);
    }

    private void setBusy(boolean busy) {
        joinButton.setEnabled(!busy);
        hostField.setEnabled(!busy);
        portField.setEnabled(!busy);
        nickField.setEnabled(!busy);
    }
}
