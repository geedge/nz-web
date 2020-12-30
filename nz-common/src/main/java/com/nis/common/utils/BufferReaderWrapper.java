package com.nis.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class BufferReaderWrapper extends BufferedReader {
    public Reader in;
    public static final int MAX_STR_LEN=1024;
    public BufferReaderWrapper(Reader in) {
        super(in);
        this.in=in;
    }
    public String load() throws IOException {
        StringBuffer sb = new StringBuffer();
        int intC;
        while ((intC = in.read()) != -1) {
            char c = (char) intC;
            if (c == '\n') {
                break;
            }
            if (sb.length() >= MAX_STR_LEN) {
                throw new IOException("input too long");
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
