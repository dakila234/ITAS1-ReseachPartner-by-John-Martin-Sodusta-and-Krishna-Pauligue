package researchpartners;

/** Result of a board operation: a line to broadcast (plus optional chat notice) or an error. */
final class Result {
    final String line;
    final String info;
    final String error;

    private Result(String line, String info, String error) {
        this.line = line;
        this.info = info;
        this.error = error;
    }

    static Result ok(String line, String info) { return new Result(line, info, null); }
    static Result fail(String error) { return new Result(null, null, error); }
    boolean isOk() { return error == null; }
}
