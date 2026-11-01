package kz.aitu.sdp.assignment6.observer;

import kz.aitu.sdp.assignment6.observer.listeners.*;
import kz.aitu.sdp.assignment6.observer.sources.SmartMeterReadingSource;
import kz.aitu.sdp.assignment6.observer.sources.SubstationTelemetrySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Observer Pattern Unit & Integration Tests")
class ObserverPatternTest {

    private SmartMeterReadingSource meterSource;
    private SubstationTelemetrySource scadaSource;

    @BeforeEach
    void setUp() {
        meterSource = new SmartMeterReadingSource();
        scadaSource = new SubstationTelemetrySource();
    }

    @Test
    @DisplayName("Test 1: Observers receive events of subscribed type with correct payload")
    void testBasicNotificationAndSubscription() {
        BillingServiceListener billing = new BillingServiceListener("BILLING-01", 0.15);
        ConsumptionStatisticsTrackerListener stats = new ConsumptionStatisticsTrackerListener("STATS-01");

        meterSource.subscribe(GridEventType.METER_READING, billing);
        meterSource.subscribe(GridEventType.METER_READING, stats);

        meterSource.emitMeterReading("MTR-101", "LINE-1", 10.0, "Telemetry interval 1");
        meterSource.emitMeterReading("MTR-102", "LINE-1", 20.0, "Telemetry interval 2");

        // Billing: 10 * 0.15 = 1.50, 20 * 0.15 = 3.00, total = 4.50
        assertEquals(4.50, billing.getTotalBilledRevenue(), 0.001);
        assertEquals(2, billing.getProcessedReadingsCount());

        // Statistics
        assertEquals(2, stats.getReadingCount());
        assertEquals(30.0, stats.getTotalEnergyKwh(), 0.001);
        assertEquals(20.0, stats.getPeakPowerKw(), 0.001);
        assertEquals(10.0, stats.getMinPowerKw(), 0.001);
        assertEquals(15.0, stats.getAveragePowerKw(), 0.001);
    }

    @Test
    @DisplayName("Test 2: Observers only receive events of the types they explicitly subscribed to")
    void testEventFilteringByType() {
        OutageAlertServiceListener outageService = new OutageAlertServiceListener("OUTAGE-01");
        BillingServiceListener billing = new BillingServiceListener("BILLING-01", 0.10);

        scadaSource.subscribe(GridEventType.OUTAGE_ALERT, outageService);
        scadaSource.subscribe(GridEventType.VOLTAGE_SAG, outageService);
        meterSource.subscribe(GridEventType.METER_READING, billing);

        // Emit an outage alert on SCADA source
        scadaSource.emitOutageAlert("SEN-99", "LINE-1", "Transformer trip");

        assertEquals(1, outageService.getOutageAlertCount());
        assertEquals(0, outageService.getVoltageSagCount());
        assertEquals(0, billing.getProcessedReadingsCount(), "Billing listener should NOT receive SCADA outage events");
    }

    @Test
    @DisplayName("Test 3: Observers can unsubscribe dynamically at runtime")
    void testDynamicUnsubscribeAtRuntime() {
        BillingServiceListener billing = new BillingServiceListener("BILLING-01", 0.10);
        meterSource.subscribe(GridEventType.METER_READING, billing);

        meterSource.emitMeterReading("MTR-1", "LINE-1", 50.0, "Read 1");
        assertEquals(1, billing.getProcessedReadingsCount());

        meterSource.unsubscribe(GridEventType.METER_READING, billing);

        meterSource.emitMeterReading("MTR-1", "LINE-1", 50.0, "Read 2");
        assertEquals(1, billing.getProcessedReadingsCount(), "Unsubscribed observer should not receive further events");
    }

    @Test
    @DisplayName("Test 4: Observers can safely unsubscribe from within their own onEvent handler")
    void testUnsubscribeFromInsideOwnHandler() {
        AtomicInteger invocationCount = new AtomicInteger(0);
        AtomicBoolean completedWithoutException = new AtomicBoolean(false);

        GridObserver<GridEvent> selfUnsubscribingObserver = new GridObserver<>() {
            @Override
            public void onEvent(GridEvent event) {
                invocationCount.incrementAndGet();
                // Dynamically unsubscribe inside callback
                meterSource.unsubscribe(GridEventType.METER_READING, this);
                completedWithoutException.set(true);
            }

            @Override
            public String getObserverId() {
                return "SELF-UNSUB";
            }
        };

        meterSource.subscribe(GridEventType.METER_READING, selfUnsubscribingObserver);

        // First emit triggers handler, which unsubscribes itself
        assertDoesNotThrow(() -> meterSource.emitMeterReading("MTR-A", "LINE-1", 12.0, "First"));
        assertTrue(completedWithoutException.get());
        assertEquals(1, invocationCount.get());

        // Second emit must not deliver to this observer
        meterSource.emitMeterReading("MTR-B", "LINE-1", 14.0, "Second");
        assertEquals(1, invocationCount.get(), "Observer must have unsubscribed itself successfully");
        assertEquals(0, meterSource.getObserverCount(GridEventType.METER_READING));
    }

    @Test
    @DisplayName("Test 5: Observer Chain - Aggregator acts as listener and subject, republishing batched data")
    void testObserverChainAggregator() {
        // Aggregator batches every 3 readings
        RegionalLoadAggregator aggregator = new RegionalLoadAggregator("REG-AGG-1", 3);
        GridLoadBalancerListener loadBalancer = new GridLoadBalancerListener("BALANCER-01", 100.0);

        meterSource.subscribe(GridEventType.METER_READING, aggregator);
        aggregator.subscribe(GridEventType.AGGREGATED_LOAD_REPORT, loadBalancer);

        // Emit 2 readings (under threshold 3)
        meterSource.emitMeterReading("M1", "LINE-X", 40.0, "Read 1");
        meterSource.emitMeterReading("M2", "LINE-X", 50.0, "Read 2");
        assertEquals(0, loadBalancer.getBalancingActionCount(), "Batch threshold not reached yet");

        // Emit 3rd reading -> triggers batch publication of sum (40 + 50 + 60 = 150 kW > threshold 100 kW)
        meterSource.emitMeterReading("M3", "LINE-X", 60.0, "Read 3");

        assertEquals(1, loadBalancer.getBalancingActionCount(), "Load balancer should react to aggregated load report");
        assertEquals(50.0, loadBalancer.getTotalCurtailedLoadKw(), 0.001, "Excess load above 100 kW is 150 - 100 = 50 kW");
    }
}
