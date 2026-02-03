package com.dms.liteflow.infrastructure.saga.redis;

import com.dms.liteflow.infrastructure.saga.config.SagaRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Saga 分布式锁工具
 * 基于 Redisson 实现的分布式锁
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaDistributedLock {

    private final RedissonClient redissonClient;

    /**
     * 尝试获取锁
     *
     * @param executionId 执行ID
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String executionId, long waitTime, long leaseTime, TimeUnit unit) {
        String lockKey = SagaRedisKeys.lockKey(executionId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("Acquired lock: key={}, leaseTime={} {}", lockKey, leaseTime, unit);
            } else {
                log.warn("Failed to acquire lock: key={}, waitTime={} {}", lockKey, waitTime, unit);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param executionId 执行ID
     */
    public void unlock(String executionId) {
        String lockKey = SagaRedisKeys.lockKey(executionId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released lock: key={}", lockKey);
            } else {
                log.warn("Attempted to unlock lock not held by current thread: key={}", lockKey);
            }
        } catch (Exception e) {
            log.error("Failed to release lock: key={}", lockKey, e);
        }
    }

    /**
     * 检查锁是否被持有
     *
     * @param executionId 执行ID
     * @return 是否被持有
     */
    public boolean isLocked(String executionId) {
        String lockKey = SagaRedisKeys.lockKey(executionId);
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 检查锁是否被当前线程持有
     *
     * @param executionId 执行ID
     * @return 是否被当前线程持有
     */
    public boolean isHeldByCurrentThread(String executionId) {
        String lockKey = SagaRedisKeys.lockKey(executionId);
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 强制释放锁（危险操作，仅用于异常恢复）
     *
     * @param executionId 执行ID
     */
    public void forceUnlock(String executionId) {
        String lockKey = SagaRedisKeys.lockKey(executionId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            lock.forceUnlock();
            log.warn("Force unlocked: key={}", lockKey);
        } catch (Exception e) {
            log.error("Failed to force unlock: key={}", lockKey, e);
        }
    }

    /**
     * 执行带锁的操作
     *
     * @param executionId 执行ID
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @param action 要执行的操作
     * @return 操作是否执行成功
     */
    public boolean executeWithLock(String executionId, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
        if (tryLock(executionId, waitTime, leaseTime, unit)) {
            try {
                action.run();
                return true;
            } finally {
                unlock(executionId);
            }
        }
        return false;
    }

    /**
     * 执行带锁的操作（有返回值）
     *
     * @param executionId 执行ID
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果，如果获取锁失败则返回 null
     */
    public <T> T executeWithLock(String executionId, long waitTime, long leaseTime, TimeUnit unit, java.util.function.Supplier<T> supplier) {
        if (tryLock(executionId, waitTime, leaseTime, unit)) {
            try {
                return supplier.get();
            } finally {
                unlock(executionId);
            }
        }
        return null;
    }

    /**
     * 延长锁持有时间
     *
     * @param executionId 执行ID
     * @param additionalTime 延长时间
     * @param unit 时间单位
     * @return 是否延长成功
     */
    public boolean renewLock(String executionId, long additionalTime, TimeUnit unit) {
        String lockKey = SagaRedisKeys.lockKey(executionId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean renewed = lock.expire(additionalTime, unit);
            if (renewed) {
                log.debug("Renewed lock: key={}, additionalTime={} {}", lockKey, additionalTime, unit);
            } else {
                log.warn("Failed to renew lock: key={}, possibly already expired", lockKey);
            }
            return renewed;
        } catch (Exception e) {
            log.error("Failed to renew lock: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 获取锁剩余持有时间
     *
     * @param executionId 执行ID
     * @return 剩余时间（毫秒），-1 表示永不过期，-2 表示锁不存在
     */
    public long getRemainingTimeToLive(String executionId) {
        String lockKey = SagaRedisKeys.lockKey(executionId);
        RLock lock = redissonClient.getLock(lockKey);
        return lock.remainTimeToLive();
    }
}
