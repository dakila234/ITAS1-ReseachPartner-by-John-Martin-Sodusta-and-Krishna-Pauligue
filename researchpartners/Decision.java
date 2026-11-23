package researchpartners;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A group decision with recorded votes (who voted for what). Only accessed through DecisionBoard. */
class Decision {
    final int id;
    final String owner;
    final String question;
    final int ideaId;                 // 0 when not linked to an idea card
    final List<String> options;
    boolean decided;

    /** lower-case nickname -> option index, in the order people voted. */
    private final Map<String, Integer> choice = new LinkedHashMap<>();
    /** lower-case nickname -> display name. */
    private final Map<String, String> display = new LinkedHashMap<>();

    Decision(int id, String owner, String question, int ideaId, List<String> options) {
        this.id = id;
        this.owner = owner;
        this.question = question;
        this.ideaId = ideaId;
        this.options = options;
    }

    void record(String voter, int index) {
        String key = voter.toLowerCase(Locale.ROOT);
        choice.remove(key);            // a changed vote moves to the end of the record
        display.remove(key);
        choice.put(key, index);
        display.put(key, voter);
    }

    List<String> votersOf(int index) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : choice.entrySet()) {
            if (e.getValue() == index) {
                out.add(display.get(e.getKey()));
            }
        }
        return out;
    }

    int totalVotes() {
        return choice.size();
    }

    /** What was decided: the winning option, a tie, or "No votes". Empty while the vote is open. */
    String outcome() {
        if (!decided) {
            return "";
        }
        int best = -1;
        List<String> top = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            int n = votersOf(i).size();
            if (n > best) {
                best = n;
                top.clear();
                top.add(options.get(i));
            } else if (n == best) {
                top.add(options.get(i));
            }
        }
        if (best <= 0) {
            return "No votes";
        }
        return top.size() == 1 ? top.get(0) : "Tie: " + String.join(" / ", top);
    }

    /** D_SET|id|owner|question|ideaId|status|outcome|option|voters|option|voters... */
    String toLine() {
        List<String> f = new ArrayList<>();
        f.add(String.valueOf(id));
        f.add(owner);
        f.add(question);
        f.add(String.valueOf(ideaId));
        f.add(decided ? "DECIDED" : "OPEN");
        f.add(outcome());
        for (int i = 0; i < options.size(); i++) {
            f.add(options.get(i));
            f.add(String.join(",", votersOf(i)));
        }
        return Protocol.build("D_SET", f.toArray(new String[0]));
    }
}
