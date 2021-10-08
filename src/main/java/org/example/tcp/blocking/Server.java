package org.example.tcp.blocking;

import lombok.extern.java.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

@Log
public class Server {
    private static volatile boolean running = true;
    private static ServerSocket serverSocket;

    private static final ExecutorService CONNECTIONS_EXECUTOR =
            new ThreadPoolExecutor(10, 200, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(50), new ThreadFactory() {
                private final AtomicLong id = new AtomicLong();

                @Override
                public Thread newThread(Runnable r) {
                    final var thread = new Thread(r);
                    thread.setName("worker-" + id.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Server::shutdownInternal));
        try {
            serverSocket = new ServerSocket(9999);
            serverSocket.setSoTimeout(1000);
            log.log(Level.INFO, "server started at port: " + serverSocket.getLocalPort());
            while (running) {
                // Acceptor
                try {
                    final var socket = serverSocket.accept(); // blocking
                    CONNECTIONS_EXECUTOR.execute(() -> ConnectionProcessor.process(socket));
                } catch (SocketTimeoutException e) {
                    //Do nothing
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void shutdownInternal() {
        // no new tasks
        running = false;
        CONNECTIONS_EXECUTOR.shutdown();
        try {
            final boolean done = CONNECTIONS_EXECUTOR.awaitTermination(1000, TimeUnit.MILLISECONDS);
            if (!done) {
                CONNECTIONS_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            CONNECTIONS_EXECUTOR.shutdownNow();
        }
    }
}
