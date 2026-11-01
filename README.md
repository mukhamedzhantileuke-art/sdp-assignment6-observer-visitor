# Assignment 6: Observer and Visitor Patterns
**Domain: Energy Grid Smart Management (Variant 12)**  
**Course:** Software Design Patterns (AITU, SE-2531)  
**Student:** Mukhamedzhan Tileuke  
**Technology:** Java 17, JUnit 5, Clean Code (Chapters 11–13)

---

## 1. Architectural Overview

This system coordinates real-time telemetry, state aggregation, and operational auditing across a multi-tier electrical energy grid. The architecture integrates two core Gang of Four behavioral patterns connected by an integration pipeline:

1. **Observer Pattern (Part A)**: Asynchronous, event-driven telemetry stream. Smart meter and SCADA sensor subjects broadcast immutable events to specialized subscribers across dynamic topics without locking the entire registry.
2. **Visitor Pattern (Part B)**: Double-dispatch hierarchical operations executed across an unmodifiable Composite grid tree (Regional Grid $\rightarrow$ Substations $\rightarrow$ Feeder Lines $\rightarrow$ Distributed Consumers, Generators, and Storage Units).
3. **Integration Link**: High-priority alert events trigger targeted visitor evaluations on the affected subtree only, converting raw alarms into actionable maintenance tickets.

---

## 2. Observer Pattern Design Decisions (Clean Code ch. 11)

### 2.1 Push vs. Pull Model
* **Decision**: We adopted the **Push Model**. The subject passes an immutable `GridEvent` instance directly into `GridObserver.onEvent(event)`.
* **Justification**:
  1. *Temporal Validity*: In an electrical grid, electrical measurements (active power kW, voltage sags, power factors) are instantaneous point-in-time state. If an observer had to pull state back from the subject, the metric might already have shifted by the time the worker thread reads it.
  2. *Decoupling & Thread Contention*: A pull model forces observers to retain references to subjects and call getters concurrently, introducing read-locks, race conditions, or stale caches. Push delivers a self-contained, immutable value object that can be processed without subject callbacks.

### 2.2 Thread-Safe Registry & Dynamic Join/Leave
* **Implementation**: `DefaultGridSubject` stores subscribers in:
  `ConcurrentMap<GridEventType, CopyOnWriteArrayList<GridObserver<GridEvent>>> registry`
* **Handling Unsubscription from Inside Handlers**:
  `CopyOnWriteArrayList` creates an immutable array snapshot for active iteration during event notification. When an observer invokes `subject.unsubscribe(type, this)` within its own `onEvent()` method, it modifies the underlying backing array for future events without invalidating the active iteration snapshot. This guarantees **zero `ConcurrentModificationException`** and requires no coarse `synchronized(subject)` locks.

### 2.3 Observer Chain (Aggregator Node)
* `RegionalLoadAggregator` implements both `GridObserver<GridEvent>` and `GridSubject<GridEvent>`.
* It consumes high-frequency `METER_READING` events, aggregates kilowatt totals per feeder line in a `ConcurrentHashMap` with atomic adders, and republishes batched `AGGREGATED_LOAD_REPORT` events to downstream listeners (`GridLoadBalancerListener` and `ConsumptionStatisticsTrackerListener`).

---

## 3. Visitor Pattern Design Decisions (Clean Code ch. 12)

### 3.1 Composite Tree Hierarchy
The grid model forms a 4-level composite tree (exceeding the 3-level requirement):
* **Level 1 (Root Container)**: `PowerGrid` (Regional grid).
* **Level 2 (Container 1)**: `Substation` (110 kV $\rightarrow$ 10 kV step-down stations; holds `DistributionLine`s).
* **Level 3 (Container 2)**: `DistributionLine` (Medium-voltage feeder lines; holds leaf assets).
* **Level 4 (Leaves - 5 Types)**:
  1. `ResidentialConsumer`: Household load with smart meter telemetry.
  2. `IndustrialConsumer`: Manufacturing plants with contracted capacity quotas.
  3. `SolarGenerator`: Photovoltaic parks with active inverter generation.
  4. `BatteryStorage`: Grid-scale utility storage (Charging, Discharging, or Idle).
  5. `EvChargingStation`: Commercial fast-charging clusters (Trade-off demonstration element).

**Clean Code Rule**: Element classes hold data only (pure POJOs with getters and `accept`). No calculation or traversal algorithms reside in element classes.

### 3.2 Traversal Decision: Visitor-Driven Traversal
* **Decision**: We selected **Visitor-Driven Traversal** over Tree-driven traversal.
* **Justification**:
  1. *Algorithmic Freedom*: Visitors require radically different traversal policies. For instance, `GridJsonExportVisitor` needs to emit opening brackets before children and closing brackets after children, inserting commas only between siblings. `OverloadedComponentFilterVisitor` can prune healthy branches to accelerate scans.
  2. *SRP & Clean Code*: The composite tree elements remain simple dumb data holders (Clean Code ch. 6). They do not dictate how or when children are traversed.

### 3.3 Implemented Visitors
1. `NetLoadCalculationVisitor` (Numeric calculation): Computes net active grid load in kW:
   $$\text{Net Load} = \sum P_{\text{consumers}} - \sum P_{\text{solar}} \pm P_{\text{battery}} + P_{\text{loss}}$$
2. `GridHealthAuditVisitor` (State accumulator across whole tree): Traverses all nodes to build an immutable `GridHealthReport` (asset counts, generation capacity, storage levels, anomaly counts).
3. `OverloadedComponentFilterVisitor` (Filter returning `List<GridElement>`): Collects assets operating outside safe parameters (lines exceeding ampacity, batteries with charge $< 15\%$, offline solar farms).
4. `GridJsonExportVisitor` (Hand-written JSON export): Recursively outputs formatted, nested JSON without third-party libraries.
5. `CarbonEmissionAuditVisitor` (Trade-off proof visitor): Calculates carbon footprint in kg $\text{CO}_2$/hr.

---

## 4. Integration Link: Alert $\rightarrow$ Inspection $\rightarrow$ Ticket

`GridInspectorIntegrationService` connects the Observer and Visitor sides:
1. Subscribes to `LOAD_SURGE`, `VOLTAGE_SAG`, and `OUTAGE_ALERT` events.
2. Reads `event.getTargetSubtreeId()` and resolves the specific `Substation` or `DistributionLine` subtree.
3. Dispatches `NetLoadCalculationVisitor` and `OverloadedComponentFilterVisitor` **exclusively on that target subtree**.
4. Compiles an immutable `MaintenanceTicket` with diagnostic metrics and flagged components.
5. Publishes a `MAINTENANCE_TICKET_CREATED` event back into the subject for field dispatchers.

---

## 5. Concurrency & Thread-Safety (Clean Code ch. 13)

* **ThreadPool Execution**: `GridSimulationEngine` wraps an `ExecutorService` with configurable thread count.
* **Producers**:
  - `SmartMeterReadingProducer`: streams periodic household/commercial smart meter readings.
  - `SubstationTelemetryProducer`: simulates SCADA transformer surges, voltage sags, and breaker trips.
  - `RenewableAnomalyProducer`: injects solar intermittency and cloud-transient load fluctuations.
* **Stress Test Verification**: `ConcurrencyStressTest` executes 4 concurrent threads emitting 1,000 events each ($4 \times 1,000 = 4,000$ events). An `AtomicInteger` collector validates exact count delivery with zero drops or race conditions.
* **Graceful Shutdown**: The engine invokes `shutdown()`, awaits orderly termination within a timeout, and calls `shutdownNow()` only on timeout/interruption.

---

## 6. Trade-Off Proof: The Expression Problem

The Visitor pattern optimizes for adding new operations, while making the addition of new element types expensive.

| Evolution Scenario | Files Modified | Cost Analysis |
| :--- | :--- | :--- |
| **Commit A: Add New Operation** (`CarbonEmissionAuditVisitor`) | **1 file**: `CarbonEmissionAuditVisitor.java` (Zero element classes modified) | **Very Low Cost**: Completely Open for Extension. No changes to existing code, no risk of regression across grid elements. |
| **Commit B: Add New Element** (`EvChargingStation`) | **7 files**: `GridVisitor.java`, `PowerGrid.java`, `NetLoadCalculationVisitor.java`, `GridHealthAuditVisitor.java`, `OverloadedComponentFilterVisitor.java`, `GridJsonExportVisitor.java`, `CarbonEmissionAuditVisitor.java` | **High Cost**: Every single visitor must be edited and recompiled to implement `visit(EvChargingStation)`. This violates OCP when the element hierarchy changes frequently. |

---

## 7. Oral Defense Cheat Sheet

### Q1: Why can't the client simply call `visitor.visit(node)`? Walk through double dispatch.
* **Answer**: Java uses **single dispatch** at runtime: method overloading is resolved at **compile-time** based on the static declared type of the parameter, not the runtime type. If client code executes:
  ```java
  GridElement elem = new ResidentialConsumer(...);
  visitor.visit(elem); // Compiler error: visit(GridElement) does not exist!
  ```
  The compiler cannot know which overloaded `visit` method to bind.
* **Double Dispatch Solution**:
  1. *First Dispatch*: Client calls `element.accept(visitor)`. Polymorphism routes dynamically to `ResidentialConsumer.accept(...)`.
  2. *Second Dispatch*: Inside that concrete class, `this` has the compile-time type `ResidentialConsumer`. It invokes `visitor.visit(this)`, resolving directly to `visit(ResidentialConsumer)`.

### Q2: Show your new-element commit: which files changed, and when would this cost make you reject Visitor?
* **Answer**: Adding `EvChargingStation` required changing `GridVisitor.java` and **all 5 concrete visitors** (7 files total).
* **When to reject Visitor**: When the element hierarchy is volatile and undergoes frequent additions (e.g., IoT devices with new sensor hardware added every sprint), but the operations on them are static. In that case, standard polymorphic methods on classes or Composite + Strategy is preferable.

### Q3: What happens if a listener unsubscribes while several threads are publishing events? Show how your registry handles it.
* **Answer**: `DefaultGridSubject` stores listeners in a `CopyOnWriteArrayList` per event type inside a `ConcurrentHashMap`. When an unsubscription occurs:
  1. A thread calling `notifyObservers()` receives an array reference representing an immutable snapshot of listeners at that microsecond and iterates over it without locking.
  2. The unregistering thread creates a new internal copy of the array without the removed listener and updates the volatile reference.
  3. No locks are held across event processing, and no `ConcurrentModificationException` can be thrown.
