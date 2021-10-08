package org.example.tcp.blocking;

import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Log
public class Client {
    private static final String COMMAND_BEGIN = "{";
    private static final String COMMAND_END = "}";

    private static final String IMAGE_URL_1 = "https://cdn.pixabay.com/photo/2016/08/23/12/37/files-1614223__340.jpg";
    private static final String IMAGE_URL_2 = "https://cdn.pixabay.com/photo/2016/11/21/13/20/port-1845350__340.jpg";
    private static final String IMAGE_URL_3 = "https://cdn.pixabay.com/photo/2015/07/08/03/13/forklift-835340__340.jpg";

    public static void main(String[] args) {
        try {
            final var socket = new Socket("127.0.0.1", 9999);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();

            writeCommand("{command: sum, argument: 20}", out);
            String result = readResult(in);
            System.out.println("Sum:\n" + result + "\n");

            writeCommand(String.format("{command: download, argument: %s, %s, %s}",
                    IMAGE_URL_1, IMAGE_URL_2, IMAGE_URL_3), out);
            result = readResult(in);
            System.out.println("Download:\n" + result + "\n");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeCommand(String command, OutputStream outputStream) throws IOException {
        outputStream.write(command.getBytes(StandardCharsets.UTF_8));
    }

    private static String readResult(InputStream in) throws IOException {
        final byte[] buffer = new byte[100];
        StringBuilder dataBuffer = new StringBuilder();
        StringBuilder result = new StringBuilder();
        var read = 0;
        while ((read = in.read(buffer)) != -1) {
            final String data = new String(buffer, 0, read, StandardCharsets.UTF_8);
            dataBuffer.append(data);

            int separatorEndPosition = dataBuffer.lastIndexOf(COMMAND_END);
            int separatorBeginPosition = dataBuffer.indexOf(COMMAND_BEGIN);
            if (separatorBeginPosition != -1 && separatorEndPosition != -1) {
                result = new StringBuilder(dataBuffer.subSequence(separatorBeginPosition + 1, separatorEndPosition));
                break;
            }
        }
        return result.toString();
    }
}

