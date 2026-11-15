# Optimistic-lock retry

`@RetryOnOptimisticLock` automatically retries when an optimistic lock conflict occurs, which is suitable for high-concurrency updates such as stock and balances.

## Usage

```java
@RetryOnOptimisticLock(maxAttempts = 3, delay = 100)
@Transactional
public void updateStock(Long id, int quantity) {
    Stock stock = repository.findById(id).orElseThrow();
    stock.setQuantity(stock.getQuantity() - quantity);
}
```

| Attribute | Default | Description |
| --- | --- | --- |
| `maxAttempts` | 3 | Maximum number of attempts, including the first call; minimum is 1 |
| `delay` | 50 | Milliseconds to wait between two attempts; values less than 0 are treated as 0 |

## Aspect behavior

- The aspect runs with `Ordered.HIGHEST_PRECEDENCE` and wraps the transaction aspect, so each retry starts an independent transaction and reloads the data.
- Only `OptimisticLockingFailureException` and `jakarta.persistence.OptimisticLockException` trigger a retry; nested causes in the exception chain are also recognized.
- Other exceptions are thrown outward immediately.
- After the maximum number of attempts is reached, the original exception from the last attempt is thrown.
- If the thread is interrupted during the retry wait, the interrupt flag is restored and the original exception is thrown.

## Configuration switch

```yaml
neopen:
  extension:
    retry:
      enabled: true
```

Enabled by default. When disabled, the aspect is no longer registered.

## Notes

- The retried method must be safe to execute repeatedly, and each retry must read the latest data.
- Retries occupy the current thread, so the wait time should not be too large; in high-conflict scenarios, prefer shortening transactions and splitting hot rows.
- If the method swallows the optimistic lock exception internally, the aspect cannot detect it and will not retry.
