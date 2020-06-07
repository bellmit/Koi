package co.casterlabs.koi.util;

import xyz.e3ndr.FastLoggingFramework.Logging.FastLogger;

public class DebugStat {
    private long start = System.currentTimeMillis();
    private long lastAmount = 0;
    private FastLogger logger;
    private long lastTime = 0;
    private long count = 0;
    private long tick = 0;

    public DebugStat(String name) {
        this.logger = new FastLogger(name);
    }

    public void tick() {
        long current = System.currentTimeMillis() - this.start;
        double sec = current / 1000.0;

        this.tick++;

        if ((current - this.lastTime) >= 1000) {
            this.count = this.tick - this.lastAmount;
            this.lastAmount = this.tick;
            this.lastTime = current;
        }

        if ((this.tick % 100) == 0) {
            this.logger.debug(String.format("Processed %,d @ %.2fs (%,d/s)", this.tick, sec, this.count));
        }
    }

}
