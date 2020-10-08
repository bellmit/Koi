package co.casterlabs.koi;

import lombok.Getter;

@Getter
public class RepeatingThread {
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

}
