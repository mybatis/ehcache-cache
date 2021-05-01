/*
 *    Copyright 2010-2021 the original author or authors.
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

import java.nio.ByteBuffer;

import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

public class ObjectSerializer implements Serializer<Object> {

  public ObjectSerializer(ClassLoader loader) {
    // no-op
  }

  @Override
  public boolean equals(Object arg0, ByteBuffer arg1) throws ClassNotFoundException, SerializerException {
    return false;
  }

  @Override
  public Object read(ByteBuffer arg0) throws ClassNotFoundException, SerializerException {
    return null;
  }

  @Override
  public ByteBuffer serialize(Object arg0) throws SerializerException {
    return null;
  }

}
