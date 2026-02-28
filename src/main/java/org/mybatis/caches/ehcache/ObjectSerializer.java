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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

/**
 * Ehcache 3 {@link Serializer} that uses standard Java serialization. This serializer is required when off-heap or
 * disk-based storage tiers are used, since those tiers cannot store object references directly.
 * <p>
 * Note: heap-only caches do not need serialization; this class is provided for configurations that add off-heap or disk
 * tiers.
 * </p>
 */
public class ObjectSerializer implements Serializer<Object> {

  /**
   * Constructor required by Ehcache 3's serializer contract.
   *
   * @param loader
   *          the class loader to use when deserialising objects
   */
  public ObjectSerializer(ClassLoader loader) {
    // class loader is not used; standard ObjectInputStream resolves classes through the context class loader
  }

  @Override
  public ByteBuffer serialize(Object object) throws SerializerException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      oos.flush();
      return ByteBuffer.wrap(baos.toByteArray());
    } catch (IOException e) {
      throw new SerializerException("Failed to serialize object", e);
    }
  }

  @Override
  public Object read(ByteBuffer binary) throws ClassNotFoundException, SerializerException {
    byte[] bytes = new byte[binary.remaining()];
    binary.get(bytes);
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return ois.readObject();
    } catch (IOException e) {
      throw new SerializerException("Failed to deserialize object", e);
    }
  }

  @Override
  public boolean equals(Object object, ByteBuffer binary) throws ClassNotFoundException, SerializerException {
    return object.equals(read(binary));
  }

}
