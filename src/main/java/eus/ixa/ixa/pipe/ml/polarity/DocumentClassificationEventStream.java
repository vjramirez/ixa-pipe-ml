package eus.ixa.ixa.pipe.ml.polarity;

import java.util.Iterator;

import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.ml.model.Event;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.featuregen.AdditionalContextFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

public class DocumentClassificationEventStream 
	extends opennlp.tools.util.AbstractEventStream<DocumentSample> {

	private final DocumentClassificationContextGenerator contextGenerator;
	private final AdditionalContextFeatureGenerator additionalContextFeatureGenerator = new AdditionalContextFeatureGenerator();
	
	  /**
	   * Creates a new name finder event stream using the specified data stream and
	   * context generator.
	   * 
	   * @param dataStream
	   *          The data stream of events.
	   * @param contextGenerator
	   *          The context generator used to generate features for the event
	   *          stream.
	   * @param codec
	   *          the encoding
	   */
	  public DocumentClassificationEventStream(
	      final ObjectStream<DocumentSample> dataStream,
	      final DocumentClassificationContextGenerator contextGenerator) {
	    super(dataStream);
	    
	    this.contextGenerator = contextGenerator;
	    this.contextGenerator.addFeatureGenerator(new WindowFeatureGenerator(
	        this.additionalContextFeatureGenerator, 8, 8));
	  }

	  public DocumentClassificationEventStream(
	      final ObjectStream<DocumentSample> dataStream) {
	    this(dataStream, new DefaultDocumentClassificationContextGenerator());
	  }
	  /*
	  public static List<Event> generateEvents(final String[] sentence,
		      final String[] outcomes, final DocumentClassificationContextGenerator cg) {
		    final List<Event> events = new ArrayList<Event>(outcomes.length);
		    for (int i = 0; i < outcomes.length; i++) {
		      events.add(
		          new Event(outcomes[i], cg.getContext(i, sentence, outcomes, null)));
		    }
		    cg.updateAdaptiveData(sentence, outcomes);
		    return events;
		  }
	  
	  @Override
	  protected Iterator<Event> createEvents(final DocumentSample sample) {

	    if (sample.isClearAdaptiveDataSet()) {
	      this.contextGenerator.clearAdaptiveData();
	    }
	    final String outcomes[] = this.codec.encode(sample.getSequences(),
	        sample.getTokens().length);
	    this.additionalContextFeatureGenerator
	        .setCurrentContext(sample.getAdditionalContext());
	    final String[] tokens = new String[sample.getTokens().length];

	    for (int i = 0; i < sample.getTokens().length; i++) {
	      tokens[i] = sample.getTokens()[i];
	    }

	    return generateEvents(tokens, outcomes, this.contextGenerator).iterator();
	  }*/
	  
	  @Override
	  protected Iterator<Event> createEvents(final DocumentSample sample) {

	    return new Iterator<Event>(){

	      private boolean isVirgin = true;

	      public boolean hasNext() {
	        return isVirgin;
	      }

	      public Event next() {

	        isVirgin = false;

	        return new Event(sample.getCategory(),
	            contextGenerator.getContext(0, sample.getText(), null, null));
	      }

	      public void remove() {
	        throw new UnsupportedOperationException();
	      }};
	  }
}
