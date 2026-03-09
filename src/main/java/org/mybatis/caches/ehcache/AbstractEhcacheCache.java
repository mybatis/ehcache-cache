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

import java.util.concurrent.locks.ReadWriteLock;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.ibatis.cache.Cache;

/**
 * Cache adapter for Ehcache.
 *
 * @author Simone Tripodi
 */
public abstract class AbstractEhcacheCache implements Cache {

  /**
   * The cache manager reference.
   */
  protected static CacheManager CACHE_MANAGER = CacheManager.create();

  /**
   * The cache id (namespace).
   */
  protected final String id;

  /**
   * The cache instance.
   */
  protected Ehcache cache;

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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    cache.removeAll();
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
    Element cachedElement = cache.get(key);
    if (cachedElement == null) {
      return null;
    }
    return cachedElement.getObjectValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getSize() {
    return cache.getSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putObject(Object key, Object value) {
    cache.put(new Element(key, value));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object removeObject(Object key) {
    Object obj = getObject(key);
    cache.remove(key);
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
   * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
   *
   * @param timeToIdleSeconds
   *          the default amount of time to live for an element from its last accessed or modified date
   */
  public void setTimeToIdleSeconds(long timeToIdleSeconds) {
    cache.getCacheConfiguration().setTimeToIdleSeconds(timeToIdleSeconds);
  }

  /**
   * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
   *
   * @param timeToLiveSeconds
   *          the default amount of time to live for an element from its creation date
   */
  public void setTimeToLiveSeconds(long timeToLiveSeconds) {
    cache.getCacheConfiguration().setTimeToLiveSeconds(timeToLiveSeconds);
  }

  /**
   * Sets the maximum objects to be held in memory (0 = no limit).
   *
   * @param maxEntriesLocalHeap
   *          The maximum number of elements in heap, before they are evicted (0 == no limit)
   */
  public void setMaxEntriesLocalHeap(long maxEntriesLocalHeap) {
    cache.getCacheConfiguration().setMaxEntriesLocalHeap(maxEntriesLocalHeap);
  }

  /**
   * Sets the maximum number elements on Disk. 0 means unlimited.
   *
   * @param maxEntriesLocalDisk
   *          the maximum number of Elements to allow on the disk. 0 means unlimited.
   */
  public void setMaxEntriesLocalDisk(long maxEntriesLocalDisk) {
    cache.getCacheConfiguration().setMaxEntriesLocalDisk(maxEntriesLocalDisk);
  }

  /**
   * Sets the maximum bytes to be used for the disk tier. When greater than zero the cache will overflow to disk when
   * the heap tier is full.
   * <p>
   * In Ehcache 2, {@code maxBytesLocalDisk} and {@code maxEntriesLocalDisk} are mutually exclusive, and the disk pool
   * type cannot be changed on a running cache instance. This method therefore removes and recreates the underlying
   * cache with a fresh {@link net.sf.ehcache.config.CacheConfiguration} that applies the requested byte limit while
   * preserving the other settings (TTI, TTL, heap size, eviction policy) from the current configuration.
   * </p>
   *
   * @param maxBytesLocalDisk
   *          the maximum number of bytes to allocate on disk. 0 means no disk tier (heap-only).
   */
  public void setMaxBytesLocalDisk(long maxBytesLocalDisk) {
    net.sf.ehcache.config.CacheConfiguration current = cache.getCacheConfiguration();
    // Build a fresh CacheConfiguration so that onDiskPoolUsage is unset and setMaxBytesLocalDisk
    // can be applied without triggering the "can't switch disk pool" guard in Ehcache 2.
    net.sf.ehcache.config.CacheConfiguration newConfig = new net.sf.ehcache.config.CacheConfiguration(id,
        (int) current.getMaxEntriesLocalHeap());
    newConfig.setTimeToIdleSeconds(current.getTimeToIdleSeconds());
    newConfig.setTimeToLiveSeconds(current.getTimeToLiveSeconds());
    newConfig.setEternal(current.isEternal());
    newConfig.setMemoryStoreEvictionPolicy(current.getMemoryStoreEvictionPolicy().toString());
    newConfig.setMaxBytesLocalDisk(maxBytesLocalDisk);
    rebuildCacheWith(newConfig);
  }

  /**
   * Removes the existing cache from the {@link CacheManager} and registers a new one built from {@code newConfig}.
   * Subclasses may override this method to apply additional decorators (e.g.
   * {@link net.sf.ehcache.constructs.blocking.BlockingCache}).
   *
   * @param newConfig
   *          the configuration to use for the replacement cache
   */
  protected void rebuildCacheWith(net.sf.ehcache.config.CacheConfiguration newConfig) {
    CACHE_MANAGER.removeCache(id);
    CACHE_MANAGER.addCache(new net.sf.ehcache.Cache(newConfig));
    this.cache = CACHE_MANAGER.getEhcache(id);
  }

  /**
   * Sets the eviction policy. An invalid argument will set it to null.
   *
   * @param memoryStoreEvictionPolicy
   *          a String representation of the policy. One of "LRU", "LFU" or "FIFO".
   */
  public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
    cache.getCacheConfiguration().setMemoryStoreEvictionPolicy(memoryStoreEvictionPolicy);
  }

}
