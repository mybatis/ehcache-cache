/*
 *    Copyright 2010-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.caches.ehcache;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

/**
 * Cache adapter for Ehcache 3.
 *
 * @author Simone Tripodi
 */
public abstract class AbstractEhcacheCache implements Cache {

  /** Placeholder stored in Ehcache 3 for entries whose actual value is {@code null}. */
  private static final Object NULL_VALUE = new NullValue();

  /**
   * The cache manager reference. A {@link PersistentCacheManager} is used so that individual caches may optionally
   * configure a disk tier via {@link #setMaxBytesLocalDisk(long)}.
   */
  protected static PersistentCacheManager CACHE_MANAGER = CacheManagerBuilder.newCacheManagerBuilder()
      .with(
          CacheManagerBuilder.persistence(Path.of(System.getProperty("java.io.tmpdir"), "ehcache-mybatis").toString()))
      .build(true);

  /**
   * The cache id (namespace).
   */
  protected final String id;

  /**
   * The cache instance (lazily initialised on first use).
   */
  protected org.ehcache.Cache<Object, Object> cache;

  protected long timeToIdleSeconds;
  protected long timeToLiveSeconds;
  protected long maxEntriesLocalHeap;
  protected long maxEntriesLocalDisk;
  protected long maxBytesLocalDisk;
  protected String memoryStoreEvictionPolicy;

  /**
   * Instantiates a new abstract ehcache cache.
   *
   * @param id
   *          the cache id (namespace)
   */
  public AbstractEhcacheCache(final String id) {
    if (id == null) {
      throw new IllegalArgumentException("Cache instances require an ID");
    }
    this.id = id;
    // Remove any pre-existing cache so this instance always starts with a fresh default configuration.
    if (CACHE_MANAGER.getCache(id, Object.class, Object.class) != null) {
      CACHE_MANAGER.removeCache(id);
    }
  }

  /**
   * Returns the underlying Ehcache 3 cache, creating it on first use with the current configuration.
   */
  protected synchronized org.ehcache.Cache<Object, Object> getOrCreateCache() {
    if (cache == null) {
      cache = buildAndRegisterCache();
    }
    return cache;
  }

  /**
   * Builds and registers a new Ehcache 3 cache instance using the current configuration fields.
   */
  protected org.ehcache.Cache<Object, Object> buildAndRegisterCache() {
    if (CACHE_MANAGER.getCache(id, Object.class, Object.class) != null) {
      CACHE_MANAGER.removeCache(id);
    }
    long heapEntries = maxEntriesLocalHeap > 0 ? maxEntriesLocalHeap : Long.MAX_VALUE / 2;
    ResourcePoolsBuilder poolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder().heap(heapEntries,
        EntryUnit.ENTRIES);
    if (maxBytesLocalDisk > 0) {
      poolsBuilder = poolsBuilder.disk(maxBytesLocalDisk, MemoryUnit.B);
    }
    CacheConfigurationBuilder<Object, Object> builder = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(Object.class, Object.class, poolsBuilder).withExpiry(buildExpiryPolicy());
    if (maxBytesLocalDisk > 0) {
      // Disk and off-heap tiers require a Serializer since entries cannot be stored as object references.
      // ObjectSerializer uses standard Java serialisation; cached values must implement Serializable.
      builder = builder.withKeySerializer(ObjectSerializer.class).withValueSerializer(ObjectSerializer.class);
    }
    CACHE_MANAGER.createCache(id, builder.build());
    return CACHE_MANAGER.getCache(id, Object.class, Object.class);
  }

  private org.ehcache.expiry.ExpiryPolicy<Object, Object> buildExpiryPolicy() {
    if (timeToLiveSeconds > 0) {
      return ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(timeToLiveSeconds));
    }
    if (timeToIdleSeconds > 0) {
      return ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(timeToIdleSeconds));
    }
    return ExpiryPolicyBuilder.noExpiration();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    getOrCreateCache().clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getObject(Object key) {
    Object value = getOrCreateCache().get(new HashKeyWrapper(key));
    return value instanceof NullValue ? null : value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getSize() {
    int count = 0;
    for (org.ehcache.Cache.Entry<Object, Object> ignored : getOrCreateCache()) {
      count++;
    }
    return count;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putObject(Object key, Object value) {
    getOrCreateCache().put(new HashKeyWrapper(key), value == null ? NULL_VALUE : value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object removeObject(Object key) {
    Object obj = getObject(key);
    getOrCreateCache().remove(new HashKeyWrapper(key));
    return obj;
  }

  /**
   * {@inheritDoc}
   */
  public void unlock(Object key) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Cache)) {
      return false;
    }

    Cache otherCache = (Cache) obj;
    return id.equals(otherCache.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "EHCache {" + id + "}";
  }

  // DYNAMIC PROPERTIES

  /**
   * Sets the time to idle for an element before it expires. Is only used if the element is not eternal. If the cache
   * has already been initialised the configuration change takes effect immediately by recreating the cache.
   *
   * @param timeToIdleSeconds
   *          the default amount of time to live for an element from its last accessed or modified date
   */
  public void setTimeToIdleSeconds(long timeToIdleSeconds) {
    this.timeToIdleSeconds = timeToIdleSeconds;
    recreateCacheIfInitialized();
  }

  /**
   * Sets the time to live for an element before it expires. Is only used if the element is not eternal. If the cache
   * has already been initialised the configuration change takes effect immediately by recreating the cache.
   *
   * @param timeToLiveSeconds
   *          the default amount of time to live for an element from its creation date
   */
  public void setTimeToLiveSeconds(long timeToLiveSeconds) {
    this.timeToLiveSeconds = timeToLiveSeconds;
    recreateCacheIfInitialized();
  }

  /**
   * Sets the maximum objects to be held in memory (0 = no limit). If the cache has already been initialised the
   * configuration change takes effect immediately by recreating the cache.
   *
   * @param maxEntriesLocalHeap
   *          The maximum number of elements in heap, before they are evicted (0 == no limit)
   */
  public void setMaxEntriesLocalHeap(long maxEntriesLocalHeap) {
    this.maxEntriesLocalHeap = maxEntriesLocalHeap;
    recreateCacheIfInitialized();
  }

  /**
   * Sets the maximum number elements on Disk. 0 means unlimited.
   * <p>
   * Note: this property is retained for compatibility but has no effect in Ehcache 3, which does not support an
   * entry-count limit for the disk tier. Use {@link #setMaxBytesLocalDisk(long)} to configure disk storage instead.
   * </p>
   *
   * @param maxEntriesLocalDisk
   *          the maximum number of Elements to allow on the disk. 0 means unlimited.
   */
  public void setMaxEntriesLocalDisk(long maxEntriesLocalDisk) {
    this.maxEntriesLocalDisk = maxEntriesLocalDisk;
    recreateCacheIfInitialized();
  }

  /**
   * Sets the maximum bytes to be used for the disk tier. When greater than zero a disk resource pool is added to the
   * cache, allowing entries evicted from the heap to overflow to disk. If set to zero (the default) no disk tier is
   * configured and the cache is heap-only.
   *
   * @param maxBytesLocalDisk
   *          the maximum number of bytes to allocate on disk. 0 means no disk tier (heap-only).
   */
  public void setMaxBytesLocalDisk(long maxBytesLocalDisk) {
    this.maxBytesLocalDisk = maxBytesLocalDisk;
    recreateCacheIfInitialized();
  }

  /**
   * Sets the eviction policy. Stored for informational purposes; Ehcache 3 manages its own eviction strategy.
   *
   * @param memoryStoreEvictionPolicy
   *          a String representation of the policy. One of "LRU", "LFU" or "FIFO".
   */
  public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
    this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
    recreateCacheIfInitialized();
  }

  /**
   * Recreates the underlying Ehcache 3 cache with the current configuration if the cache has already been initialised.
   * Called by property setters when a configuration change is requested after first use.
   */
  protected synchronized void recreateCacheIfInitialized() {
    if (cache != null) {
      cache = buildAndRegisterCache();
    }
  }

  /**
   * Placeholder used to represent a cached {@code null} value. Ehcache 3 does not permit null values, so this sentinel
   * is stored and translated back to {@code null} on retrieval.
   */
  private static final class NullValue implements Serializable {
    private static final long serialVersionUID = 1L;
  }

}
