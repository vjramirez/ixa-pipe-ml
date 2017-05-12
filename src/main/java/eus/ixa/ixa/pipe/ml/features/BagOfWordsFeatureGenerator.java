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

package eus.ixa.ixa.pipe.ml.features;

import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.featuregen.StringPattern;

public class BagOfWordsFeatureGenerator 
	extends CustomFeatureGenerator {

  private Map<String, String> attributes;

  public BagOfWordsFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] preds) {

    for (String word : tokens) {
        if (this.attributes.get("useOnlyAllLetterTokens").equalsIgnoreCase("yes")) {
          StringPattern pattern = StringPattern.recognize(word);

          if (pattern.isAllLetter())
        	  features.add("bow=" + word);
          if (Flags.DEBUG) {
		        System.err.println("-> " + word + ": bow="
		            + word);
		      }
          
        }
        else {
        	features.add("bow=" + word);
        	if (Flags.DEBUG) {
		        System.err.println("-> " + word + ": bow="
		            + word);
		      }
        } 
      }
  }

  @Override
  public void updateAdaptiveData(final String[] tokens,
      final String[] outcomes) {
  }

  @Override
  public void clearAdaptiveData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    this.attributes = properties;

  }

}

