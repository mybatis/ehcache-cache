/*
 *    Copyright 2012 The original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

/**
 * @version $Id$
 */
public final class EhcacheTestCase {

    private static final String DEFAULT_ID = "EHCACHE";

    // CacheManager holds any settings between tests
    private AbstractEhcacheCache cache;

    @Before
    public void newCache() {
        cache = new EhcacheCache(DEFAULT_ID);
    }

    @Test
    public void shouldDemonstrateHowAllObjectsAreKept() {
        for (int i = 0; i < 100000; i++) {
            cache.putObject(i, i);
            assertEquals(i, cache.getObject(i));
        }
        assertEquals(100000, cache.getSize());
    }

    @Test
    public void shouldDemonstrateCopiesAreEqual() {
        for (int i = 0; i < 1000; i++) {
            cache.putObject(i, i);
            assertEquals(i, cache.getObject(i));
        }
    }

    @Test
    public void shouldRemoveItemOnDemand() {
        cache.putObject(0, 0);
        assertNotNull(cache.getObject(0));
        cache.removeObject(0);
        assertNull(cache.getObject(0));
    }

    @Test
    public void shouldFlushAllItemsOnDemand() {
        for (int i = 0; i < 5; i++) {
            cache.putObject(i, i);
        }
        assertNotNull(cache.getObject(0));
        assertNotNull(cache.getObject(4));
        cache.clear();
        assertNull(cache.getObject(0));
        assertNull(cache.getObject(4));
    }

    @Test
    public void shouldChangeTimeToLive() throws Exception {
      cache.putObject("test", "test");
      Thread.sleep(1200);
      assertEquals("test", cache.getObject("test"));
      cache.setTimeToLiveSeconds(1);
      Thread.sleep(1200);
      assertNull(cache.getObject("test"));
      this.resetCache();
    }

    @Test
    public void shouldChangeTimeToIdle() throws Exception {
      cache.putObject("test", "test");
      Thread.sleep(1200);
      assertEquals("test", cache.getObject("test"));
      cache.setTimeToIdleSeconds(1);
      Thread.sleep(1200);
      assertNull(cache.getObject("test"));
      this.resetCache();
    }

    @Test
    public void shouldTestEvictionPolicy() throws Exception {
      cache.clear();
      cache.setMemoryStoreEvictionPolicy("FIFO");
      cache.setMaxEntriesLocalHeap(1);
      cache.setMaxEntriesLocalDisk(1);
      cache.putObject("eviction", "eviction");
      cache.putObject("eviction2", "eviction2");
      cache.putObject("eviction3", "eviction3");
      Thread.sleep(1200);
      assertEquals(1, cache.getSize());
      this.resetCache();
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotCreateCache() {
      cache = new EhcacheCache(null);
    }

    @Test
    public void shouldVerifyCacheId() {
      assertEquals("EHCACHE", cache.getId());
    }

    @Test
    public void shouldVerifyToString() {
      assertEquals("EHCache {EHCACHE}", cache.toString());
    }

    @Test
    public void equalsAndHashCodeSymmetricTest() {
      // equals and hashCode check name field value
      AbstractEhcacheCache x = new EhcacheCache("EHCACHE");
      AbstractEhcacheCache y = new EhcacheCache("EHCACHE");
      assertTrue(x.equals(y));
      assertTrue(y.equals(x));
      assertEquals(x.hashCode(), y.hashCode());
      // dummy tests to cover edge cases
      assertFalse(x.equals(new String()));
      assertFalse(x.equals(null));
      assertTrue(x.equals(x));
    }

    // CacheManager holds reference to settings, reset this for other tests
    private void resetCache() {
        cache.setTimeToLiveSeconds(120);
        cache.setTimeToIdleSeconds(120);
        cache.setMemoryStoreEvictionPolicy("LRU");
    }
    
}
