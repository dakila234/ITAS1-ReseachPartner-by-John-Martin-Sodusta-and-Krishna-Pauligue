package researchpartners;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side state of research feature #1: idea cards with categories.
 * Synchronized because many ClientHandler threads use it at once.
 */
class IdeaBoard {

    private static final int MAX_IDEAS = 200;

    private final List<Idea> ideas = new ArrayList<>();
    private int nextId = 1;

    synchronized void reset() {
        ideas.clear();
        nextId = 1;
    }

    synchronized Result add(String author, String category, String text) {
        String cat = matchCategory(category);
        if (cat == null) {
            return Result.fail("Category must be one of: " + String.join(", ", Protocol.CATEGORIES) + ".");
        }
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) {
            return Result.fail("An idea card cannot be empty.");
        }
        if (text.length() > Protocol.MAX_IDEA) {
            text = text.substring(0, Protocol.MAX_IDEA);
        }
        if (ideas.size() >= MAX_IDEAS) {
            return Result.fail("The idea board is full.");
        }
        Idea idea = new Idea(nextId++, author, cat, text);
        ideas.add(idea);
        return Result.ok(idea.toLine(), author + " pinned a " + cat.toLowerCase() + " (idea #" + idea.id + ").");
    }

    synchronized Result remove(int id, String user) {
        Idea idea = find(id);
        if (idea == null || idea.removed) {
            return Result.fail("No idea with number " + id + ".");
        }
        if (!idea.author.equalsIgnoreCase(user)) {
            return Result.fail("Only " + idea.author + " can remove idea #" + id + ".");
        }
        idea.removed = true;
        return Result.ok(idea.toLine(), user + " removed idea #" + id + ".");
    }

    synchronized boolean exists(int id) {
        Idea idea = find(id);
        return idea != null && !idea.removed;
    }

    /** All active ideas as protocol lines (used when a new client joins). */
    synchronized List<String> snapshotLines() {
        List<String> lines = new ArrayList<>();
        for (Idea i : ideas) {
            if (!i.removed) {
                lines.add(i.toLine());
            }
        }
        return lines;
    }

    private Idea find(int id) {
        for (Idea i : ideas) {
            if (i.id == id) {
                return i;
            }
        }
        return null;
    }

    private static String matchCategory(String c) {
        if (c == null) {
            return null;
        }
        for (String known : Protocol.CATEGORIES) {
            if (known.equalsIgnoreCase(c.trim())) {
                return known;
            }
        }
        return null;
    }
}
