/*
 * Copyright 2014 Rodrigo Agerri

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

import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.ngram.NGramModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.StringList;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

/**
 * The {@link DocCharacterNgramFeatureGenerator} uses character ngrams to generate
 * features about each token. The minimum and maximum length can be specified.
 */
public class DocCharacterNgramFeatureGenerator extends DocumentCustomFeatureGenerator {

  private Map<String, String> attributes;

  /**
   * Initializes the current instance.
   */
  public DocCharacterNgramFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {
    
    for (String token : tokens) {
      final NGramModel model = new NGramModel();
      model.add(token, Integer.parseInt(this.attributes.get("minLength")),
          Integer.parseInt(this.attributes.get("maxLength")));
      for (final StringList tokenList : model) {
        if (tokenList.size() > 0) {
          features.add("ng=" + tokenList.getToken(0).toLowerCase());
          if (Flags.DEBUG) {
            System.err.println("-> " + tokenList.getToken(0).toLowerCase()
                + ": ng=" + tokenList.getToken(0).toLowerCase());
          }
        }
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
    this.attributes = properties;

  }
}
