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

package eus.ixa.ixa.pipe.ml.polarity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ext.ExtensionLoader;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.AggregatedFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.featuregen.GeneratorFactory;

/**
 * The factory that provides DocumentClassification default implementations and resources
 */
public class DocumentClassificationFactory extends BaseToolFactory {
	
	private byte[] featureGeneratorBytes;
	private Map<String, Object> resources;
	private FeatureGenerator[] featureGenerators;
	
	private static final String FEATURE_GENERATORS = "doccat.featureGenerators";

	  /**
	   * Creates a {@link DocumentClassificationFactory} that provides the default implementation of
	   * the resources.
	   */
	public DocumentClassificationFactory() {}
	
	void init(final byte[] featureGeneratorBytes,
			final Map<String, Object> resources) {
		this.featureGeneratorBytes = featureGeneratorBytes;
		this.resources = resources;
	}
	
	  private static byte[] loadDefaultFeatureGeneratorBytes() {

		    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		    try (InputStream in = DocumentClassificationFactory.class.getResourceAsStream(
		        "/documentClassification/default-feature-descriptor.xml")) {

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
	  
	protected Map<String, Object> getResources() {
		return this.resources;
	}

	protected byte[] getFeatureGenerator() {
		return this.featureGeneratorBytes;
	}
	
	public static DocumentClassificationFactory create(final String subclassName,
		      final byte[] featureGeneratorBytes, final Map<String, Object> resources) 
		    		  throws InvalidFormatException {
		DocumentClassificationFactory theFactory;
		if (subclassName == null) {
			// will create the default factory
			theFactory = new DocumentClassificationFactory();
			} else {
				try {
					theFactory = ExtensionLoader.instantiateExtension(DocumentClassificationFactory.class, subclassName);
				} catch (final Exception e) {
					final String msg = "Could not instantiate the " + subclassName
		            + ". The initialization throw an exception.";
					System.err.println(msg);
					e.printStackTrace();
					throw new InvalidFormatException(msg, e);
				}
			}
		theFactory.init(featureGeneratorBytes, resources);
		return theFactory;
	}
	
	
	@Override
	public void validateArtifactMap() throws InvalidFormatException {
	// no additional artifacts
	}

	public DocumentClassificationContextGenerator createContextGenerator() {

		final AdaptiveFeatureGenerator featureGenerator = createFeatureGenerators();
		if (featureGenerator == null) {
			throw new NullPointerException("featureGenerator must not be null");
		}

		return new DefaultDocumentClassificationContextGenerator(featureGenerator);
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
	          .getArtifact(DocumentClassificationModel.GENERATOR_DESCRIPTOR_ENTRY_NAME);
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
	              if (DocumentClassificationFactory.this.artifactProvider != null) {
	                return DocumentClassificationFactory.this.artifactProvider
	                    .getArtifact(key);
	              } else {
	                return DocumentClassificationFactory.this.resources.get(key);
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

	      throw new DocumentClassificationModel.FeatureGeneratorCreationError(e);
	    } catch (final IOException e) {
	      throw new IllegalStateException(
	          "Reading from mem cannot result in an I/O error", e);
	    }

	    return generator;
	  }
	
	private FeatureGenerator[] loadFeatureGenerators(String classNames) {
	    String[] classes = classNames.split(",");
	    FeatureGenerator[] fgs = new FeatureGenerator[classes.length];

	    for (int i = 0; i < classes.length; i++) {
	      fgs[i] = ExtensionLoader.instantiateExtension(FeatureGenerator.class,
	          classes[i]);
	    }
	    return fgs;
	}
	
	public FeatureGenerator[] getFeatureGenerators() {
	    if (featureGenerators == null) {
	      if (artifactProvider != null) {
	        String classNames = artifactProvider
	            .getManifestProperty(FEATURE_GENERATORS);
	        if (classNames != null) {
	          this.featureGenerators = loadFeatureGenerators(classNames);
	        }
	      }
	      if (featureGenerators == null) { // could not load using artifact provider
	        // load bag of words as default
	        this.featureGenerators = new FeatureGenerator[]{new BagOfWordsFeatureGenerator()};
	      }
	    }
	    return featureGenerators;
	}
}
