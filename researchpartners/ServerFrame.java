package researchpartners;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Window that hosts the server console. Closing it stops the server. */
public class ServerFrame extends JFrame {

    public ServerFrame() {
        super("Research Partners - Host Console");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        final ServerView view = new ServerView();
        setContentPane(view);
        pack();
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                view.shutdown();
            }
        });
    }

    public static void main(String[] args) {
        Theme.apply();
        SwingUtilities.invokeLater(() -> {
            ServerFrame f = new ServerFrame();
            f.setDefaultCloseOperation(EXIT_ON_CLOSE);
            f.setVisible(true);
        });
    }
}
