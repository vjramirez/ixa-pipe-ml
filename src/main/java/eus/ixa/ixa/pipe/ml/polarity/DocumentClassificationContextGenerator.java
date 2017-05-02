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

import opennlp.tools.util.BeamSearchContextGenerator;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;

/*
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.util.BeamSearchContextGenerator;
*/

public interface DocumentClassificationContextGenerator
extends BeamSearchContextGenerator<String> {
	  /**
	   * Adds a feature generator to this set of feature generators.
	   *
	   * @param generator
	   *          The feature generator to add.
	   */
	  public void addFeatureGenerator(AdaptiveFeatureGenerator generator);

	  /**
	   * Informs all the feature generators for a sequence labeler that the
	   * specified tokens have been classified with the corresponding set of
	   * specified outcomes.
	   *
	   * @param tokens
	   *          The tokens of the sentence or other text unit which has been
	   *          processed.
	   * @param outcomes
	   *          The outcomes associated with the specified tokens.
	   */
	  public void updateAdaptiveData(String[] tokens, String[] outcomes);

	  /**
	   * Informs all the feature generators for a name finder that the context of
	   * the adaptive data (typically a document) is no longer valid.
	   */
	  public void clearAdaptiveData();
}


/**
 * Context generator for document classification
 */
/*
class DocumentClassificationContextGenerator {
	private FeatureGenerator[] mFeatureGenerators;

	  DocumentClassificationContextGenerator(FeatureGenerator... featureGenerators) {
	    mFeatureGenerators = featureGenerators;
	  }

	  public String[] getContext(String[] text, Map<String, Object> extraInformation) {

	    Collection<String> context = new LinkedList<>();

	    for (FeatureGenerator mFeatureGenerator : mFeatureGenerators) {
	      Collection<String> extractedFeatures =
	          mFeatureGenerator.extractFeatures(text, extraInformation);
	      context.addAll(extractedFeatures);
	    }

	    return context.toArray(new String[context.size()]);
	  }
}
*/