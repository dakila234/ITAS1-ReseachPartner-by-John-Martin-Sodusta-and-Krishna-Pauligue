package researchpartners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Server-side state of research feature #2: group decisions with recorded votes.
 * Votes are stored per person, so everyone can see who voted for what. A vote can be
 * changed until the decision is closed; closing records the outcome.
 */
class DecisionBoard {

    private static final int MAX_DECISIONS = 100;

    private final List<Decision> decisions = new ArrayList<>();
    private int nextId = 1;

    synchronized void reset() {
        decisions.clear();
        nextId = 1;
    }

    synchronized Result open(String owner, String question, int ideaId, List<String> rawOptions) {
        question = question == null ? "" : question.trim();
        if (question.isEmpty()) {
            return Result.fail("A decision needs a question.");
        }
        if (question.length() > Protocol.MAX_QUESTION) {
            question = question.substring(0, Protocol.MAX_QUESTION);
        }
        List<String> options = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String o : rawOptions) {
            String t = o == null ? "" : o.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > Protocol.MAX_OPTION) {
                t = t.substring(0, Protocol.MAX_OPTION);
            }
            if (seen.add(t.toLowerCase(Locale.ROOT))) {
                options.add(t);
            }
        }
        if (options.size() < Protocol.MIN_OPTIONS || options.size() > Protocol.MAX_OPTIONS) {
            return Result.fail("A decision needs " + Protocol.MIN_OPTIONS + " to " + Protocol.MAX_OPTIONS
                    + " different options.");
        }
        if (decisions.size() >= MAX_DECISIONS) {
            return Result.fail("Too many decisions. Close old ones first.");
        }
        Decision d = new Decision(nextId++, owner, question, ideaId, options);
        decisions.add(d);
        return Result.ok(d.toLine(), owner + " opened decision #" + d.id + ": " + question);
    }

    synchronized Result vote(int id, String user, int index) {
        Decision d = find(id);
        if (d == null) {
            return Result.fail("No decision with number " + id + ".");
        }
        if (d.decided) {
            return Result.fail("Decision #" + id + " is already closed.");
        }
        if (index < 0 || index >= d.options.size()) {
            return Result.fail("That option does not exist.");
        }
        d.record(user, index);
        return Result.ok(d.toLine(), null);
    }

    /** Anyone may close the decision if its owner has left. */
    synchronized Result close(int id, String user, boolean ownerOnline) {
        Decision d = find(id);
        if (d == null) {
            return Result.fail("No decision with number " + id + ".");
        }
        if (d.decided) {
            return Result.fail("Decision #" + id + " is already closed.");
        }
        if (ownerOnline && !d.owner.equalsIgnoreCase(user)) {
            return Result.fail("Only " + d.owner + " can close decision #" + id + ".");
        }
        d.decided = true;
        return Result.ok(d.toLine(), "Decision #" + id + " recorded: " + d.outcome()
                + " (" + d.totalVotes() + (d.totalVotes() == 1 ? " vote)." : " votes)."));
    }

    synchronized String ownerOf(int id) {
        Decision d = find(id);
        return d == null ? "" : d.owner;
    }

    synchronized List<String> snapshotLines() {
        List<String> lines = new ArrayList<>();
        for (Decision d : decisions) {
            lines.add(d.toLine());
        }
        return lines;
    }

    private Decision find(int id) {
        for (Decision d : decisions) {
            if (d.id == id) {
                return d;
            }
        }
        return null;
    }
}
