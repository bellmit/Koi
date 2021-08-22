package co.casterlabs.koi.util;

import java.io.IOException;

import co.casterlabs.koi.client.connection.Connection;
import lombok.Getter;

@Getter
public class RepeatingThread implements Connection {
    private String name;
    private long frequency;
    private Runnable run;
    private boolean running = false;
    private Thread thread;

    public RepeatingThread(String name, long frequency, Runnable run) {
        this.name = name;
        this.frequency = frequency;
        this.run = run;
    }

    public void stop() {
        this.running = false;
    }

    public void start() {
        if (!this.running) {
            this.running = true;
            this.thread = new InternalThread();
            this.thread.setName(this.name);
            this.thread.start();
        }
    }

    private class InternalThread extends Thread {
        @Override
        public void run() {
            while (running) {
                try {
                    run.run();

                    Thread.sleep(frequency);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.stop();
    }

    @Override
    public void open() throws IOException {
        this.start();
    }

    @Override
    public boolean isOpen() {
        return this.running;
    }

}
