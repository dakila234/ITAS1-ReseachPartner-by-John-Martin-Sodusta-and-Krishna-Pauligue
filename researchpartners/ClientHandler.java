package researchpartners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs on its own thread, one per connected client.
 * Reads protocol lines from the client and reacts to them.
 */
class ClientHandler implements Runnable {

    private final ChatServer server;
    private final Socket socket;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private PrintWriter out;
    private volatile String nick = "?";
    private boolean registered = false;

    ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    String getNick() {
        return nick;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(10_000);   // a client must say HELLO within 10 seconds
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            if (!handshake(in)) {
                return;
            }
            socket.setSoTimeout(0);

            String line;
            while (server.isRunningNow() && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    if (!handle(Protocol.parse(line))) {
                        break;      // client asked to quit
                    }
                } catch (RuntimeException e) {
                    send(Protocol.build("ERR", "Server could not process that message."));
                    server.log("Bad message from " + nick + ": " + e);
                }
            }
        } catch (IOException e) {
            // connection dropped; handled in finally
        } finally {
            cleanup();
        }
    }

    /** First message must be HELLO|nickname. Returns true when the client is accepted. */
    private boolean handshake(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            return false;
        }
        String[] p = Protocol.parse(line);
        if (!"HELLO".equals(p[0])) {
            send(Protocol.build("ERR", "Expected HELLO."));
            return false;
        }
        String wanted = Protocol.arg(p, 1).trim();
        String problem = Protocol.validateNickname(wanted);
        if (problem != null) {
            send(Protocol.build("ERR", problem));
            return false;
        }
        nick = wanted;
        if (!server.register(nick, this)) {
            send(Protocol.build("ERR", "The nickname '" + nick + "' is already taken."));
            return false;
        }
        registered = true;

        send(Protocol.build("OK", nick));
        // bring the new client up to date
        send(Protocol.build("R_RESET"));
        for (String i : server.ideas().snapshotLines()) {
            send(i);
        }
        for (String d : server.decisions().snapshotLines()) {
            send(d);
        }

        server.log(nick + " joined (" + socket.getRemoteSocketAddress() + ").");
        server.broadcastSystem(nick + " joined the team.");
        server.broadcastUserList();
        return true;
    }

    /** @return false when the connection should be closed */
    private boolean handle(String[] p) {
        switch (p[0]) {
            case "MSG": {
                String text = Protocol.arg(p, 1).trim();
                if (text.isEmpty()) {
                    return true;
                }
                if (text.length() > Protocol.MAX_MESSAGE) {
                    text = text.substring(0, Protocol.MAX_MESSAGE);
                }
                server.broadcast(Protocol.build("MSG", server.time(), nick, text));
                return true;
            }
            case "PM": {
                String target = Protocol.arg(p, 1).trim();
                String text = Protocol.arg(p, 2).trim();
                if (target.isEmpty() || text.isEmpty()) {
                    send(Protocol.build("ERR", "Usage: /msg <nickname> <text>"));
                    return true;
                }
                ClientHandler other = server.find(target);
                if (other == null) {
                    send(Protocol.build("ERR", "'" + target + "' is not online."));
                    return true;
                }
                String line = Protocol.build("PM", server.time(), nick, other.getNick(), text);
                other.send(line);
                if (other != this) {
                    send(line);
                }
                return true;
            }
            case "LIST": {
                List<String> names = server.onlineNames();
                send(Protocol.build("SYS", server.time(),
                        "Online (" + names.size() + "): " + String.join(", ", names)));
                return true;
            }
            case "QUIT":
                return false;

            // ---- feature 1: idea cards with categories ----
            case "I_ADD":
                return finish(server.ideas().add(nick, Protocol.arg(p, 1), Protocol.arg(p, 2)));
            case "I_DEL": {
                Integer id = parseInt(Protocol.arg(p, 1));
                if (id == null) {
                    send(Protocol.build("ERR", "Idea number must be a number."));
                    return true;
                }
                return finish(server.ideas().remove(id, nick));
            }

            // ---- feature 2: group decisions with recorded votes ----
            case "D_ADD": {
                Integer ideaId = parseInt(Protocol.arg(p, 2));
                if (ideaId == null) {
                    send(Protocol.build("ERR", "Idea number must be a number."));
                    return true;
                }
                if (ideaId != 0 && !server.ideas().exists(ideaId)) {
                    send(Protocol.build("ERR", "Idea #" + ideaId + " does not exist."));
                    return true;
                }
                List<String> options = new ArrayList<>();
                for (int i = 3; i < p.length; i++) {
                    options.add(p[i]);
                }
                return finish(server.decisions().open(nick, Protocol.arg(p, 1), ideaId, options));
            }
            case "D_VOTE": {
                Integer id = parseInt(Protocol.arg(p, 1));
                Integer index = parseInt(Protocol.arg(p, 2));
                if (id == null || index == null) {
                    send(Protocol.build("ERR", "Usage: /vote <decision number> <option number>"));
                    return true;
                }
                return finish(server.decisions().vote(id, nick, index));
            }
            case "D_CLOSE": {
                Integer id = parseInt(Protocol.arg(p, 1));
                if (id == null) {
                    send(Protocol.build("ERR", "Decision number must be a number."));
                    return true;
                }
                boolean ownerOnline = server.isOnline(server.decisions().ownerOf(id));
                return finish(server.decisions().close(id, nick, ownerOnline));
            }

            default:
                send(Protocol.build("ERR", "Unknown command."));
                return true;
        }
    }

    /** Broadcasts a successful board result, or reports the error to the sender only. */
    private boolean finish(Result r) {
        if (!r.isOk()) {
            send(Protocol.build("ERR", r.error));
            return true;
        }
        server.broadcast(r.line);
        if (r.info != null) {
            server.broadcastSystem(r.info);
        }
        return true;
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Thread-safe: many threads may send to the same client at once. */
    void send(String line) {
        PrintWriter w = out;
        if (w == null) {
            return;
        }
        synchronized (this) {
            w.println(line);
            if (w.checkError()) {
                close();
            }
        }
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // already closed
        }
    }

    private void cleanup() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        close();
        if (registered && server.unregister(nick, this)) {
            server.log(nick + " left.");
            if (server.isRunningNow()) {
                server.broadcastSystem(nick + " left the team.");
                server.broadcastUserList();
            }
        }
    }
}
