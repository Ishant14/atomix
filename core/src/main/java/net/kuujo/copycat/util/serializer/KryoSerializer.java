/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat.util.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValueFactory;
import net.kuujo.copycat.util.ConfigurationException;
import net.kuujo.copycat.util.internal.Assert;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Kryo serializer.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class KryoSerializer extends SerializerConfig {
  private static final String KRYO_SERIALIZER_BUFFER_SIZE = "buffer.size";
  private static final String KRYO_SERIALIZER_REGISTRATIONS = "registrations";

  private static final int DEFAULT_KRYO_SERIALIZER_BUFFER_SIZE = 1024 * 1024 * 16;

  private final Kryo kryo = new Kryo();
  private ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_KRYO_SERIALIZER_BUFFER_SIZE);
  private ByteBufferOutput output;
  private final ByteBufferInput input = new ByteBufferInput();

  public KryoSerializer() {
    super();
    this.output = new ByteBufferOutput(buffer);
    register();
  }

  public KryoSerializer(Map<String, Object> config) {
    super(config);
    this.output = new ByteBufferOutput(buffer);
    register();
  }

  public KryoSerializer(String resource) {
    super(resource);
    register();
  }

  public KryoSerializer(KryoSerializer serializer) {
    super(serializer);
    this.output = new ByteBufferOutput(buffer);
    register();
  }

  /**
   * Registers classes.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void register() {
    if (config.hasPath(KRYO_SERIALIZER_REGISTRATIONS)) {
      ConfigObject config = this.config.getObject(KRYO_SERIALIZER_REGISTRATIONS);
      for (Map.Entry<String, Object> entry : config.unwrapped().entrySet()) {
        Object type = entry.getValue();
        if (type instanceof Class) {
          register((Class) type, Integer.valueOf(entry.getKey()));
        } else if (type instanceof String) {
          try {
            register(Class.forName(type.toString()), Integer.valueOf(entry.getKey()));
          } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Failed to register serializer class", e);
          }
        }
      }
    }
  }

  /**
   * Registers a class for serialization.
   *
   * @param type The class to register.
   * @return The Kryo serializer.
   */
  public KryoSerializer register(Class<?> type) {
    kryo.register(type);
    return this;
  }

  /**
   * Registers a class for serialization.
   *
   * @param type The class to register.
   * @param id The registration ID.
   * @return The Kryo serializer.
   */
  public KryoSerializer register(Class<?> type, int id) {
    kryo.register(type, id);
    return this;
  }

  /**
   * Sets the serializer buffer size.
   *
   * @param bufferSize The serializer buffer size.
   * @throws java.lang.IllegalArgumentException If the buffer size is not positive
   */
  public void setBufferSize(int bufferSize) {
    this.config = config.withValue(KRYO_SERIALIZER_BUFFER_SIZE, ConfigValueFactory.fromAnyRef(Assert.arg(bufferSize, bufferSize > 0, "buffer size must be positive")));
    buffer = ByteBuffer.allocateDirect(bufferSize);
    this.output = new ByteBufferOutput(buffer);
  }

  /**
   * Returns the serializer buffer size.
   *
   * @return The serializer buffer size.
   */
  public int getBufferSize() {
    return config.hasPath(KRYO_SERIALIZER_BUFFER_SIZE) ? config.getInt(KRYO_SERIALIZER_BUFFER_SIZE) : DEFAULT_KRYO_SERIALIZER_BUFFER_SIZE;
  }

  /**
   * Sets the serializer buffer size.
   *
   * @param bufferSize The serializer buffer size.
   * @return The Kryo serializer.
   * @throws java.lang.IllegalArgumentException If the buffer size is not positive
   */
  public KryoSerializer withBufferSize(int bufferSize) {
    setBufferSize(bufferSize);
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized <T> T readObject(ByteBuffer buffer) {
    input.setBuffer(buffer);
    return (T) kryo.readClassAndObject(input);
  }

  @Override
  public synchronized ByteBuffer writeObject(Object object) {
    kryo.writeClassAndObject(output, object);
    byte[] bytes = output.toBytes();
    output.clear();
    return ByteBuffer.wrap(bytes);
  }

}
