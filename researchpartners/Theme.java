package researchpartners;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

/**
 * Look and feel: the normal system look (standard Swing), plus a few colours:
 * every person gets their own name colour, categories have a colour, and panels get a pale tint.
 */
final class Theme {

    static final Color TEAL = new Color(0x1F6F78);
    static final Color TERRACOTTA = new Color(0xC8553D);
    static final Color GOLD = new Color(0xB07A12);
    static final Color PLUM = new Color(0x6B4E9B);
    static final Color GREEN = new Color(0x2E7D4F);
    static final Color RED = new Color(0xB3382C);
    static final Color MUTED = new Color(0x6F6F6F);

    /** Name colours, dark enough to read on white or on a pale tint. */
    private static final Color[] NAMES = {
        new Color(0x1F6F78), new Color(0xC8553D), new Color(0x6B4E9B), new Color(0x2E7D4F),
        new Color(0xB07A12), new Color(0x2F55A4), new Color(0xAD1457), new Color(0x00796B)
    };

    private Theme() { }

    static void apply() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep the default look and feel
        }
    }

    /** The colour of a person's name (the same name always gets the same colour). */
    static Color nameColor(String name) {
        int h = name == null ? 0 : name.toLowerCase().hashCode();
        return NAMES[Math.abs(h % NAMES.length)];
    }

    /** Colour of an idea category. */
    static Color categoryColor(String category) {
        if ("Hypothesis".equalsIgnoreCase(category)) {
            return TEAL;
        }
        if ("Method".equalsIgnoreCase(category)) {
            return GOLD;
        }
        if ("Source".equalsIgnoreCase(category)) {
            return TERRACOTTA;
        }
        return PLUM;
    }

    /** A very pale version of a colour, for backgrounds (amount 0 = white, 1 = full colour). */
    static Color tint(Color c, float amount) {
        float u = 1f - amount;
        return new Color(
                Math.round(255 * u + c.getRed() * amount),
                Math.round(255 * u + c.getGreen() * amount),
                Math.round(255 * u + c.getBlue() * amount));
    }

    /** Titled border whose title is bold and coloured. */
    static TitledBorder titled(String title, Color color) {
        TitledBorder b = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(tint(color, 0.45f), 1), title);
        b.setTitleColor(color);
        Font base = UIManager.getFont("TitledBorder.font");
        if (base != null) {
            b.setTitleFont(base.deriveFont(Font.BOLD));
        }
        return b;
    }

    static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** HTML snippet: a person's name, bold and in their colour. */
    static String name(String name) {
        return "<b><font color='" + hex(nameColor(name)) + "'>" + esc(name) + "</font></b>";
    }
}
