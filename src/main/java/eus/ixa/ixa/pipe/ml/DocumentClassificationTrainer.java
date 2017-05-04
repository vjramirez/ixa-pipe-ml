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
import java.nio.charset.Charset;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.features.XMLFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.polarity.DocumentClassificationEvaluator;
import eus.ixa.ixa.pipe.ml.polarity.DocumentClassificationFactory;
import eus.ixa.ixa.pipe.ml.polarity.DocumentClassificationME;
import eus.ixa.ixa.pipe.ml.polarity.DocumentClassificationModel;
import eus.ixa.ixa.pipe.ml.resources.LoadModelResources;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
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
	   * String holding the testData.
	   */
	  private final String testData;
	  /**
	   * Reset the adaptive features every newline in the training data.
	   */
	  private final String clearTrainingFeatures;
	  /**
	   * Reset the adaptive features every newline in the testing data.
	   */
	  private final String clearEvaluationFeatures;
	  /**
	   * ObjectStream of the training data.
	   */
	  private ObjectStream<DocumentSample> trainSamples;
	  /**
	   * ObjectStream of the test data.
	   */
	  private ObjectStream<DocumentSample> testSamples;
	  /**
	   * features needs to be implemented by any class extending this one.
	   */
	  private DocumentClassificationFactory newFactory;
	  
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
		    this.clearTrainingFeatures = Flags.getClearTrainingFeatures(params);
		    this.clearEvaluationFeatures = Flags.getClearEvaluationFeatures(params);
		    this.trainData = params.getSettings().get("TrainSet");
		    this.testData = params.getSettings().get("TestSet");
		    this.trainSamples = getDocumentStream(this.trainData);
		    this.testSamples = getDocumentStream(this.testData);
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
		  final String featureDescription = XMLFeatureDescriptor
			        .createXMLFeatureDescriptor(params);
		  System.err.println(featureDescription);
		  byte[] featureGeneratorBytes = featureDescription
			        .getBytes(Charset.forName("UTF-8"));
		  //if there are not feature generators in properties
		  if (featureDescription.contains("<generators />")) {
			  featureGeneratorBytes = null;
		  }
		  final Map<String, Object> resources = LoadModelResources
			        .loadSequenceResources(params);
		  setDocumentClassificationFactory(
				  DocumentClassificationFactory.create(DocumentClassificationFactory.class.getName(),
			            featureGeneratorBytes, resources));
	  }
	  
	  public final DocumentClassificationModel train(final TrainingParameters params) {
		    if (getDocumentClassificationFactory() == null) {
		      throw new IllegalStateException(
		          "The DocumentClassificationFactory must be instantiated!!");
		    }
		    DocumentClassificationModel trainedModel = null;
		    try {
		    	trainedModel = DocumentClassificationME.train(this.lang, this.trainSamples, params, this.newFactory);
		    	final DocumentClassificationME docClassification = new DocumentClassificationME(trainedModel);
		    	trainingEvaluate(docClassification);
		    } catch (final IOException e) {
		      System.err.println("IO error while loading traing and test sets!");
		      e.printStackTrace();
		      System.exit(1);
		    }
		    return trainedModel;
	  }
	  
	  private void trainingEvaluate(final DocumentClassificationME DocumentClassification) {
		      final DocumentClassificationEvaluator evaluator = new DocumentClassificationEvaluator(DocumentClassification);
		      try {
		        evaluator.evaluate(this.testSamples);
		      } catch (final IOException e) {
		        e.printStackTrace();
		      }
		      System.out.println();
		      System.out.println("Document Count: " + evaluator.getDocumentCount());
		      System.out.println("Accuracy: " + evaluator.getAccuracy());
		    
		  }
	  
	  /**
	   * Getting the stream with the right corpus format.
	   * 
	   * @param inputData
	   *          the input data
	   * @return the stream from the documents
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
	  public final DocumentClassificationFactory getDocumentClassificationFactory() {
	    return this.newFactory;
	  }
	  
	  public final DocumentClassificationFactory setDocumentClassificationFactory(
	      final DocumentClassificationFactory tokenNameFinderFactory) {
	    this.newFactory = tokenNameFinderFactory;
	    return this.newFactory;
	  }

}
