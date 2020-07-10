package co.casterlabs.koi;

import lombok.Getter;

@Getter
public class RepeatingThread {
    private long frequency;
    private Runnable run;
    private boolean running = false;
    private Thread thread;

    public RepeatingThread(long frequency, Runnable run) {
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

}
