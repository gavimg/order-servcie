package com.gavi.microservices.order_service.client;

import com.gavi.microservices.order_service.exception.InventoryServiceUnavailableException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceClient {

    private static final String INVENTORY_CIRCUIT_BREAKER = "inventoryServiceCircuitBreaker";
    private static final String INVENTORY_RETRY = "inventoryServiceRetry";
    private static final String INVENTORY_TIME_LIMITER = "inventoryServiceTimeLimiter";

    private final InventoryClient inventoryClient;
    private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    public boolean isInStock(String skuCode, Integer quantity) {
        Retry retry = retryRegistry.retry(INVENTORY_RETRY);
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(INVENTORY_TIME_LIMITER);
        Supplier<Boolean> inventoryCheck = Retry.decorateSupplier(
                retry,
                () -> executeWithTimeout(skuCode, quantity, timeLimiter)
        );

        return circuitBreakerFactory.create(INVENTORY_CIRCUIT_BREAKER)
                .run(
                        inventoryCheck::get,
                        throwable -> handleInventoryFailure(skuCode, throwable)
                );
    }

    private boolean executeWithTimeout(String skuCode, Integer quantity, TimeLimiter timeLimiter) {
        try {
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(() -> inventoryClient.isInStock(skuCode, quantity))
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean handleInventoryFailure(String skuCode, Throwable throwable) {
        log.error("Inventory service call failed for skuCode {}", skuCode, throwable);
        throw new InventoryServiceUnavailableException(
                "Inventory service is currently unavailable. Please try again later.",
                throwable
        );
    }
}
