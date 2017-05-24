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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.model.AbstractModel;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ml.model.SequenceClassificationModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.ModelUtil;

/**
 * A model for document categorization
 */
public class DocumentClassificationModel extends BaseModel {
	
	public static class FeatureGeneratorCreationError extends RuntimeException {
	    /**
	     * Default serial version.
	     */
		private static final long serialVersionUID = 1L;

		FeatureGeneratorCreationError(final Throwable t) {
			super(t);
		}
	}
	
	private static class ByteArraySerializer 
		implements ArtifactSerializer<byte[]> {

		@Override
		public byte[] create(final InputStream in)
				throws IOException, InvalidFormatException {

			return ModelUtil.read(in);
		}

		@Override
		public void serialize(final byte[] artifact, final OutputStream out)
				throws IOException {
			out.write(artifact);
		}
	}
	
	private static final String COMPONENT_NAME = "DocumentClassificationME";
	//private static final String DOCCAT_MODEL_ENTRY_NAME = "doccat.model";
	static final String GENERATOR_DESCRIPTOR_ENTRY_NAME = "generator.featuregen";
	private static final String MAXENT_MODEL_ENTRY_NAME = "DocumentClassification.model";

	public DocumentClassificationModel(String languageCode, MaxentModel doccatModel,
			Map<String, String> manifestInfoEntries, DocumentClassificationFactory factory) {
		super(COMPONENT_NAME, languageCode, manifestInfoEntries, factory);
		
		artifactMap.put(MAXENT_MODEL_ENTRY_NAME, doccatModel);
		checkArtifactMap();
	}
	
	 public DocumentClassificationModel(final String languageCode,
		      final MaxentModel nameFinderModel,
		      final int beamSize,
		      final byte[] generatorDescriptor, final Map<String, Object> resources,
		      final Map<String, String> manifestInfoEntries,
		      final DocumentClassificationFactory factory) {
		    super(COMPONENT_NAME, languageCode, manifestInfoEntries, factory);

		    final Properties manifest = (Properties) this.artifactMap.get(MANIFEST_ENTRY);
		    manifest.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));

		    init(nameFinderModel, generatorDescriptor, resources, manifestInfoEntries);

		    /*if (!isModelValid(nameFinderModel)) {
		      throw new IllegalArgumentException(
		          "Model not compatible with name finder!");
		    }*/
		  }
	
	 
	  private void init(final Object nameFinderModel,
		      final byte[] generatorDescriptor, final Map<String, Object> resources,
		      final Map<String, String> manifestInfoEntries) {

		    //final Properties manifest = (Properties) this.artifactMap.get(MANIFEST_ENTRY);

		    this.artifactMap.put(MAXENT_MODEL_ENTRY_NAME, nameFinderModel);

		    if (generatorDescriptor != null && generatorDescriptor.length > 0) {
		      this.artifactMap.put(GENERATOR_DESCRIPTOR_ENTRY_NAME,
		          generatorDescriptor);
		    }

		    if (resources != null) {
		      // The resource map must not contain key which are already taken
		      // like the name finder maxent model name
		      if (resources.containsKey(MAXENT_MODEL_ENTRY_NAME)
		          || resources.containsKey(GENERATOR_DESCRIPTOR_ENTRY_NAME)) {
		        throw new IllegalArgumentException();
		      }

		      // TODO: Add checks to not put resources where no serializer exists,
		      // make that case fail here, should be done in the BaseModel
		      this.artifactMap.putAll(resources);
		    }
		    checkArtifactMap();
		  }
	  
	@SuppressWarnings({ "unchecked" })
	public SequenceClassificationModel<String> getDocumentClassificationModel() {

	    final Properties manifest = (Properties) this.artifactMap
	        .get(MANIFEST_ENTRY);

	    if (this.artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel) {
	      final String beamSizeString = manifest
	          .getProperty(BeamSearch.BEAM_SIZE_PARAMETER);

	      int beamSize = DocumentClassificationME.DEFAULT_BEAM_SIZE;
	      if (beamSizeString != null) {
	        beamSize = Integer.parseInt(beamSizeString);
	      }

	      return new BeamSearch<>(beamSize,
	          (MaxentModel) this.artifactMap.get(MAXENT_MODEL_ENTRY_NAME));
	    } else if (this.artifactMap
	        .get(MAXENT_MODEL_ENTRY_NAME) instanceof DocumentClassificationModel) {
	      return (SequenceClassificationModel<String>) this.artifactMap
	          .get(MAXENT_MODEL_ENTRY_NAME);
	    } else {
	      return null;
	    }
	}
	
	public DocumentClassificationModel(InputStream in) throws IOException {
		super(COMPONENT_NAME, in);
	}

	public DocumentClassificationModel(File modelFile) throws IOException {
		super(COMPONENT_NAME, modelFile);
	}

	public DocumentClassificationModel(URL modelURL) throws IOException {
		super(COMPONENT_NAME, modelURL);
	}
	  
	public MaxentModel getMaxentModel() {
		return (MaxentModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME);
	}
	
	public DocumentClassificationFactory getFactory() {
		return (DocumentClassificationFactory) this.toolFactory;
	}
	
	  @Override
	  protected void validateArtifactMap() throws InvalidFormatException {
	    super.validateArtifactMap();

	    if (!(artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof AbstractModel)) {
	      throw new InvalidFormatException("Documentclassification model is incomplete!");
	    }
	  }
	
	  @Override
	  protected void createArtifactSerializers(
	      @SuppressWarnings("rawtypes") final Map<String, ArtifactSerializer> serializers) {
	    super.createArtifactSerializers(serializers);

	    serializers.put("featuregen", new ByteArraySerializer());
	  }
	
	  /**
	   * Create the artifact serializers. The DefaultTrainer deals with any other
	   * Custom serializers.
	   * 
	   * @return the map containing the added serializers
	   */
	  @SuppressWarnings("rawtypes")
	  public static Map<String, ArtifactSerializer> createArtifactSerializers() {
	    final Map<String, ArtifactSerializer> serializers = BaseModel
	        .createArtifactSerializers();

	    serializers.put("featuregen", new ByteArraySerializer());
	    return serializers;
	  }
}
