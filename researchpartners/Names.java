package researchpartners;

import java.util.ArrayList;
import java.util.List;

/** Helper for the comma-separated name lists inside W_SET messages. */
final class Names {

    private Names() { }

    static List<String> split(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isEmpty()) {
            return out;
        }
        for (String n : csv.split(",")) {
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        return out;
    }

    static int count(String csv) {
        return split(csv).size();
    }
}
