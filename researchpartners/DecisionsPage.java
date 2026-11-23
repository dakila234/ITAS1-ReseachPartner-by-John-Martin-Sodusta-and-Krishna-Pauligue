package researchpartners;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ButtonGroup;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Feature 2: group decisions with recorded votes.
 * Pick a decision on the left. On the right you see its options and exactly who voted for each one.
 * Choose an option and press Vote (you can change your vote until the owner closes it).
 */
class DecisionsPage extends JPanel {

    private static final class Item {
        final int id;
        final String[] fields;

        Item(int id, String[] fields) {
            this.id = id;
            this.fields = fields;
        }

        @Override
        public String toString() {
            String status = "DECIDED".equals(Protocol.arg(fields, 5)) ? "decided" : "open";
            return "#" + id + " [" + status + "]  " + Protocol.arg(fields, 3);
        }
    }

    /** Draws each decision with a coloured OPEN / DECIDED tag. */
    private static final class DecisionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean selected, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            Item item = (Item) value;
            boolean decided = "DECIDED".equals(Protocol.arg(item.fields, 5));
            l.setText("<html><b>#" + item.id + "</b> <b><font color='" + Theme.hex(decided ? Theme.GREEN : Theme.TEAL)
                    + "'>" + (decided ? "DECIDED" : "OPEN") + "</font></b> &nbsp;"
                    + Theme.esc(Protocol.arg(item.fields, 3)) + "</html>");
            return l;
        }
    }

    private final String myNick;
    private final Consumer<String> send;
    private final DefaultListModel<Item> model = new DefaultListModel<>();
    private final JList<Item> list = new JList<>(model);
    private final JPanel detail = new JPanel();
    private final JTextField questionField = new JTextField(30);
    private final JTextField[] optionFields = new JTextField[4];

    private Map<Integer, String[]> ideas = Collections.emptyMap();
    private Set<String> online = Collections.emptySet();
    private Collection<String[]> last = new ArrayList<>();
    private boolean updating;

    DecisionsPage(String myNick, Consumer<String> send) {
        super(new BorderLayout(8, 8));
        this.myNick = myNick;
        this.send = send;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // left: list of decisions
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.tint(Theme.TEAL, 0.07f));
        list.setCellRenderer(new DecisionRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updating) {
                showDetail();
            }
        });
        JScrollPane left = new JScrollPane(list);
        left.setBorder(Theme.titled("Decisions", Theme.TEAL));
        left.setPreferredSize(new Dimension(300, 0));
        add(left, BorderLayout.WEST);

        // center: details of the selected decision
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        JPanel detailWrap = new JPanel(new BorderLayout());
        detailWrap.add(detail, BorderLayout.NORTH);
        detail.setOpaque(false);
        detailWrap.setOpaque(false);
        JScrollPane center = new JScrollPane(detailWrap);
        center.getViewport().setBackground(Theme.tint(Theme.GOLD, 0.06f));
        center.setBorder(Theme.titled("Votes (who voted for what)", Theme.GOLD));
        add(center, BorderLayout.CENTER);

        // bottom: open a new decision
        JPanel form = new JPanel(new BorderLayout(6, 6));
        form.setBorder(Theme.titled("Open a new decision (2 to 4 options)", Theme.TERRACOTTA));
        JPanel q = new JPanel(new BorderLayout(6, 0));
        q.add(new JLabel("Question:"), BorderLayout.WEST);
        q.add(questionField, BorderLayout.CENTER);
        form.add(q, BorderLayout.NORTH);
        JPanel opts = new JPanel(new GridLayout(1, optionFields.length, 6, 0));
        for (int i = 0; i < optionFields.length; i++) {
            optionFields[i] = new JTextField();
            optionFields[i].setToolTipText("Option " + (i + 1));
            opts.add(optionFields[i]);
        }
        form.add(opts, BorderLayout.CENTER);
        JButton open = new JButton("Open vote");
        open.addActionListener(e -> openVote());
        JPanel east = new JPanel(new BorderLayout());
        east.add(open, BorderLayout.SOUTH);
        form.add(east, BorderLayout.EAST);
        add(form, BorderLayout.SOUTH);

        showDetail();
    }

    private void openVote() {
        List<String> fields = new ArrayList<>();
        fields.add(questionField.getText().trim());
        fields.add("0");
        int options = 0;
        for (JTextField f : optionFields) {
            if (!f.getText().trim().isEmpty()) {
                fields.add(f.getText().trim());
                options++;
            }
        }
        if (fields.get(0).isEmpty()) {
            JOptionPane.showMessageDialog(this, "Write the question for the decision.", "Decision",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (options < 2) {
            JOptionPane.showMessageDialog(this, "Add at least two options.", "Decision",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        send.accept(Protocol.build("D_ADD", fields.toArray(new String[0])));
        questionField.setText("");
        for (JTextField f : optionFields) {
            f.setText("");
        }
    }

    /** Idea cards, used to show which idea a decision is about. */
    void setIdeas(Map<Integer, String[]> ideas) {
        this.ideas = ideas;
        showDetail();
    }

    /** Who is online (the owner of an open decision may have left). */
    void setOnline(Set<String> names) {
        this.online = names;
        showDetail();
    }

    /** Selects a decision (used after pressing Decide on an idea). */
    void select(int id) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id == id) {
                list.setSelectedIndex(i);
                return;
            }
        }
    }

    void setDecisions(Collection<String[]> decisions) {
        last = decisions;
        Item keep = list.getSelectedValue();
        updating = true;
        model.clear();
        List<String[]> sorted = new ArrayList<>(decisions);
        sorted.sort((a, b) -> Integer.compare(parse(Protocol.arg(b, 1)), parse(Protocol.arg(a, 1))));
        for (String[] d : sorted) {
            model.addElement(new Item(parse(Protocol.arg(d, 1)), d));
        }
        int target = -1;
        for (int i = 0; i < model.size(); i++) {
            if (keep != null && model.get(i).id == keep.id) {
                target = i;
            }
        }
        if (target < 0 && model.size() > 0) {
            target = 0;
        }
        if (target >= 0) {
            list.setSelectedIndex(target);
        }
        updating = false;
        showDetail();
    }

    private void showDetail() {
        detail.removeAll();
        Item item = list.getSelectedValue();
        if (item == null) {
            detail.add(row(new JLabel("No decisions yet. Open one below, or press Decide on an idea.")));
            detail.revalidate();
            detail.repaint();
            return;
        }
        String[] d = item.fields;
        final int id = item.id;
        String owner = Protocol.arg(d, 2);
        int ideaId = parse(Protocol.arg(d, 4));
        boolean decided = "DECIDED".equals(Protocol.arg(d, 5));
        String outcome = Protocol.arg(d, 6);
        int optionCount = Math.max(0, (d.length - 7) / 2);

        JLabel question = new JLabel(Protocol.arg(d, 3));
        question.setFont(question.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        detail.add(row(question));
        String about = ideaId > 0 && ideas.get(ideaId) != null
                ? "   |   about idea #" + ideaId + " (" + Protocol.arg(ideas.get(ideaId), 3) + ")" : "";
        detail.add(row(new JLabel("<html>Opened by " + Theme.name(owner)
                + (owner.equalsIgnoreCase(myNick) ? " <font color='gray'>(you)</font>" : "")
                + " &nbsp;|&nbsp; <b><font color='" + Theme.hex(decided ? Theme.GREEN : Theme.TEAL) + "'>"
                + (decided ? "DECIDED" : "OPEN") + "</font></b>" + Theme.esc(about) + "</html>")));
        detail.add(javax.swing.Box.createVerticalStrut(10));

        ButtonGroup group = new ButtonGroup();
        final JRadioButton[] radios = new JRadioButton[optionCount];
        int mine = -1;
        int total = 0;
        for (int i = 0; i < optionCount; i++) {
            List<String> voters = Names.split(Protocol.arg(d, 8 + i * 2));
            total += voters.size();
            for (String v : voters) {
                if (v.equalsIgnoreCase(myNick)) {
                    mine = i;
                }
            }
        }
        for (int i = 0; i < optionCount; i++) {
            List<String> voters = Names.split(Protocol.arg(d, 8 + i * 2));
            radios[i] = new JRadioButton(Protocol.arg(d, 7 + i * 2) + "   -   " + voters.size()
                    + (voters.size() == 1 ? " vote" : " votes"));
            radios[i].setEnabled(!decided);
            radios[i].setSelected(mine == i);
            radios[i].setOpaque(false);
            group.add(radios[i]);
            detail.add(row(radios[i]));
            StringBuilder who = new StringBuilder("<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            if (voters.isEmpty()) {
                who.append("<font color='gray'>no votes yet</font>");
            } else {
                who.append("<font color='gray'>voted:</font> ");
                for (int v = 0; v < voters.size(); v++) {
                    who.append(v > 0 ? ", " : "").append(Theme.name(voters.get(v)));
                }
            }
            who.append("</html>");
            detail.add(row(new JLabel(who.toString())));
        }
        detail.add(javax.swing.Box.createVerticalStrut(10));

        if (decided) {
            JLabel result = new JLabel("Recorded outcome: " + outcome + "  (" + total
                    + (total == 1 ? " vote)" : " votes)"));
            result.setFont(result.getFont().deriveFont(java.awt.Font.BOLD, 14f));
            result.setForeground(Theme.GREEN);
            detail.add(row(result));
        } else {
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            JButton vote = new JButton(mine >= 0 ? "Change my vote" : "Vote");
            vote.addActionListener(e -> {
                for (int i = 0; i < radios.length; i++) {
                    if (radios[i].isSelected()) {
                        send.accept(Protocol.build("D_VOTE", String.valueOf(id), String.valueOf(i)));
                        return;
                    }
                }
                JOptionPane.showMessageDialog(this, "Choose an option first.", "Vote",
                        JOptionPane.INFORMATION_MESSAGE);
            });
            buttons.add(vote);
            if (owner.equalsIgnoreCase(myNick) || !isOnline(owner)) {
                JButton close = new JButton("Close and record outcome");
                close.addActionListener(e -> send.accept(Protocol.build("D_CLOSE", String.valueOf(id))));
                buttons.add(close);
            } else {
                buttons.add(new JLabel("(only " + owner + " can close this decision)"));
            }
            detail.add(row(buttons));
        }
        detail.revalidate();
        detail.repaint();
    }

    private static JPanel row(java.awt.Component c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setOpaque(false);
        p.add(c);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private boolean isOnline(String name) {
        for (String n : online) {
            if (n.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
