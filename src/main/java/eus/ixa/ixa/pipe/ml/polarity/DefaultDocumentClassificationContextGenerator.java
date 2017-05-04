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

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.CachedFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

public class DefaultDocumentClassificationContextGenerator 
	implements DocumentClassificationContextGenerator{
	
	  private AdaptiveFeatureGenerator[] featureGenerators;
	  
	  
	  @Deprecated
	  private static AdaptiveFeatureGenerator windowFeatures = new CachedFeatureGenerator(
	      new AdaptiveFeatureGenerator[] {
	          new WindowFeatureGenerator(new TokenFeatureGenerator(), 5, 5)});
	  /**
	   * Creates a name context generator with the specified cache size.
	   *
	   * @param featureGenerators
	   *          the array of feature generators
	   */
	  public DefaultDocumentClassificationContextGenerator(
	      final AdaptiveFeatureGenerator... featureGenerators) {

	    if (featureGenerators != null) {
	      this.featureGenerators = featureGenerators;
	    } else {
	      // use defaults
	      this.featureGenerators = new AdaptiveFeatureGenerator[] { windowFeatures,
	          new PreviousMapFeatureGenerator() };
	    }
	  }
	  
	  @Override
	  public void addFeatureGenerator(final AdaptiveFeatureGenerator generator) {
	    final AdaptiveFeatureGenerator generators[] = this.featureGenerators;

	    this.featureGenerators = new AdaptiveFeatureGenerator[this.featureGenerators.length
	        + 1];

	    System.arraycopy(generators, 0, this.featureGenerators, 0,
	        generators.length);

	    this.featureGenerators[this.featureGenerators.length - 1] = generator;
	  }

	  @Override
	  public void clearAdaptiveData() {
	    for (final AdaptiveFeatureGenerator featureGenerator : this.featureGenerators) {
	      featureGenerator.clearAdaptiveData();
	    }
	  }
	  
	  @Override
	  public void updateAdaptiveData(final String[] tokens,
	      final String[] outcomes) {

	    if (tokens != null && outcomes != null
	        && tokens.length != outcomes.length) {
	      throw new IllegalArgumentException(
	          "The tokens and outcome arrays MUST have the same size!");
	    }

	    for (final AdaptiveFeatureGenerator featureGenerator : this.featureGenerators) {
	      featureGenerator.updateAdaptiveData(tokens, outcomes);
	    }
	  }
	  
	  @Override
	  public String[] getContext(final int index, final String[] tokens,
	      final String[] preds, final Object[] additionalContext) {
	    final List<String> features = new ArrayList<String>();

	    for (final AdaptiveFeatureGenerator featureGenerator : this.featureGenerators) {
	    		featureGenerator.createFeatures(features, tokens, index, preds);
	    }

	    return features.toArray(new String[features.size()]);
	  }
}
