package researchpartners;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Feature 1: idea cards with categories. One list per category (Hypothesis, Method, Source, Question).
 * Add an idea at the top; select one and press Decide to vote on it, or Remove to delete your own.
 */
class IdeaBoardPage extends JPanel {

    /** One idea in a list. */
    private static final class Item {
        final int id;
        final String author;
        final String text;

        Item(int id, String author, String text) {
            this.id = id;
            this.author = author;
            this.text = text;
        }

        @Override
        public String toString() {
            return "#" + id + "  " + text + "  (" + author + ")";
        }
    }

    /** Draws each idea with the author's name in their colour. */
    private static final class IdeaRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean selected, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            Item item = (Item) value;
            l.setText("<html><b>#" + item.id + "</b> " + Theme.name(item.author) + " &nbsp;" + Theme.esc(item.text)
                    + "</html>");
            return l;
        }
    }

    private final String myNick;
    private final Consumer<String> send;
    private final Consumer<Integer> onDecide;
    private final JComboBox<String> categoryBox = new JComboBox<>(Protocol.CATEGORIES);
    private final JTextField ideaField = new JTextField(30);
    private final JLabel selectedBy = new JLabel(" ");
    private final javax.swing.JTextArea selectedTextArea = new javax.swing.JTextArea(2, 40);
    private final JScrollPane selectedText = new JScrollPane(selectedTextArea);
    private final DefaultListModel<Item>[] models;
    private final JList<Item>[] lists;

    @SuppressWarnings("unchecked")
    IdeaBoardPage(String myNick, Consumer<String> send, Consumer<Integer> onDecide) {
        super(new BorderLayout(8, 8));
        this.myNick = myNick;
        this.send = send;
        this.onDecide = onDecide;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        selectedTextArea.setEditable(false);
        selectedTextArea.setLineWrap(true);
        selectedTextArea.setWrapStyleWord(true);
        selectedText.setBorder(Theme.titled("Selected idea (full text)", Theme.TEAL));
        selectedBy.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 0));

        // top: add an idea
        JPanel add = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton addButton = new JButton("Add idea");
        categoryBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (!selected) {
                    l.setForeground(Theme.categoryColor(String.valueOf(value)));
                }
                l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
                return l;
            }
        });
        add.add(new JLabel("Category:"));
        add.add(categoryBox);
        add.add(new JLabel("Idea:"));
        add.add(ideaField);
        add.add(addButton);
        addButton.addActionListener(e -> addIdea());
        ideaField.addActionListener(e -> addIdea());
        add(add, BorderLayout.NORTH);

        // center: one list per category
        models = new DefaultListModel[Protocol.CATEGORIES.length];
        lists = new JList[Protocol.CATEGORIES.length];
        JPanel grid = new JPanel(new GridLayout(1, Protocol.CATEGORIES.length, 8, 0));
        for (int i = 0; i < Protocol.CATEGORIES.length; i++) {
            final int index = i;
            models[i] = new DefaultListModel<>();
            lists[i] = new JList<Item>(models[i]) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    int row = locationToIndex(e.getPoint());
                    return row < 0 ? null : getModel().getElementAt(row).toString();
                }
            };
            lists[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            lists[i].setBackground(Theme.tint(Theme.categoryColor(Protocol.CATEGORIES[i]), 0.09f));
            lists[i].setCellRenderer(new IdeaRenderer());
            lists[i].setToolTipText("");
            lists[i].addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && lists[index].getSelectedIndex() >= 0) {
                    for (int k = 0; k < lists.length; k++) {
                        if (k != index) {
                            lists[k].clearSelection();
                        }
                    }
                    Item item = lists[index].getSelectedValue();
                    selectedTextArea.setText(item == null ? "" : item.text);
                    selectedBy.setText(item == null ? " " : "<html>" + Theme.name(item.author) + " pinned this "
                            + Theme.esc(Protocol.CATEGORIES[index].toLowerCase()) + " (idea #" + item.id + ")</html>");
                }
            });
            JScrollPane scroll = new JScrollPane(lists[i], JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(Theme.titled(Protocol.CATEGORIES[i], Theme.categoryColor(Protocol.CATEGORIES[i])));
            grid.add(scroll);
        }
        add(grid, BorderLayout.CENTER);

        // bottom: full text of the selected idea, then actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton decide = new JButton("Decide on selected idea");
        JButton remove = new JButton("Remove selected idea");
        decide.addActionListener(e -> {
            Item item = selected();
            if (item == null) {
                nothingSelected();
            } else {
                onDecide.accept(item.id);
            }
        });
        remove.addActionListener(e -> {
            Item item = selected();
            if (item == null) {
                nothingSelected();
            } else {
                send.accept(Protocol.build("I_DEL", String.valueOf(item.id)));
            }
        });
        actions.add(decide);
        actions.add(remove);
        actions.add(new JLabel("(you can only remove your own ideas)"));
        JPanel south = new JPanel(new BorderLayout(0, 6));
        JPanel info = new JPanel(new BorderLayout());
        info.add(selectedBy, BorderLayout.NORTH);
        info.add(selectedText, BorderLayout.CENTER);
        south.add(info, BorderLayout.CENTER);
        south.add(actions, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private void addIdea() {
        String text = ideaField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        send.accept(Protocol.build("I_ADD", String.valueOf(categoryBox.getSelectedItem()), text));
        ideaField.setText("");
    }

    private Item selected() {
        for (JList<Item> l : lists) {
            if (l.getSelectedValue() != null) {
                return l.getSelectedValue();
            }
        }
        return null;
    }

    private void nothingSelected() {
        javax.swing.JOptionPane.showMessageDialog(this, "Select an idea in one of the lists first.",
                "No idea selected", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /** Rebuilds the lists from the latest I_SET messages (one String[] per active idea). */
    void setIdeas(Collection<String[]> ideas) {
        Item keep = selected();
        for (int i = 0; i < models.length; i++) {
            models[i].clear();
            List<String[]> mine = new ArrayList<>();
            for (String[] idea : ideas) {
                if (Protocol.CATEGORIES[i].equalsIgnoreCase(Protocol.arg(idea, 3))) {
                    mine.add(idea);
                }
            }
            for (String[] idea : mine) {
                Item item = new Item(parse(Protocol.arg(idea, 1)), Protocol.arg(idea, 2), Protocol.arg(idea, 4));
                models[i].addElement(item);
                if (keep != null && keep.id == item.id) {
                    lists[i].setSelectedValue(item, true);
                }
            }
        }
    }

    boolean isMine(String author) {
        return author.equalsIgnoreCase(myNick);
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
