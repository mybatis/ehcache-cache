/**
 *    Copyright 2010-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
    if (!CACHE_MANAGER.cacheExists(id)) {
      CACHE_MANAGER.addCache(this.id);
      Ehcache ehcache = CACHE_MANAGER.getEhcache(this.id);
      BlockingCache blockingCache = new BlockingCache(ehcache);
      CACHE_MANAGER.replaceCacheWithDecoratedCache(ehcache, blockingCache);
    }
  }

  @Override
  public Object removeObject(Object key) {
    // this method is called during a rollback just to
    // release any previous lock
    cache.put(new Element(key, null));
    return null;
  }

}