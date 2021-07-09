package io;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FilePushbackReader implements Closeable {
    private final static int LOOKAHEAD_SIZE = 1000;
    private final File file;
    private final PushbackReader pr;
    private int currentLength = 0;

    public FilePushbackReader(String filename) throws FileNotFoundException {
        file = new File(filename);
        if (!file.exists() || file.isDirectory())
            throw new FileNotFoundException();
        pr = new PushbackReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), LOOKAHEAD_SIZE);
    }

    public int getCurrentLength() {
        return currentLength;
    }

    public char readChar() throws IOException {
        ++currentLength;
        return (char) pr.read();
    }

    public char peekChar() throws IOException {
        int nextCodepoint = pr.read();
        pr.unread(nextCodepoint);
        return (char) nextCodepoint;
    }

    /**
     * This function will not move forward the current stream, and will return the next N characters in order.
     */
    public String peekChars(int num) throws IOException {
        char[] charr = new char[num];
        for (int i = 0; i < num; i++) {
            charr[i] = (char) pr.read();
        }

        pr.unread(charr);

        return new String(charr);
    }

    /**
     * Discards the next character in line.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void eat() throws IOException {
        ++currentLength;
        pr.read();
    }

    /**
     * Discards the next N characters in line.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void eat(int num) throws IOException {
        for (int i = 0; i < num; i++) {
            ++currentLength;
            pr.read();
        }
    }

    public String getFilename() {
        return file.getName();
    }

    @Override
    public void close() throws IOException {
        pr.close();
    }
}
