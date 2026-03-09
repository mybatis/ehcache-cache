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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;

/**
 * Cache implementation that wraps Ehcache 2 with {@link BlockingCache} semantics.
 * <p>
 * {@link BlockingCache} acquires a per-key lock when a cache miss occurs so that only one thread computes the missing
 * value while others block. This prevents cache-stampede on a cold or expired entry.
 * </p>
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
    if (!CACHE_MANAGER.cacheExists(id)) {
      CACHE_MANAGER.addCache(this.id);
      Ehcache ehcache = CACHE_MANAGER.getEhcache(this.id);
      BlockingCache blockingCache = new BlockingCache(ehcache);
      CACHE_MANAGER.replaceCacheWithDecoratedCache(ehcache, blockingCache);
    }
    this.cache = CACHE_MANAGER.getEhcache(id);
  }

  @Override
  public Object removeObject(Object key) {
    // this method is called during a rollback just to
    // release any previous lock
    cache.put(new Element(key, null));
    return null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Re-wraps the rebuilt cache in a {@link BlockingCache} after replacing it.
   * </p>
   */
  @Override
  protected void rebuildCacheWith(net.sf.ehcache.config.CacheConfiguration newConfig) {
    CACHE_MANAGER.removeCache(id);
    CACHE_MANAGER.addCache(new net.sf.ehcache.Cache(newConfig));
    Ehcache ehcache = CACHE_MANAGER.getEhcache(id);
    BlockingCache blockingCache = new BlockingCache(ehcache);
    CACHE_MANAGER.replaceCacheWithDecoratedCache(ehcache, blockingCache);
    this.cache = CACHE_MANAGER.getEhcache(id);
  }

}
