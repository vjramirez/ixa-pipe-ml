/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eus.ixa.ixa.pipe.ml.sequence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ext.ExtensionLoader;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.AggregatedFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.featuregen.GeneratorFactory;

/**
 * Based on opennlp.tools.namefind.TokenNameFinderFactory.
 * 
 * @author ragerri
 * @version 2016-11-11
 */
public class SequenceLabelerFactory extends BaseToolFactory {

  private byte[] featureGeneratorBytes;
  private Map<String, Object> resources;
  private SequenceLabelerCodec<String> seqCodec;

  /**
   * Creates a {@link SequenceLabelerFactory} that provides the default
   * implementation of the resources.
   */
  public SequenceLabelerFactory() {
    this.seqCodec = new BioCodec();
  }

  public SequenceLabelerFactory(final byte[] featureGeneratorBytes,
      final Map<String, Object> resources,
      final SequenceLabelerCodec<String> seqCodec) {
    init(featureGeneratorBytes, resources, seqCodec);
  }

  void init(final byte[] featureGeneratorBytes,
      final Map<String, Object> resources,
      final SequenceLabelerCodec<String> seqCodec) {
    this.featureGeneratorBytes = featureGeneratorBytes;
    this.resources = resources;
    this.seqCodec = seqCodec;
  }

  //TODO call this method for default feature generation
  private static byte[] loadDefaultFeatureGeneratorBytes() {

    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (InputStream in = SequenceLabelerFactory.class.getResourceAsStream(
        "/sequenceLabeler/default-feature-descriptor.xml")) {

      if (in == null) {
        throw new IllegalStateException(
            "Classpath must contain default-feature-descriptor.xml file!");
      }

      final byte buf[] = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        bytes.write(buf, 0, len);
      }
    } catch (final IOException e) {
      throw new IllegalStateException(
          "Failed reading from default-feature-descriptor.xml file on classpath!");
    }

    return bytes.toByteArray();
  }

  protected SequenceLabelerCodec<String> getSequenceCodec() {
    return this.seqCodec;
  }

  protected Map<String, Object> getResources() {
    return this.resources;
  }

  protected byte[] getFeatureGenerator() {
    return this.featureGeneratorBytes;
  }

  public static SequenceLabelerFactory create(final String subclassName,
      final byte[] featureGeneratorBytes, final Map<String, Object> resources,
      final SequenceLabelerCodec<String> seqCodec)
      throws InvalidFormatException {
    SequenceLabelerFactory theFactory;
    if (subclassName == null) {
      // will create the default factory
      theFactory = new SequenceLabelerFactory();
    } else {
      try {
        theFactory = ExtensionLoader
            .instantiateExtension(SequenceLabelerFactory.class, subclassName);
      } catch (final Exception e) {
        final String msg = "Could not instantiate the " + subclassName
            + ". The initialization throw an exception.";
        System.err.println(msg);
        e.printStackTrace();
        throw new InvalidFormatException(msg, e);
      }
    }
    theFactory.init(featureGeneratorBytes, resources, seqCodec);
    return theFactory;
  }

  @Override
  public void validateArtifactMap() throws InvalidFormatException {
    // no additional artifacts
  }

  public SequenceLabelerCodec<String> createSequenceCodec() {

    if (this.artifactProvider != null) {
      final String sequeceCodecImplName = this.artifactProvider
          .getManifestProperty(
              SequenceLabelerModel.SEQUENCE_CODEC_CLASS_NAME_PARAMETER);
      return instantiateSequenceCodec(sequeceCodecImplName);
    } else {
      return this.seqCodec;
    }
  }

  public SequenceLabelerContextGenerator createContextGenerator() {

    final AdaptiveFeatureGenerator featureGenerator = createFeatureGenerators();
    if (featureGenerator == null) {
      throw new NullPointerException("featureGenerator must not be null");
    }

    return new DefaultSequenceLabelerContextGenerator(featureGenerator);
  }

  /**
   * Creates the {@link AdaptiveFeatureGenerator}. Usually this is a set of
   * generators contained in the {@link AggregatedFeatureGenerator}.
   *
   * Note: The generators are created on every call to this method.
   *
   * @return the feature generator or null if there is no descriptor in the
   *         model
   */
  public AdaptiveFeatureGenerator createFeatureGenerators() {

    if (this.featureGeneratorBytes == null && this.artifactProvider != null) {
      this.featureGeneratorBytes = (byte[]) this.artifactProvider
          .getArtifact(SequenceLabelerModel.GENERATOR_DESCRIPTOR_ENTRY_NAME);
    }
    if (this.featureGeneratorBytes == null) {
      System.err.println(
          "WARNING: loading the default feature generator descriptor!!");
      this.featureGeneratorBytes = loadDefaultFeatureGeneratorBytes();
    }

    final InputStream descriptorIn = new ByteArrayInputStream(
        this.featureGeneratorBytes);
    AdaptiveFeatureGenerator generator = null;
    try {
      generator = GeneratorFactory.create(descriptorIn,
          new FeatureGeneratorResourceProvider() {

            @Override
            public Object getResource(final String key) {
              if (SequenceLabelerFactory.this.artifactProvider != null) {
                return SequenceLabelerFactory.this.artifactProvider
                    .getArtifact(key);
              } else {
                return SequenceLabelerFactory.this.resources.get(key);
              }
            }
          });
    } catch (final InvalidFormatException e) {
      // It is assumed that the creation of the feature generation does not
      // fail after it succeeded once during model loading.

      // But it might still be possible that such an exception is thrown,
      // in this case the caller should not be forced to handle the exception
      // and a Runtime Exception is thrown instead.

      // If the re-creation of the feature generation fails it is assumed
      // that this can only be caused by a programming mistake and therefore
      // throwing a Runtime Exception is reasonable

      throw new SequenceLabelerModel.FeatureGeneratorCreationError(e);
    } catch (final IOException e) {
      throw new IllegalStateException(
          "Reading from mem cannot result in an I/O error", e);
    }

    return generator;
  }

  @SuppressWarnings("unchecked")
  public static SequenceLabelerCodec<String> instantiateSequenceCodec(
      final String sequenceCodecImplName) {

    if (sequenceCodecImplName != null) {
      return ExtensionLoader.instantiateExtension(SequenceLabelerCodec.class,
          sequenceCodecImplName);
    } else {
      // If nothing is specified return old default!
      return new BioCodec();
    }
  }
}
