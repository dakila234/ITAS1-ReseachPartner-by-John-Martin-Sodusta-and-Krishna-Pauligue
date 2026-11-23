package researchpartners;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Team chat: a log where every person's name has their own colour, the online list on the right
 * (names in the same colours) and an input line underneath.
 */
class ChatPanel extends JPanel {

    private final String myNick;
    private final JTextPane log = new JTextPane();
    private final JTextField input = new JTextField();
    private final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JScrollPane usersScroll;

    ChatPanel(String myNick, Consumer<String> onInput) {
        super(new BorderLayout(6, 6));
        this.myNick = myNick;
        setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));

        log.setEditable(false);
        log.setBackground(Theme.tint(Theme.TEAL, 0.05f));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(Theme.titled("Team chat", Theme.TEAL));
        add(logScroll, BorderLayout.CENTER);

        JList<String> users = new JList<>(usersModel);
        users.setBackground(Theme.tint(Theme.TEAL, 0.05f));
        users.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, false, false);
                String name = String.valueOf(value);
                l.setText("<html><font color='" + Theme.hex(Theme.nameColor(name)) + "'>&#9679;</font>&nbsp; "
                        + Theme.name(name) + (name.equalsIgnoreCase(myNick) ? " <font color='gray'>(you)</font>" : "")
                        + "</html>");
                l.setOpaque(false);
                return l;
            }
        });
        usersScroll = new JScrollPane(users);
        usersScroll.setBorder(Theme.titled("Online", Theme.TEAL));
        usersScroll.setPreferredSize(new Dimension(160, 0));
        add(usersScroll, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        JButton send = new JButton("Send");
        Runnable submit = () -> {
            String t = input.getText().trim();
            if (!t.isEmpty()) {
                input.setText("");
                onInput.accept(t);
            }
        };
        send.addActionListener(e -> submit.run());
        input.addActionListener(e -> submit.run());
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    // ---- writing into the log ------------------------------------------------------------

    private void append(String text, Color color, boolean bold, boolean italic) {
        StyledDocument doc = log.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, bold);
        StyleConstants.setItalic(a, italic);
        try {
            doc.insertString(doc.getLength(), text, a);
        } catch (BadLocationException ignored) {
            // cannot happen: we always insert at the end
        }
        log.setCaretPosition(doc.getLength());   // auto-scroll
    }

    void chat(String time, String sender, String text) {
        append("[" + time + "] ", Theme.MUTED, false, false);
        append(sender, Theme.nameColor(sender), true, false);
        append(": ", Color.DARK_GRAY, false, false);
        append(text + "\n", Color.BLACK, false, false);
    }

    void privateMessage(String time, String from, String to, String text) {
        append("[" + time + "] ", Theme.MUTED, false, false);
        append("(private) ", Theme.PLUM, false, true);
        append(from, Theme.nameColor(from), true, false);
        append(" -> ", Color.DARK_GRAY, false, false);
        append(to, Theme.nameColor(to), true, false);
        append(": ", Color.DARK_GRAY, false, false);
        append(text + "\n", Theme.PLUM, false, true);
    }

    void system(String text) {
        append("* " + text + "\n", Theme.MUTED, false, true);
    }

    void error(String text) {
        append("! " + text + "\n", Theme.RED, true, false);
    }

    void setUsers(List<String> names) {
        usersModel.clear();
        for (String n : names) {
            usersModel.addElement(n);
        }
        usersScroll.setBorder(Theme.titled("Online (" + names.size() + ")", Theme.TEAL));
    }

    void prefill(String text) {
        input.setText(text);
        input.requestFocusInWindow();
    }

    void focusInput() {
        input.requestFocusInWindow();
    }
}
