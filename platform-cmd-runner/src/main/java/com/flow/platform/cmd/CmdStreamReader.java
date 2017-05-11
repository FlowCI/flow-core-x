package com.flow.platform.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Async InputStream reader
 *
 * Created by gy@fir.im on 10/05/2017.
 *
 * @copyright fir.im
 */
public class CmdStreamReader implements Runnable {

    /**
     * Callback on stream line and finish
     */
    public interface CmdStreamListener {

        void onLogging(String line);

        void onFinish();
    }

    private InputStream stream;
    private int bufferSize;
    private CmdStreamListener listener;

    public CmdStreamReader(InputStream stream, int bufferSize, CmdStreamListener listener) {
        this.stream = stream;
        this.bufferSize = bufferSize;
        this.listener = listener;
    }

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream), bufferSize)) {
            String line;
            while ((line = reader.readLine()) != null) {
                listener.onLogging(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            listener.onFinish();
        }
    }
}
