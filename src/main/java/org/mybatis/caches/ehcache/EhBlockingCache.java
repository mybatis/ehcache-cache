/*
 *    Copyright 2010-2022 the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

/**
 * The Class EhBlockingCache.
 *
 * @author Iwao AVE!
 */
public class EhBlockingCache extends AbstractEhcacheCache {

  /**
   * Instantiates a new eh blocking cache.
   *
   * @param id
   *          the id
   */
  public EhBlockingCache(final String id) {
    super(id);
    if (CACHE_MANAGER.getCache(id, Object.class, Object.class) == null) {
      CACHE_MANAGER.createCache(this.id, CacheConfigurationBuilder
          .newCacheConfigurationBuilder(Object.class, Object.class,
              ResourcePoolsBuilder.newResourcePoolsBuilder().heap(this.maxEntriesLocalHeap, EntryUnit.ENTRIES)
                  .offheap(this.maxEntriesLocalDisk, MemoryUnit.MB))
          .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.of(this.timeToIdleSeconds, ChronoUnit.SECONDS)))
          .withExpiry(
              ExpiryPolicyBuilder.timeToLiveExpiration(Duration.of(this.timeToLiveSeconds, ChronoUnit.SECONDS))));
      Cache<Object, Object> ehcache = CACHE_MANAGER.getCache(this.id, Object.class, Object.class);
      // BlockingCache blockingCache = new BlockingCache(ehcache);
      // CACHE_MANAGER.replaceCacheWithDecoratedCache(ehcache, blockingCache);
    }
    this.cache = CACHE_MANAGER.getCache(id, Object.class, Object.class);
  }

  @Override
  public Object removeObject(Object key) {
    // this method is called during a rollback just to
    // release any previous lock
    cache.put(key, null);
    return null;
  }

}
