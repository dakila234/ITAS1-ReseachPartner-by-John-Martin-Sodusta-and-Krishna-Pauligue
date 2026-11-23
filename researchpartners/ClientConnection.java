package researchpartners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client side of the socket. connect() does the HELLO handshake (call it from a
 * background thread); startReading() then launches a reader thread that hands every
 * incoming message to the Listener. The Listener is called on the reader thread,
 * so GUI code must switch to the Swing thread itself.
 */
class ClientConnection {

    interface Listener {
        void onMessage(String[] parts);
        void onClosed();
    }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /** Opens the socket and logs in. Throws IOException with a readable message on failure. */
    void connect(String host, int port, String nick) throws IOException {
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            out.println(Protocol.build("HELLO", nick));
            String reply = in.readLine();
            if (reply == null) {
                throw new IOException("The server closed the connection.");
            }
            String[] p = Protocol.parse(reply);
            if ("ERR".equals(p[0])) {
                throw new IOException(Protocol.arg(p, 1));
            }
            if (!"OK".equals(p[0])) {
                throw new IOException("Unexpected reply from server.");
            }
            socket.setSoTimeout(0);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    void startReading(Listener listener) {
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (!line.isEmpty()) {
                        listener.onMessage(Protocol.parse(line));
                    }
                }
            } catch (IOException e) {
                // connection lost
            } finally {
                boolean wasOpen = !closed.get();
                close();
                if (wasOpen) {
                    listener.onClosed();
                }
            }
        }, "client-reader");
        t.setDaemon(true);
        t.start();
    }

    synchronized void send(String line) {
        if (out != null && !closed.get()) {
            out.println(line);
        }
    }

    /** User-initiated disconnect. */
    void disconnect() {
        send(Protocol.build("QUIT"));
        close();
    }

    private void close() {
        closed.set(true);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // nothing more to do
        }
    }
}
