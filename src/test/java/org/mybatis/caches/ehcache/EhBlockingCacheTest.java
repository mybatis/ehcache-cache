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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EhBlockingCacheTest {

  private static final String DEFAULT_ID = "EHBLOCKINGCACHE";

  // CacheManager holds any settings between tests
  private AbstractEhcacheCache cache;

  @BeforeEach
  void newCache() {
    cache = new EhBlockingCache(DEFAULT_ID);
  }

  @Test
  void shouldDemonstrateHowAllObjectsAreKept() {
    for (int i = 0; i < 100000; i++) {
      cache.putObject(i, i);
      assertEquals(i, cache.getObject(i));
    }
    assertEquals(100000, cache.getSize());
  }

  @Test
  void shouldDemonstrateCopiesAreEqual() {
    for (int i = 0; i < 1000; i++) {
      cache.putObject(i, i);
      assertEquals(i, cache.getObject(i));
    }
  }

  @Test
  void shouldRemoveItemOnDemand() {
    cache.putObject(0, 0);
    assertNotNull(cache.getObject(0));
    cache.removeObject(0);
    assertNull(cache.getObject(0));
  }

  @Test
  void shouldFlushAllItemsOnDemand() {
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
  void shouldChangeTimeToLive() throws Exception {
    cache.putObject("test", "test");
    Thread.sleep(1200);
    assertEquals("test", cache.getObject("test"));
    cache.setTimeToLiveSeconds(1);
    Thread.sleep(1200);
    assertNull(cache.getObject("test"));
    this.resetCache();
  }

  @Test
  void shouldChangeTimeToIdle() throws Exception {
    cache.putObject("test", "test");
    Thread.sleep(1200);
    assertEquals("test", cache.getObject("test"));
    cache.setTimeToIdleSeconds(1);
    Thread.sleep(1200);
    assertNull(cache.getObject("test"));
    this.resetCache();
  }

  @Test
  void shouldTestEvictionPolicy() throws Exception {
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

  @Test
  void shouldSetMaxBytesLocalDisk() {
    // Use a distinct cache ID to avoid interfering with the shared EHBLOCKINGCACHE used by other tests.
    // setMaxBytesLocalDisk triggers a cache rebuild because Ehcache 2 does not allow enabling
    // the byte-based disk pool on an already-running cache instance. The rebuild re-applies the
    // BlockingCache decorator so locking semantics are preserved.
    AbstractEhcacheCache diskCache = new EhBlockingCache("EHBLOCKINGCACHE_DISK_TEST");
    try {
      diskCache.setMaxBytesLocalDisk(10 * 1024 * 1024L); // 10 MB
      assertEquals(10 * 1024 * 1024L, diskCache.cache.getCacheConfiguration().getMaxBytesLocalDisk());
      assertEquals(0, diskCache.cache.getCacheConfiguration().getMaxEntriesLocalDisk());
      diskCache.putObject("key", "value");
      assertEquals("value", diskCache.getObject("key"));
    } finally {
      AbstractEhcacheCache.CACHE_MANAGER.removeCache("EHBLOCKINGCACHE_DISK_TEST");
    }
  }

  @Test
  void shouldSupportDiskOverflow() {
    // Use a distinct cache ID to avoid interfering with the shared EHBLOCKINGCACHE used by other tests.
    AbstractEhcacheCache diskCache = new EhBlockingCache("EHBLOCKINGCACHE_DISK_OVERFLOW_TEST");
    try {
      diskCache.setMaxEntriesLocalHeap(1); // limit heap to 1 entry so others overflow to disk
      diskCache.setMaxBytesLocalDisk(10 * 1024 * 1024L); // 10 MB — triggers rebuild with disk tier
      diskCache.putObject("key1", "value1");
      diskCache.putObject("key2", "value2"); // key1 overflows to disk
      diskCache.putObject("key3", "value3"); // key2 overflows to disk
      // All entries must remain retrievable; heap-evicted entries should be found on disk.
      assertEquals("value1", diskCache.getObject("key1"));
      assertEquals("value2", diskCache.getObject("key2"));
      assertEquals("value3", diskCache.getObject("key3"));
    } finally {
      AbstractEhcacheCache.CACHE_MANAGER.removeCache("EHBLOCKINGCACHE_DISK_OVERFLOW_TEST");
    }
  }

  @Test
  void shouldNotCreateCache() {
    assertThrows(IllegalArgumentException.class, () -> {
      cache = new EhBlockingCache(null);
    });
  }

  @Test
  void shouldVerifyCacheId() {
    assertEquals(DEFAULT_ID, cache.getId());
  }

  @Test
  void shouldVerifyToString() {
    assertEquals("EHCache {EHBLOCKINGCACHE}", cache.toString());
  }

  @Test
  void equalsAndHashCodeSymmetricTest() {
    // equals and hashCode check name field value
    AbstractEhcacheCache x = new EhBlockingCache(DEFAULT_ID);
    AbstractEhcacheCache y = new EhBlockingCache(DEFAULT_ID);
    assertEquals(x, y);
    assertEquals(y, x);
    assertEquals(x.hashCode(), y.hashCode());
    // dummy tests to cover edge cases
    assertNotEquals(x, new String());
    assertNotNull(x);
    assertEquals(x, x);
  }

  // CacheManager holds reference to settings, reset this for other tests
  private void resetCache() {
    cache.setTimeToLiveSeconds(120);
    cache.setTimeToIdleSeconds(120);
    cache.setMemoryStoreEvictionPolicy("LRU");
  }

}
