package researchpartners;

/** One idea card. Only accessed through IdeaBoard. */
class Idea {
    final int id;
    final String author;
    final String category;
    final String text;
    boolean removed;

    Idea(int id, String author, String category, String text) {
        this.id = id;
        this.author = author;
        this.category = category;
        this.text = text;
    }

    /** I_SET|id|author|category|text|status */
    String toLine() {
        return Protocol.build("I_SET", String.valueOf(id), author, category, text, removed ? "REMOVED" : "ACTIVE");
    }
}
