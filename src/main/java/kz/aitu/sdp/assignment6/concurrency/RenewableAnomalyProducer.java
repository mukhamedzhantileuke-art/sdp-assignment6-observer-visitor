package kz.aitu.sdp.assignment6.concurrency;

import kz.aitu.sdp.assignment6.observer.sources.SubstationTelemetrySource;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Producer Thread 3: Simulates renewable generation anomalies and grid fluctuations.
 */
public class RenewableAnomalyProducer implements Runnable {

    private final SubstationTelemetrySource telemetrySource;
    private final int eventCount;
    private final String targetSubtreeId;
    private final CountDownLatch completionLatch;

    public RenewableAnomalyProducer(SubstationTelemetrySource telemetrySource,
                                    int eventCount,
                                    String targetSubtreeId,
                                    CountDownLatch completionLatch) {
        this.telemetrySource = Objects.requireNonNull(telemetrySource, "telemetrySource cannot be null");
        this.eventCount = eventCount;
        this.targetSubtreeId = Objects.requireNonNull(targetSubtreeId, "targetSubtreeId cannot be null");
        this.completionLatch = completionLatch;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= eventCount; i++) {
                if (i % 2 == 0) {
                    telemetrySource.emitLoadSurge("PV-SENS-01", targetSubtreeId, 850.0 + (i % 30), "Solar cloud drop compensation #" + i);
                } else {
                    telemetrySource.emitVoltageSag("PV-INV-01", targetSubtreeId, 212.0, "Inverter harmonic ripple #" + i);
                }
            }
        } finally {
            if (completionLatch != null) {
                completionLatch.countDown();
            }
        }
    }
}
