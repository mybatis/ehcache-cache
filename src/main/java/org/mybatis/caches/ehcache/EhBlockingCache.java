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

/**
 * Cache implementation backed by Ehcache 3 that provides basic blocking semantics.
 * <p>
 * In Ehcache 2 this class used {@code BlockingCache} to acquire per-key locks and prevent cache stampedes. Ehcache 3
 * does not provide an equivalent decorator, so blocking must be provided by the caller (e.g. by wrapping this cache
 * with {@link org.apache.ibatis.cache.decorators.BlockingCache}).
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
  }

  @Override
  public Object removeObject(Object key) {
    // this method is called during a rollback to release any previously acquired lock;
    // removing the entry is the correct action for Ehcache 3 (null values are not supported).
    return super.removeObject(key);
  }

}
