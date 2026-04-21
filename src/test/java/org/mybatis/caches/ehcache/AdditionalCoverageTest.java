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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.ehcache.spi.serialization.SerializerException;
import org.junit.jupiter.api.Test;

class AdditionalCoverageTest {

  @Test
  void loggingEhcacheConstructorCreatesUsableCache() {
    LoggingEhcache cache = new LoggingEhcache("LOGGING_CACHE");

    cache.putObject("k", "v");

    assertEquals("LOGGING_CACHE", cache.getId());
    assertEquals("v", cache.getObject("k"));
  }

  @Test
  void dummyReadWriteLockUsesSameNoOpLockForReadAndWrite() throws Exception {
    DummyReadWriteLock readWriteLock = new DummyReadWriteLock();

    Lock readLock = readWriteLock.readLock();
    Lock writeLock = readWriteLock.writeLock();

    assertSame(readLock, writeLock);
    assertDoesNotThrow(readLock::lock);
    assertDoesNotThrow(readLock::lockInterruptibly);
    assertTrue(readLock.tryLock());
    assertTrue(readLock.tryLock(1L, TimeUnit.MILLISECONDS));
    assertDoesNotThrow(readLock::unlock);
    assertNull(readLock.newCondition());
  }

  @Test
  void objectSerializerRoundTripAndEqualsWork() throws Exception {
    ObjectSerializer serializer = new ObjectSerializer(getClass().getClassLoader());
    SampleValue value = new SampleValue("name", 7);

    ByteBuffer serialized = serializer.serialize(value);

    assertNotNull(serialized);
    assertEquals(value, serializer.read(serialized.duplicate()));
    assertTrue(serializer.equals(value, serialized.duplicate()));
    assertFalse(serializer.equals(new SampleValue("other", 7), serialized.duplicate()));
  }

  @Test
  void objectSerializerThrowsForNonSerializableObject() {
    ObjectSerializer serializer = new ObjectSerializer(getClass().getClassLoader());

    assertThrows(SerializerException.class, () -> serializer.serialize(new Object()));
  }

  @Test
  void objectSerializerThrowsForInvalidSerializedBytes() {
    ObjectSerializer serializer = new ObjectSerializer(getClass().getClassLoader());
    ByteBuffer invalidData = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 });

    assertThrows(SerializerException.class, () -> serializer.read(invalidData));
  }

  @Test
  void abstractCacheUnlockAndReadWriteLockDefaultBehavior() {
    AbstractEhcacheCache cache = new EhcacheCache("ABSTRACT_BEHAVIOR");

    assertDoesNotThrow(() -> cache.unlock("k"));
    assertNull(cache.getReadWriteLock());
  }

  private static final class SampleValue implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final int number;

    private SampleValue(String name, int number) {
      this.name = name;
      this.number = number;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SampleValue)) {
        return false;
      }
      SampleValue other = (SampleValue) obj;
      return number == other.number && name.equals(other.name);
    }

    @Override
    public int hashCode() {
      return 31 * name.hashCode() + number;
    }
  }
}
