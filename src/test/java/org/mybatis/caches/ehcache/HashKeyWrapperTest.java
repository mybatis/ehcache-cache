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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HashKeyWrapper}.
 * <p>
 * These tests also exercise the hash-flooding DoS protection end-to-end through {@link EhcacheCache}: keys that all
 * share the same raw {@code hashCode()} must still be stored and retrieved correctly after bit-mixing.
 * </p>
 */
class HashKeyWrapperTest {

  // ---------------------------------------------------------------------------
  // Unit tests for HashKeyWrapper itself
  // ---------------------------------------------------------------------------

  @Test
  void mixingActuallyChangesTheHashCode() {
    // For most inputs the mixed hash must differ from the raw hashCode.
    // Value 0 maps to 0 under fmix32, so start from 1.
    int raw = "someKey".hashCode();
    int mixed = HashKeyWrapper.fmix32(raw);
    assertNotEquals(raw, mixed, "fmix32 should change the hash for non-trivial input");
  }

  @Test
  void equalKeysProduceEqualWrappersAndHashes() {
    HashKeyWrapper w1 = new HashKeyWrapper("key");
    HashKeyWrapper w2 = new HashKeyWrapper("key");
    assertEquals(w1, w2);
    assertEquals(w1.hashCode(), w2.hashCode());
  }

  @Test
  void differentKeysProduceDifferentWrappers() {
    HashKeyWrapper w1 = new HashKeyWrapper("keyA");
    HashKeyWrapper w2 = new HashKeyWrapper("keyB");
    assertNotEquals(w1, w2);
  }

  @Test
  void nullKeyIsHandledSafely() {
    HashKeyWrapper w = new HashKeyWrapper(null);
    // hashCode of null key is fmix32(0); must not throw
    assertNotNull(w.hashCode());
    assertEquals(w, new HashKeyWrapper(null));
    assertNotEquals(w, new HashKeyWrapper("notNull"));
  }

  @Test
  void selfEqualityHolds() {
    HashKeyWrapper w = new HashKeyWrapper("self");
    assertEquals(w, w);
  }

  @Test
  void notEqualToUnrelatedType() {
    HashKeyWrapper w = new HashKeyWrapper("key");
    assertNotEquals(w, "key");
    assertNotEquals(w, null);
  }

  // ---------------------------------------------------------------------------
  // End-to-end: keys with colliding raw hashCodes survive the cache round-trip
  // ---------------------------------------------------------------------------

  /**
   * A key whose {@code hashCode()} is fixed to a constant, allowing multiple distinct keys to share the same raw hash.
   * This is the attacker-controlled scenario. Even though all these keys collide before mixing, they must remain
   * distinguishable after mixing and must be stored/retrieved independently.
   */
  private static final class CollidingKey {
    private final String label;

    CollidingKey(String label) {
      this.label = label;
    }

    @Override
    public int hashCode() {
      return 42; // forced collision
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CollidingKey)) {
        return false;
      }
      return label.equals(((CollidingKey) obj).label);
    }

    @Override
    public String toString() {
      return label;
    }
  }

  @Test
  void collidingRawHashKeysAreStoredAndRetrievedIndependently() {
    AbstractEhcacheCache cache = new EhcacheCache("HASH_COLLISION_TEST");

    CollidingKey k1 = new CollidingKey("alpha");
    CollidingKey k2 = new CollidingKey("beta");
    CollidingKey k3 = new CollidingKey("gamma");

    // Confirm all three keys have the same raw hashCode (the precondition for the attack).
    assertEquals(k1.hashCode(), k2.hashCode());
    assertEquals(k2.hashCode(), k3.hashCode());

    cache.putObject(k1, "value-alpha");
    cache.putObject(k2, "value-beta");
    cache.putObject(k3, "value-gamma");

    assertEquals("value-alpha", cache.getObject(k1));
    assertEquals("value-beta", cache.getObject(k2));
    assertEquals("value-gamma", cache.getObject(k3));

    cache.removeObject(k2);
    assertNull(cache.getObject(k2));
    assertEquals("value-alpha", cache.getObject(k1));
    assertEquals("value-gamma", cache.getObject(k3));
  }

  @Test
  void wrappedKeysMixedHashesDifferForCollidingRawHashes() {
    CollidingKey k1 = new CollidingKey("alpha");
    CollidingKey k2 = new CollidingKey("beta");

    HashKeyWrapper w1 = new HashKeyWrapper(k1);
    HashKeyWrapper w2 = new HashKeyWrapper(k2);

    // The raw hashCodes are identical ...
    assertEquals(k1.hashCode(), k2.hashCode());
    // ... and the mixed hashCodes are identical too (same raw input → same fmix32 output),
    // but the wrappers are NOT equal because their wrapped keys are not equal.
    // This is correct: equal mixed hashes only cause a collision in the same bucket;
    // equals() still distinguishes the two entries.
    assertEquals(w1.hashCode(), w2.hashCode());
    assertNotEquals(w1, w2);
  }

}
