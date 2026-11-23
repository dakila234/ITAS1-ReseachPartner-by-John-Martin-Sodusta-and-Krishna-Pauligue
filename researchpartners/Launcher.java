package researchpartners;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Starting window: start the server or open a client. */
public class Launcher extends JFrame {

    public Launcher() {
        super("Research Partners");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildView());
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    static JPanel buildView() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Research Partners", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        title.setForeground(Theme.TEAL);
        root.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8));
        JButton server = new JButton("Start Server (host computer)");
        JButton client = new JButton("Open Client (join a team)");
        server.addActionListener(e -> new ServerFrame().setVisible(true));
        client.addActionListener(e -> new ConnectFrame().setVisible(true));
        buttons.add(server);
        buttons.add(client);
        root.add(buttons, BorderLayout.CENTER);

        root.add(new JLabel("Run the server once, then open a client for each person.",
                SwingConstants.CENTER), BorderLayout.SOUTH);
        return root;
    }

    public static void main(String[] args) {
        Theme.apply();
        SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
    }
}
