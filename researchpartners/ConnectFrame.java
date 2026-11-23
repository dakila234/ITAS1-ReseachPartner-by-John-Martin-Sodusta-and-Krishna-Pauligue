package researchpartners;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Window that hosts the join screen. */
public class ConnectFrame extends JFrame {

    public ConnectFrame() {
        super("Research Partners - Join");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ConnectView view = new ConnectView((conn, nick, host, port) -> {
            new MainFrame(conn, nick, host, port).setVisible(true);
            dispose();
        });
        setContentPane(view);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        Theme.apply();
        SwingUtilities.invokeLater(() -> {
            ConnectFrame f = new ConnectFrame();
            f.setDefaultCloseOperation(EXIT_ON_CLOSE);
            f.setVisible(true);
        });
    }
}
