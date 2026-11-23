package researchpartners;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * Wire format shared by the server and the client.
 *
 * One message = one line of text. Fields are URL-encoded and joined with '|',
 * so user text can safely contain '|' or any other character:
 *
 *     COMMAND|field1|field2|...
 */
public final class Protocol {

    public static final int DEFAULT_PORT = 5070;
    public static final int MAX_NICK = 15;
    public static final int MIN_NICK = 2;
    public static final int MAX_MESSAGE = 500;
    public static final int MAX_IDEA = 280;
    public static final int MAX_QUESTION = 140;
    public static final int MAX_OPTION = 40;
    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 5;

    /** The categories an idea card can have, in board order. */
    public static final String[] CATEGORIES = {"Hypothesis", "Method", "Source", "Question"};

    private static final Pattern NICK_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]+");

    private Protocol() { }

    /** Builds one protocol line from a command and its fields. */
    public static String build(String command, String... fields) {
        StringBuilder sb = new StringBuilder(command);
        for (String f : fields) {
            sb.append('|').append(encode(f == null ? "" : f));
        }
        return sb.toString();
    }

    /** Splits a protocol line into [command, field1, field2, ...] (fields decoded). */
    public static String[] parse(String line) {
        String[] raw = line.split("\\|", -1);
        String[] out = new String[raw.length];
        out[0] = raw[0];
        for (int i = 1; i < raw.length; i++) {
            out[i] = decode(raw[i]);
        }
        return out;
    }

    /** Returns field i, or an empty string when the message is shorter than expected. */
    public static String arg(String[] parts, int i) {
        return i < parts.length ? parts[i] : "";
    }

    /** Returns an error message, or null when the nickname is acceptable. */
    public static String validateNickname(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return "Nickname cannot be empty.";
        }
        if (nick.length() < MIN_NICK || nick.length() > MAX_NICK) {
            return "Nickname must be " + MIN_NICK + "-" + MAX_NICK + " characters.";
        }
        if (!NICK_PATTERN.matcher(nick).matches()) {
            return "Nickname may only contain letters, numbers, '_' and '-'.";
        }
        return null;
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return s;
        }
    }
}
