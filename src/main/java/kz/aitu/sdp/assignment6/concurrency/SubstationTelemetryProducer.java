package kz.aitu.sdp.assignment6.concurrency;

import kz.aitu.sdp.assignment6.observer.sources.SubstationTelemetrySource;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Producer Thread 2: Simulates SCADA substation telemetry (surges, voltage sags, trips).
 */
public class SubstationTelemetryProducer implements Runnable {

    private final SubstationTelemetrySource telemetrySource;
    private final int eventCount;
    private final String substationId;
    private final CountDownLatch completionLatch;

    public SubstationTelemetryProducer(SubstationTelemetrySource telemetrySource,
                                       int eventCount,
                                       String substationId,
                                       CountDownLatch completionLatch) {
        this.telemetrySource = Objects.requireNonNull(telemetrySource, "telemetrySource cannot be null");
        this.eventCount = eventCount;
        this.substationId = Objects.requireNonNull(substationId, "substationId cannot be null");
        this.completionLatch = completionLatch;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= eventCount; i++) {
                if (i % 10 == 0) {
                    telemetrySource.emitLoadSurge("SCADA-SEN-01", substationId, 1250.0 + (i % 50), "Transformer peak surge event #" + i);
                } else if (i % 7 == 0) {
                    telemetrySource.emitVoltageSag("SCADA-SEN-02", substationId, 204.5, "Bus voltage sag event #" + i);
                } else if (i % 25 == 0) {
                    telemetrySource.emitOutageAlert("SCADA-BRK-01", substationId, "Feeder breaker trip #" + i);
                } else {
                    telemetrySource.emitLoadSurge("SCADA-SEN-01", substationId, 600.0, "Nominal load variation #" + i);
                }
            }
        } finally {
            if (completionLatch != null) {
                completionLatch.countDown();
            }
        }
    }
}
