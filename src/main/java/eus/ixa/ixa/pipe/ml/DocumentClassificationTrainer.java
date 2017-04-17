/*
 *  Copyright 2017 Victor Ramirez

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

package eus.ixa.ixa.pipe.ml;

import java.io.IOException;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;

/**
 * Trainer based on Apache OpenNLP Machine Learning API. This class creates a
 * feature set based on the features activated in the trainParams.properties
 * file
 * 
 * @author vramirez
 * @version 2017-04-17
 */


public class DocumentClassificationTrainer {

	  /**
	   * The language.
	   */
	  private final String lang;
	  /**
	   * String holding the training data.
	   */
	  private final String trainData;
	  /**
	   * ObjectStream of the training data.
	   */
	  private ObjectStream<DocumentSample> trainSamples;
	  /**
	   * features needs to be implemented by any class extending this one.
	   */
	  private DoccatFactory newFactory;
	  
	  /**
	   * Construct a trainer with training and test data, and with options for
	   * language, beamsize for decoding, sequence codec and corpus format (conll or
	   * opennlp).
	   * 
	   * @param params
	   *          the training parameters
	   * @throws IOException
	   *           io exception
	   */
	  public DocumentClassificationTrainer(final TrainingParameters params)
		      throws IOException {

		    this.lang = Flags.getLanguage(params);
		    this.trainData = params.getSettings().get("TrainSet");
		    this.trainSamples = getDocumentStream(this.trainData);
		    createDocumentClassificationFactory(params);
		  }
	  
	  /**
	   * Create {@code createDocumentClassificationFactory} .
	   *
	   * @param params
	   *          the parameter training file
	   * @throws IOException
	   *           if io error
	   */
	  public void createDocumentClassificationFactory(final TrainingParameters params)
	      throws IOException {
	    newFactory = new DoccatFactory();
	  }
	  
	  public final DoccatModel train(final TrainingParameters params) {
		    if (getDocumentClassificationFactory() == null) {
		      throw new IllegalStateException(
		          "The DocumentClassificationFactory must be instantiated!!");
		    }
		    DoccatModel trainedModel = null;
		    try {
		    	trainedModel = DocumentCategorizerME.train(this.lang, trainSamples, params, newFactory);
		    } catch (final IOException e) {
		      System.err.println("IO error while loading traing and test sets!");
		      e.printStackTrace();
		      System.exit(1);
		    }
		    return trainedModel;
	  }
	  /**
	   * Getting the stream with the right corpus format.
	   * 
	   * @param inputData
	   *          the input data
	   * @param clearFeatures
	   *          clear the features
	   * @param aCorpusFormat
	   *          the corpus format
	   * @return the stream from the several corpus formats
	   * @throws IOException
	   *           the io exception
	   */
	  public static ObjectStream<DocumentSample> getDocumentStream(
	      final String inputData) throws IOException {
	      final ObjectStream<String> nameStream = IOUtils
	          .readFileIntoMarkableStreamFactory(inputData);
	      ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(nameStream);
	    return sampleStream;
	  }
	  
	  /**
	   * Get the features which are implemented in each of the trainers extending
	   * this class.
	   * 
	   * @return the features
	   */
	  public final DoccatFactory getDocumentClassificationFactory() {
	    return this.newFactory;
	  }

}
