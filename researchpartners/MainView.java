package researchpartners;

import java.awt.BorderLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * The Research Partners screen: tabs (Idea Board, Decisions) on top and the team chat underneath.
 * Server messages arrive on the reader thread and are moved to the Swing thread with
 * SwingUtilities.invokeLater before any GUI is touched.
 */
class MainView extends JPanel implements ClientConnection.Listener {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ClientConnection connection;
    private final Runnable onExit;

    private final JTabbedPane tabs = new JTabbedPane();
    private final IdeaBoardPage ideaPage;
    private final DecisionsPage decisionPage;
    private final ChatPanel chat;

    private final Map<Integer, String[]> ideas = new LinkedHashMap<>();
    private final Map<Integer, String[]> decisions = new LinkedHashMap<>();

    private boolean connected = true;

    /** @param onExit called after the user leaves or the connection is lost (shows the join screen again) */
    MainView(ClientConnection connection, String nick, String host, int port, Runnable onExit) {
        super(new BorderLayout());
        this.connection = connection;
        this.onExit = onExit;

        chat = new ChatPanel(nick, this::submitInput);
        ideaPage = new IdeaBoardPage(nick, connection::send, this::decideOnIdea);
        decisionPage = new DecisionsPage(nick, connection::send);

        tabs.addTab("Idea Board", ideaPage);
        tabs.addTab("Decisions", decisionPage);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, chat);
        split.setResizeWeight(0.62);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // header band
        JPanel band = new JPanel(new BorderLayout());
        band.setBackground(Theme.TEAL);
        band.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        JLabel title = new JLabel("Research Partners");
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        band.add(title, BorderLayout.WEST);
        JLabel who = new JLabel("<html><font color='#E8F3F2'>Signed in as</font> <b><font color='#FFD9A0'>"
                + Theme.esc(nick) + "</font></b> <font color='#E8F3F2'>@ " + Theme.esc(host) + ":" + port
                + "</font></html>");
        band.add(who, BorderLayout.EAST);
        add(band, BorderLayout.NORTH);

        setPreferredSize(new java.awt.Dimension(1000, 680));
        chat.system("Welcome to Research Partners, " + nick + ". Type /help to see commands.");
        connection.startReading(this);   // start only after the GUI exists
    }

    /** "Decide" on an idea: open a vote about it and show the Decisions tab. */
    private void decideOnIdea(int ideaId) {
        String[] idea = ideas.get(ideaId);
        if (idea == null) {
            return;
        }
        String text = Protocol.arg(idea, 4);
        String shortText = text.length() > 60 ? text.substring(0, 57) + "..." : text;
        connection.send(Protocol.build("D_ADD", "Adopt idea #" + ideaId + ": " + shortText,
                String.valueOf(ideaId), "Adopt", "Revise first", "Drop"));
        tabs.setSelectedComponent(decisionPage);
    }

    void showCommands() {
        JOptionPane.showMessageDialog(this,
                "/list                                  show who is online\n"
                + "/msg <nick> <text>                       private message\n"
                + "/quit                                   leave the chat\n"
                + "/idea Category | text                    pin an idea (" + String.join(", ", Protocol.CATEGORIES) + ")\n"
                + "/decide Question | option | option       open a vote\n"
                + "/vote <decision #> <option #>            vote or change your vote\n"
                + "/close <decision #>                      close your vote and record the outcome\n"
                + "/delidea <#>                             remove your idea",
                "Commands", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---- sending -------------------------------------------------------------------------

    private void submitInput(String text) {
        if (!connected) {
            return;
        }
        if (text.startsWith("/")) {
            runCommand(text);
        } else {
            connection.send(Protocol.build("MSG", text));
        }
    }

    /** Slash commands typed in the chat box. */
    private void runCommand(String text) {
        String[] parts = text.split("\\s+", 3);
        String arg1 = parts.length > 1 ? parts[1] : "";
        String arg2 = parts.length > 2 ? parts[2] : "";
        switch (parts[0].toLowerCase()) {
            case "/help":
                chat.system("Commands: /list  /msg <nick> <text>  /quit  /idea Category | text  "
                        + "/decide Question | option | option  /vote <decision #> <option #>  /close <decision #>  "
                        + "/delidea <#>. Categories: " + String.join(", ", Protocol.CATEGORIES) + ".");
                break;
            case "/list":
                connection.send(Protocol.build("LIST"));
                break;
            case "/quit":
                leave();
                break;
            case "/msg":
                if (parts.length < 3) {
                    chat.error("Usage: /msg <nickname> <text>");
                } else {
                    connection.send(Protocol.build("PM", parts[1], parts[2]));
                }
                break;
            case "/idea": {
                String body = text.length() > 5 ? text.substring(5) : "";
                int bar = body.indexOf('|');
                if (bar < 0) {
                    chat.error("Usage: /idea Category | text");
                } else {
                    connection.send(Protocol.build("I_ADD", body.substring(0, bar).trim(),
                            body.substring(bar + 1).trim()));
                }
                break;
            }
            case "/decide": {
                String body = text.length() > 7 ? text.substring(7) : "";
                String[] f = body.split("\\|");
                List<String> fields = new ArrayList<>();
                fields.add(f[0].trim());
                fields.add("0");
                for (int i = 1; i < f.length; i++) {
                    fields.add(f[i].trim());
                }
                connection.send(Protocol.build("D_ADD", fields.toArray(new String[0])));
                break;
            }
            case "/vote": {
                try {
                    int option = Integer.parseInt(arg2.trim());
                    connection.send(Protocol.build("D_VOTE", arg1, String.valueOf(option - 1)));
                } catch (NumberFormatException e) {
                    chat.error("Usage: /vote <decision number> <option number>");
                }
                break;
            }
            case "/close":
                connection.send(Protocol.build("D_CLOSE", arg1));
                break;
            case "/delidea":
                connection.send(Protocol.build("I_DEL", arg1));
                break;
            default:
                chat.error("Unknown command. Type /help.");
        }
    }

    void leave() {
        if (connected) {
            connected = false;
            connection.disconnect();
        }
        onExit.run();
    }

    // ---- receiving (ClientConnection.Listener) --------------------------------------------

    @Override
    public void onMessage(String[] p) {
        SwingUtilities.invokeLater(() -> handleMessage(p));
    }

    @Override
    public void onClosed() {
        SwingUtilities.invokeLater(() -> {
            if (!connected) {
                return;
            }
            connected = false;
            chat.error("Lost connection to the server.");
            JOptionPane.showMessageDialog(this, "The connection to the server was lost.\n"
                    + "You will be taken back to the join screen.",
                    "Disconnected", JOptionPane.WARNING_MESSAGE);
            onExit.run();
        });
    }

    void handleMessage(String[] p) {
        switch (p[0]) {
            case "MSG":
                chat.chat(Protocol.arg(p, 1), Protocol.arg(p, 2), Protocol.arg(p, 3));
                break;
            case "SYS":
                chat.system(Protocol.arg(p, 2));
                break;
            case "PM":
                chat.privateMessage(Protocol.arg(p, 1), Protocol.arg(p, 2), Protocol.arg(p, 3), Protocol.arg(p, 4));
                break;
            case "ERR":
                chat.error(Protocol.arg(p, 1));
                break;
            case "USERS": {
                List<String> names = new ArrayList<>(Arrays.asList(p).subList(1, p.length));
                Set<String> online = new LinkedHashSet<>(names);
                chat.setUsers(names);
                decisionPage.setOnline(online);
                break;
            }
            case "R_RESET":
                ideas.clear();
                decisions.clear();
                refresh();
                break;
            case "I_SET": {
                int id = parseId(Protocol.arg(p, 1));
                if (id <= 0) {
                    break;
                }
                if ("REMOVED".equals(Protocol.arg(p, 5))) {
                    ideas.remove(id);
                } else {
                    ideas.put(id, p);
                }
                refresh();
                break;
            }
            case "D_SET": {
                int id = parseId(Protocol.arg(p, 1));
                if (id > 0) {
                    decisions.put(id, p);
                    refresh();
                }
                break;
            }
            default:
                break;
        }
    }

    private void refresh() {
        ideaPage.setIdeas(ideas.values());
        decisionPage.setIdeas(ideas);
        decisionPage.setDecisions(decisions.values());
    }

    private static int parseId(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String now() {
        return LocalTime.now().format(TIME);
    }
}
