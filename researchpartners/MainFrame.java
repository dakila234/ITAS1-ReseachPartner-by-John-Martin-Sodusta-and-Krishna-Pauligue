package researchpartners;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/** Window that hosts the MainView. Closing it leaves the chat and returns to the join screen. */
public class MainFrame extends JFrame {

    private boolean exited = false;

    public MainFrame(ClientConnection connection, String nick, String host, int port) {
        super("Research Partners - " + nick);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        final MainView view = new MainView(connection, nick, host, port, this::backToJoin);
        setContentPane(view);

        JMenuBar bar = new JMenuBar();
        JMenu session = new JMenu("Session");
        JMenuItem leave = new JMenuItem("Leave");
        leave.addActionListener(e -> view.leave());
        session.add(leave);
        JMenu help = new JMenu("Help");
        JMenuItem commands = new JMenuItem("Commands");
        commands.addActionListener(e -> view.showCommands());
        help.add(commands);
        bar.add(session);
        bar.add(help);
        setJMenuBar(bar);

        pack();
        setMinimumSize(new java.awt.Dimension(800, 520));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                view.leave();
            }
        });
    }

    private void backToJoin() {
        if (exited) {
            return;
        }
        exited = true;
        dispose();
        new ConnectFrame().setVisible(true);
    }
}
