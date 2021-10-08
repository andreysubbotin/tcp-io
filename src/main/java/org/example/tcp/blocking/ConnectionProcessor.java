package org.example.tcp.blocking;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionProcessor {
    private static final String COMMAND_BEGIN = "{";
    private static final String COMMAND_END = "}";
    private static final Pattern DETAILED_COMMAND_PATTERN = Pattern.compile("\s*command:\s*(\\w+)\s*,\s*argument:\s*(.+)\s*");

    public static void process(Socket socket) {
        try (
                socket;
                final var in = socket.getInputStream();
                final var out = socket.getOutputStream();
        ) {
            final byte[] buffer = new byte[100];
            StringBuilder dataBuffer = new StringBuilder();
            var read = 0;
            while ((read = in.read(buffer)) != -1) {
                final String data = new String(buffer, 0, read, StandardCharsets.UTF_8);
                dataBuffer.append(data);

                int separatorEndPosition = dataBuffer.lastIndexOf(COMMAND_END);
                int separatorBeginPosition = dataBuffer.indexOf(COMMAND_BEGIN);
                if (separatorBeginPosition != -1 && separatorEndPosition != -1) {
                    processRawCommand(new StringBuilder(dataBuffer.subSequence(separatorBeginPosition + 1, separatorEndPosition)), out);

                    out.flush();

                    dataBuffer = new StringBuilder();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processRawCommand(StringBuilder rawCommand, OutputStream outputStream) throws IOException {
        Command command = parseCommand(rawCommand.toString());
        if (command == null) {
            outputStream.write("{NO RESULT}".getBytes(StandardCharsets.UTF_8));
        } else {
            executeCommand(command, outputStream);
        }
    }

    private static Command parseCommand(String command) {
        Matcher matcher = DETAILED_COMMAND_PATTERN.matcher(command);
        if (matcher.find()) {
            return new Command(command, matcher.group(1), matcher.group(2));
        }
        return null;
    }

    private static void executeCommand(Command command, OutputStream outputStream) throws IOException {
        if ("sum".equals(command.getCommandName())) {
            String result = CommandExecutor.executeSumCommand(command);
            outputStream.write(convertResult(result));
        } else if ("zip".equals(command.getCommandName())) {
            String result = CommandExecutor.executeZipCommand(command);
            outputStream.write(convertResult(result));
        } else if ("download".equals(command.getCommandName())) {
            String result = CommandExecutor.executeDownloadCommand(command);
            outputStream.write(convertResult(result));
        }
    }

    private static byte[] convertResult(String result) {
        return String.format("{%s}", result).getBytes(StandardCharsets.UTF_8);
    }
}
