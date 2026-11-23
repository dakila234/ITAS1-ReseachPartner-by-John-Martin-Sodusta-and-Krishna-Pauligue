package researchpartners;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Research Partners server: accepts connections, keeps the shared state
 * (online users, idea cards and decisions) and broadcasts changes to every client.
 * It has no GUI of its own; ServerFrame listens to it through {@link Listener}.
 */
public class ChatServer {

    public interface Listener {
        void onLog(String message);
        void onUserCountChanged(int count);
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Online users keyed by lower-case nickname (nicknames are unique, case-insensitive). */
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final IdeaBoard ideas = new IdeaBoard();
    private final DecisionBoard decisions = new DecisionBoard();
    private final Listener listener;

    private ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(Listener listener) {
        this.listener = listener;
    }

    public synchronized void start(int port) throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        ideas.reset();
        decisions.reset();
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "accept-loop");
        acceptThread.setDaemon(true);
        acceptThread.start();
        log("Server started on port " + port + ".");
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // closing anyway
        }
        for (ClientHandler h : new ArrayList<>(clients.values())) {
            h.close();
        }
        clients.clear();
        listener.onUserCountChanged(0);
        log("Server stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(new ClientHandler(this, socket), "client-handler");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) {
                    log("Accept error: " + e.getMessage());
                }
            }
        }
    }

    // ---- helpers used by ClientHandler -------------------------------------------------

    IdeaBoard ideas() {
        return ideas;
    }

    DecisionBoard decisions() {
        return decisions;
    }

    boolean isRunningNow() {
        return running;
    }

    String time() {
        return LocalTime.now().format(TIME);
    }

    void log(String message) {
        listener.onLog("[" + time() + "] " + message);
    }

    /** Tries to claim a nickname. Returns false when someone already uses it. */
    boolean register(String nick, ClientHandler handler) {
        boolean ok = clients.putIfAbsent(nick.toLowerCase(Locale.ROOT), handler) == null;
        if (ok) {
            listener.onUserCountChanged(clients.size());
        }
        return ok;
    }

    /** Removes a client; returns true if it was really registered. */
    boolean unregister(String nick, ClientHandler handler) {
        boolean removed = clients.remove(nick.toLowerCase(Locale.ROOT), handler);
        if (removed) {
            listener.onUserCountChanged(clients.size());
        }
        return removed;
    }

    ClientHandler find(String nick) {
        return clients.get(nick.toLowerCase(Locale.ROOT));
    }

    boolean isOnline(String nick) {
        return find(nick) != null;
    }

    /** Sends one line to every connected client (each client receives it exactly once). */
    void broadcast(String line) {
        for (ClientHandler h : clients.values()) {
            h.send(line);
        }
    }

    void broadcastSystem(String text) {
        broadcast(Protocol.build("SYS", time(), text));
    }

    List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler h : clients.values()) {
            names.add(h.getNick());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    void broadcastUserList() {
        broadcast(Protocol.build("USERS", onlineNames().toArray(new String[0])));
    }
}
