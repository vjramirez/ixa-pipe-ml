/*
 *  Copyright 2016 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package eus.ixa.ixa.pipe.ml.document.features;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.resources.Dictionary;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * Checks if a word is in a gazetteer.
 * 
 * @author ragerri
 * @version 2015-03-30
 *
 */
public class FrequentWordFeatureGenerator extends DocumentCustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private Dictionary dictionary;
  private Map<String, String> attributes;

  public FrequentWordFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {
    
    for (String token : tokens) {
      String gazEntry = this.dictionary.lookup(token);
      if (gazEntry != null) {
        features.add(this.attributes.get("dict") + "=" + token);
      }
    }
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    final Object dictResource = resourceProvider
        .getResource(properties.get("dict"));
    if (!(dictResource instanceof Dictionary)) {
      throw new InvalidFormatException(
          "Not a Dictionary resource for key: " + properties.get("dict"));
    }
    this.dictionary = (Dictionary) dictResource;
    this.attributes = properties;
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("frequentdictionaryserializer", new Dictionary.DictionarySerializer());
    return Collections.unmodifiableMap(mapping);
  }
}
