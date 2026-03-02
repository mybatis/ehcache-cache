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

/**
 * Key wrapper that applies Murmur3 32-bit finalizer bit-mixing to {@link Object#hashCode()} before the value is exposed
 * to Ehcache 3's internal hash structures.
 * <p>
 * Ehcache 3 uses the raw {@code hashCode()} of stored keys without any internal spreading. When the cache is
 * <em>bounded</em> (i.e. a maximum entry count or size has been configured), an attacker who can influence the cache
 * keys — for example by controlling SQL query parameters that end up in MyBatis's {@code CacheKey} — can craft a set of
 * keys that all share the same raw hash, degrading every cache operation from O(1) to O(n) and causing a
 * <strong>hash-flooding denial of service</strong>.
 * </p>
 * <p>
 * Wrapping every key in this class ensures that Ehcache 3 always sees a <em>mixed</em> hash value. The mixing function
 * (Murmur3 fmix32) has strong avalanche properties: a single-bit difference in the input changes roughly half of the
 * output bits, making it computationally infeasible to produce many keys with the same mixed hash.
 * </p>
 *
 * @see <a href="https://github.com/jhipster/generator-jhipster/issues/28546">jhipster/generator-jhipster #28546</a>
 * @see <a href="https://github.com/mybatis/ehcache-cache/issues/61">mybatis/ehcache-cache #61</a>
 */
final class HashKeyWrapper implements Serializable {

  private static final long serialVersionUID = 1L;

  /** The original, unwrapped cache key. */
  private final Object key;

  /**
   * The pre-mixed hash code, computed once at construction time. Using a {@code final} field avoids re-computing the
   * mixing on every lookup.
   */
  private final int hash;

  /**
   * Wraps {@code key}, pre-computing its mixed hash.
   *
   * @param key
   *          the original cache key; may be {@code null}
   */
  HashKeyWrapper(Object key) {
    this.key = key;
    this.hash = fmix32(key == null ? 0 : key.hashCode());
  }

  /**
   * Returns the original cache key that this wrapper was constructed with.
   *
   * @return the original key, possibly {@code null}
   */
  Object getKey() {
    return key;
  }

  /**
   * Returns the pre-mixed hash code of the wrapped key.
   * <p>
   * The value is derived by applying the Murmur3 32-bit finalizer (fmix32) to {@code key.hashCode()}. The finalizer has
   * excellent avalanche properties: every output bit depends on every input bit, so an attacker who knows the raw
   * {@code hashCode()} of a key cannot predict which Ehcache 3 internal bucket the mixed hash will land in.
   * </p>
   */
  @Override
  public int hashCode() {
    return hash;
  }

  /**
   * Two {@code HashKeyWrapper} instances are equal when their wrapped keys are equal according to
   * {@link Object#equals}.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof HashKeyWrapper)) {
      return false;
    }
    HashKeyWrapper other = (HashKeyWrapper) obj;
    return key == null ? other.key == null : key.equals(other.key);
  }

  @Override
  public String toString() {
    return "HashKeyWrapper{" + key + "}";
  }

  /**
   * Murmur3 32-bit finalizer (fmix32). Applies three xor-shift-multiply rounds that give near-perfect bit avalanche.
   *
   * @param h
   *          raw hash value to mix
   *
   * @return mixed hash value
   */
  static int fmix32(int h) {
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;
    return h;
  }

}
