package eus.ixa.ixa.pipe.ml.polarity;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.TrainerFactory.TrainerType;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.featuregen.AdditionalContextFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;


public class DocumentClassificationME implements DocumentClassification {

	public static final int DEFAULT_BEAM_SIZE = 3;
	protected DocumentClassificationModel model;
	protected DocumentClassificationContextGenerator mContextGenerator;
	private final AdditionalContextFeatureGenerator additionalContextFeatureGenerator = new AdditionalContextFeatureGenerator();
	
	
	/**
	   * Initializes the current instance with a doccat model. Default feature
	   * generation is used.
	   *
	   * @param model the DocumentClassification model
	   */
	public DocumentClassificationME(DocumentClassificationModel model) {
		this.model = model;
	    /*this.mContextGenerator = new DocumentClassificationContextGenerator(this.model
	        .getFactory().getFeatureGenerators());*/
	    
	    final DocumentClassificationFactory factory = model.getFactory();
	    
	    //this.model = model.getDocumentClassificationModel();
	    this.mContextGenerator = factory.createContextGenerator();

	    // TODO: We should deprecate this. And come up with a better solution!
	    this.mContextGenerator.addFeatureGenerator(new WindowFeatureGenerator(
	        this.additionalContextFeatureGenerator, 8, 8));
	}

	  /**
	   * Categorize the given text provided as tokens along with
	   * the provided extra information
	   *
	   * @param text text tokens to categorize
	   * @param extraInformation additional information
	   */
	@Override
	public double[] categorize(String[] text, Map<String, Object> extraInformation) {
	    return model.getMaxentModel().eval(
	        mContextGenerator.getContext(0, text, null, null));
	}
	  
	  /**
	   * Categorizes the given text.
	   *
	   * @param text the text to categorize
	   */
	@Override
	public double[] categorize(String[] text) {
	    return this.categorize(text, Collections.emptyMap());
	}
	
	/**
	   * Returns a map in which the key is the category name and the value is the score
	   *
	   * @param text the input text to classify
	   * @return the score map
	   */
	@Override
	public Map<String, Double> scoreMap(String[] text) {
		Map<String, Double> probDist = new HashMap<>();

		double[] categorize = categorize(text);
		int catSize = getNumberOfCategories();
	    for (int i = 0; i < catSize; i++) {
	      String category = getCategory(i);
	      probDist.put(category, categorize[getIndex(category)]);
	    }
	    return probDist;
	}
	
	/**
	   * Returns a map with the score as a key in ascending order.
	   * The value is a Set of categories with the score.
	   * Many categories can have the same score, hence the Set as value
	   *
	   * @param text the input text to classify
	   * @return the sorted score map
	   */
	@Override
	public SortedMap<Double, Set<String>> sortedScoreMap(String[] text) {
		SortedMap<Double, Set<String>> descendingMap = new TreeMap<>();
	    double[] categorize = categorize(text);
	    int catSize = getNumberOfCategories();
	    for (int i = 0; i < catSize; i++) {
	      String category = getCategory(i);
	      double score = categorize[getIndex(category)];
	      if (descendingMap.containsKey(score)) {
	        descendingMap.get(score).add(category);
	      } else {
	        Set<String> newset = new HashSet<>();
	        newset.add(category);
	        descendingMap.put(score, newset);
	      }
	    }
	    return descendingMap;
	}
	
	public String getBestCategory(double[] outcome) {
	    return model.getMaxentModel().getBestOutcome(outcome);
	}
	
	public int getIndex(String category) {
		return model.getMaxentModel().getIndex(category);
	}
	
	public int getNumberOfCategories() {
	    return model.getMaxentModel().getNumOutcomes();
	}

	public String getAllResults(double[] results) {
		return model.getMaxentModel().getAllOutcomes(results);
	}
	
	public String getCategory(int index) {
		return model.getMaxentModel().getOutcome(index);
	}
	
	public static DocumentClassificationModel train(String languageCode, ObjectStream<DocumentSample> samples,
			TrainingParameters mlParams, DocumentClassificationFactory factory)
		          throws IOException {

		final Map<String, String> manifestInfoEntries = new HashMap<String, String>();
		
		MaxentModel nameFinderModel = null;
		
		final TrainerType trainerType = TrainerFactory
		        .getTrainerType(mlParams.getSettings());
		
		if (TrainerType.EVENT_MODEL_TRAINER.equals(trainerType)) {
			final ObjectStream<Event> eventStream = new DocumentClassificationEventStream(
					samples, factory.createContextGenerator());

			final EventTrainer trainer = TrainerFactory
					.getEventTrainer(mlParams.getSettings(), manifestInfoEntries);
			nameFinderModel = trainer.train(eventStream);
		} else {
		    throw new IllegalStateException("Unexpected trainer type!");
	    }
		
		return new DocumentClassificationModel(languageCode, nameFinderModel,
		          factory.getFeatureGenerator(), factory.getResources(),
		          manifestInfoEntries, factory);
		
		/*ANTIGUO
		EventTrainer trainer = TrainerFactory.getEventTrainer(
				mlParams.getSettings(), manifestInfoEntries);

		MaxentModel model = trainer.train(
				new DocumentCategorizerEventStream(samples, factory.getFeatureGenerators()));

		return new DocumentClassificationModel(languageCode, model, manifestInfoEntries, factory);
		*/
	}
}
