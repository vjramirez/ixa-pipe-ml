package eus.ixa.ixa.pipe.ml.polarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.TrainerFactory.TrainerType;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ml.model.SequenceClassificationModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.SequenceValidator;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.featuregen.AdditionalContextFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;


public class DocumentClassificationME implements DocumentClassification {

	public static final int DEFAULT_BEAM_SIZE = 3;
	private static String[][] EMPTY = new String[0][0];
	protected SequenceClassificationModel<String> model;
	protected DocumentClassificationContextGenerator mContextGenerator;
	private final AdditionalContextFeatureGenerator additionalContextFeatureGenerator = new AdditionalContextFeatureGenerator();
	private Sequence bestSequence;
	private final SequenceValidator<String> sequenceValidator;
	
	
	/**
	   * Initializes the current instance with a doccat model. Default feature
	   * generation is used.
	   *
	   * @param model the DocumentClassification model
	   */
	public DocumentClassificationME(DocumentClassificationModel model) {
		this.model = model.getDocumentClassificationModel();
	    /*this.mContextGenerator = new DocumentClassificationContextGenerator(this.model
	        .getFactory().getFeatureGenerators());*/
	    
	    final DocumentClassificationFactory factory = model.getFactory();
	    
	    this.sequenceValidator = new DocumentClassificationSequenceValidator();
	    
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
	   * @param additionalContext additional Context
	   */
	@Override
	public String categorize(String[] text, final String[][] additionalContext) {
		
		this.additionalContextFeatureGenerator.setCurrentContext(additionalContext);
	    this.bestSequence = this.model.bestSequence(text, additionalContext,
	        this.mContextGenerator, this.sequenceValidator);
	    final List<String> c = this.bestSequence.getOutcomes();
	    final double[] d = this.bestSequence.getProbs();
	    
	    this.mContextGenerator.updateAdaptiveData(text,
		        c.toArray(new String[c.size()]));
	    
	    
	    
	    //System.err.println("Cantidad Outcomes: " + c.size());
	    //System.err.println("Cantidad Probs: " + d.length);
	    
	    Map<String, Double> OutcomesMap = new HashMap<String, Double>();
	    
	    for (int i = 0; i<c.size(); i++) {
	    	//System.err.println("Outcome"+i+": "+c.get(i)+" ... Prob"+i+": "+d[i]);
	    	if (OutcomesMap.containsKey(c.get(i))) {
	    		OutcomesMap.put(c.get(i), OutcomesMap.get(c.get(i)) + 1000 + d[i]);
	    	}
	    	else {
	    		OutcomesMap.put(c.get(i), 1000 + d[i]);
	    	}
	    }
	    
	    //System.err.println("Score: " + this.bestSequence.getScore());
	    
	    
	    Map.Entry<String, Double> tmpEntry = null;
	    double maxValue = 0;
	    for (Map.Entry<String, Double> entry : OutcomesMap.entrySet())
	    {
	    	if (entry.getValue() > maxValue) {
	    		tmpEntry = entry;
	    		maxValue = entry.getValue();
	    	}
	    }
	    
	    //System.err.println("Resultado Final: " + tmpEntry.getKey() + " ... " + tmpEntry.getValue());
	    
	    return tmpEntry.getKey();
		
		//String[] features = null;
		/*
	    List<String> listString = new ArrayList<String>();
		
		
		
		for (int i = 0; i < text.length; i++) {
			String[] featuresTemp = mContextGenerator.getContext(i, text, null, null);
			listString.addAll(Arrays.asList(featuresTemp));
			//features = ArrayUtils.addAll();
		}
		
	    return model.getMaxentModel().eval(listString.toArray(new String[0]));
	    */
	}
	  
	  /**
	   * Categorizes the given text.
	   *
	   * @param text the text to categorize
	   */
	@Override
	public String categorize(String[] text) {
	    return this.categorize(text, EMPTY);
	}
	
	/**
	   * Returns a map in which the key is the category name and the value is the score
	   *
	   * @param text the input text to classify
	   * @return the score map
	   
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
	}*/
	
	/**
	   * Returns a map with the score as a key in ascending order.
	   * The value is a Set of categories with the score.
	   * Many categories can have the same score, hence the Set as value
	   *
	   * @param text the input text to classify
	   * @return the sorted score map
	   
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
	}*/
	/*
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
	*/
	  /**
	   * Forgets all adaptive data which was collected during previous calls to one
	   * of the find methods.
	   *
	   * This method is typical called at the end of a document.
	   */
	  @Override
	  public void clearAdaptiveData() {
	    this.mContextGenerator.clearAdaptiveData();
	  }
	
	public static DocumentClassificationModel train(String languageCode, ObjectStream<DocumentClassificationSample> samples,
			TrainingParameters mlParams, DocumentClassificationFactory factory)
		          throws IOException {
		
		final String beamSizeString = mlParams.getSettings()
		        .get(BeamSearch.BEAM_SIZE_PARAMETER);

		    int beamSize = DocumentClassificationME.DEFAULT_BEAM_SIZE;
		    if (beamSizeString != null) {
		      beamSize = Integer.parseInt(beamSizeString);
		    }

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
		
		return new DocumentClassificationModel(languageCode, nameFinderModel, beamSize,
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
