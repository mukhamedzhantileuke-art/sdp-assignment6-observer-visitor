package kz.aitu.sdp.assignment6.concurrency;

import kz.aitu.sdp.assignment6.observer.sources.SmartMeterReadingSource;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Producer Thread 1: Streams smart meter readings into the grid telemetry subject.
 */
public class SmartMeterReadingProducer implements Runnable {

    private final SmartMeterReadingSource meterSource;
    private final int eventCount;
    private final String lineId;
    private final CountDownLatch completionLatch;

    public SmartMeterReadingProducer(SmartMeterReadingSource meterSource,
                                    int eventCount,
                                    String lineId,
                                    CountDownLatch completionLatch) {
        this.meterSource = Objects.requireNonNull(meterSource, "meterSource cannot be null");
        this.eventCount = eventCount;
        this.lineId = Objects.requireNonNull(lineId, "lineId cannot be null");
        this.completionLatch = completionLatch;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= eventCount; i++) {
                double demandKw = 3.0 + (i % 7) * 0.8;
                meterSource.emitMeterReading("MTR-" + (1000 + i % 50), lineId, demandKw, "Periodic interval telemetry #" + i);
            }
        } finally {
            if (completionLatch != null) {
                completionLatch.countDown();
            }
        }
    }
}
