# 乐观锁重试

`@RetryOnOptimisticLock` 在乐观锁冲突时自动重试，适合高并发更新库存、余额等场景。

## 用法

```java
@RetryOnOptimisticLock(maxAttempts = 3, delay = 100)
@Transactional
public void updateStock(Long id, int quantity) {
    Stock stock = repository.findById(id).orElseThrow();
    stock.setQuantity(stock.getQuantity() - quantity);
}
```

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `maxAttempts` | 3 | 最大尝试次数，包含首次调用，最小为 1 |
| `delay` | 50 | 两次尝试之间的等待毫秒数，小于 0 按 0 处理 |

## 切面行为

- 切面以 `Ordered.HIGHEST_PRECEDENCE` 执行，包裹事务切面，因此每次重试都会开启独立事务并重新加载数据。
- 只有 `OptimisticLockingFailureException` 和 `jakarta.persistence.OptimisticLockException` 会触发重试，异常链中的嵌套原因同样会被识别。
- 其他异常立即向外抛出。
- 达到最大次数后抛出最后一次的原始异常。
- 重试等待期间线程被中断时，恢复中断标记并抛出原始异常。

## 配置开关

```yaml
neopen:
  extension:
    retry:
      enabled: true
```

默认启用。关闭后切面不再注册。

## 注意事项

- 被重试的方法需要保证可重复执行，且每次重试读取到最新数据。
- 重试会占用当前线程，等待时间不宜过大；高冲突场景应优先缩短事务、拆分热点行。
- 若方法内部吞掉了乐观锁异常，切面无法感知，也不会重试。
