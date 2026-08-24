// Demo data for RxJava Disposable Leak Companion -- used with
// `./gradlew runIde` to capture the real Marketplace screenshot. Open
// this file, the warning should appear on the subscribe() line inside
// placeOrderUnsafely.

class OrderService {

    void placeOrderUnsafely(Order order) {
        // The Disposable returned here is discarded -- FLAGGED. This
        // subscription can never be cancelled.
        orders.subscribe(o -> repository.save(o), error -> log.error("failed", error));
    }

    void placeOrderSafely(Order order) {
        // Captured in a variable so it can be disposed later -- NOT
        // flagged.
        Disposable subscription = orders.subscribe(
            o -> repository.save(o),
            error -> log.error("failed", error)
        );
        activeSubscriptions.add(subscription);
    }
}
