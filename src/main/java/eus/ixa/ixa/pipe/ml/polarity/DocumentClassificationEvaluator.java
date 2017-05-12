package eus.ixa.ixa.pipe.ml.polarity;

import opennlp.tools.doccat.DocumentCategorizer;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.util.eval.Evaluator;
import opennlp.tools.util.eval.Mean;

public class DocumentClassificationEvaluator extends Evaluator<DocumentClassificationSample>{
	private DocumentClassification categorizer;

	private Mean accuracy = new Mean();
	  
	  /**
	   * Initializes the current instance.
	   *
	   * @param categorizer the document categorizer instance
	   */
	public DocumentClassificationEvaluator(DocumentClassification categorizer,
	      DocumentClassificationEvaluationMonitor ... listeners) {
		super(listeners);
	    this.categorizer = categorizer;
	}
	
	  /**
	   * Evaluates the given reference {@link DocumentClassificationSample} object.
	   *
	   * This is done by categorizing the document from the provided
	   * {@link DocumentClassificationSample}. The detected category is then used
	   * to calculate and update the score.
	   *
	   * @param sample the reference {@link TokenSample}.
	   */
	public DocumentClassificationSample processSample(DocumentClassificationSample sample) {

	    String[] document = sample.getText();

	    double[] probs = categorizer.categorize(document);

	    String cat = categorizer.getBestCategory(probs);

	    if (sample.getCategory().equals(cat)) {
	      accuracy.add(1);
	    }
	    else {
	      accuracy.add(0);
	    }

	    return new DocumentClassificationSample(cat, sample.getText(), sample.isClearAdaptiveDataSet());
	}
	
	  /**
	   * Retrieves the accuracy of provided {@link DocumentCategorizer}.
	   *
	   * accuracy = correctly categorized documents / total documents
	   *
	   * @return the accuracy
	   */
	public double getAccuracy() {
		return accuracy.mean();
	}

	public long getDocumentCount() {
		return accuracy.count();
	}

	  /**
	   * Represents this objects as human readable {@link String}.
	   */
	@Override
	public String toString() {
		return "Accuracy: " + accuracy.mean() + "\n" +
				"Number of documents: " + accuracy.count();
	}

}
