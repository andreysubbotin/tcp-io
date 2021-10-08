package org.example.tcp.blocking;

import com.google.common.base.Splitter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

public class CommandExecutor {
    private static final String FILE_PATH = "/Users/subbotin/Developer/work/files";

    private static final ExecutorService DOWNLOAD_EXECUTOR =
            new ThreadPoolExecutor(10, 200, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(50), new ThreadFactory() {
                private final AtomicLong id = new AtomicLong();

                @Override
                public Thread newThread(Runnable r) {
                    final var thread = new Thread(r);
                    thread.setName("download-worker-" + id.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    /**
     * Use standard common fork-join pool for CPU calculations
     *
     * @param command - CPU command to execute
     * @return command result as String
     */
    public static String executeSumCommand(Command command) {
        //Use standard common fork-join pool
        long max = Long.parseLong(command.getArgs().trim());

        return String.valueOf(LongStream.range(0, max)
                .parallel()
                .sum());
    }

    /**
     * Use standard common fork-join pool for CPU calculations
     *
     * @param command - CPU command to execute
     * @return command result as String
     */
    public static String executeZipCommand(Command command) {
        //TODO:
        return "NO RESULT";
    }

    /**
     * Use separate IO pool for downloading resources
     *
     * @param command - download command to execute
     * @return command result as String
     */
    public static String executeDownloadCommand(Command command) {
        List<URI> urls = new ArrayList<>();
        for (String value : Splitter.on(",").trimResults().splitToList(command.getArgs())) {
            try {
                urls.add(new URI(value));
            } catch (URISyntaxException e) {
                return "Incorrect URL format";
            }
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .executor(DOWNLOAD_EXECUTOR)
                .connectTimeout(Duration.ofSeconds(100))
                .build();


        List<DownloadResult> downloadResults = new ArrayList<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (URI uri : urls) {
            UUID resultId = UUID.randomUUID();
            String fileName = String.format("%s/download_%s.dat", FILE_PATH, resultId);
            CompletableFuture<?> future = httpClient.sendAsync(HttpRequest.newBuilder(uri).build(),
                    HttpResponse.BodyHandlers.ofFile(new File(fileName).toPath()));

            futures.add(future);
            downloadResults.add(new DownloadResult(uri.toString(), fileName, future));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        StringBuilder stringBuilder = new StringBuilder();
        for (DownloadResult result : downloadResults) {
            CompletableFuture<?> future = result.completableFuture();
            if (future.isCompletedExceptionally()) {
                stringBuilder.append("URL:").append(result.url).append(" ")
                        .append("ERROR: Unable to download file").append("\n");
            } else {
                stringBuilder.append("URL:").append(result.url).append(" ")
                        .append("FILE:").append(result.fileName).append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private static record DownloadResult(String url, String fileName, CompletableFuture<?> completableFuture) {
    }
}
