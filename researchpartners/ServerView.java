package researchpartners;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Server window: choose the port, start or stop the server, watch the log. */
class ServerView extends JPanel implements ChatServer.Listener {

    private final ChatServer server = new ChatServer(this);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 6);
    private final JButton toggleButton = new JButton("Start server");
    private final JLabel statusLabel = new JLabel("Stopped");
    private final JLabel usersLabel = new JLabel("Online: 0");
    private final JTextArea logArea = new JTextArea();

    ServerView() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(new JLabel("Port:"));
        controls.add(portField);
        controls.add(toggleButton);
        statusLabel.setForeground(Theme.RED);
        controls.add(statusLabel);
        controls.add(usersLabel);

        JLabel ips = new JLabel("Clients on other computers connect to: " + localAddresses());
        ips.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.add(controls, BorderLayout.NORTH);
        top.add(ips, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        toggleButton.addActionListener(e -> toggle());
        setPreferredSize(new java.awt.Dimension(560, 380));
    }

    void shutdown() {
        server.stop();
    }

    private void toggle() {
        if (server.isRunning()) {
            server.stop();
            toggleButton.setText("Start server");
            portField.setEnabled(true);
            statusLabel.setText("Stopped");
            statusLabel.setForeground(Theme.RED);
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1024 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port must be a number from 1024 to 65535.",
                    "Invalid port", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            server.start(port);
            toggleButton.setText("Stop server");
            portField.setEnabled(false);
            statusLabel.setText("Running on port " + port);
            statusLabel.setForeground(Theme.GREEN);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not start the server:\n" + ex.getMessage()
                    + "\n\nIs another server already using this port?",
                    "Start failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Called from server threads, so hop onto the Swing thread. */
    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onUserCountChanged(int count) {
        SwingUtilities.invokeLater(() -> usersLabel.setText("Online: " + count));
    }

    private static String localAddresses() {
        StringBuilder sb = new StringBuilder();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(a.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            // just show nothing
        }
        return sb.length() == 0 ? "(no network found)" : sb.toString();
    }
}
