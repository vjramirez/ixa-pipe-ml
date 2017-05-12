package eus.ixa.ixa.pipe.ml.polarity;

import java.io.IOException;


import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;

public class DocumentClassificationSampleStream extends FilterObjectStream<String, DocumentClassificationSample>  {
	
	  /**
	   * Whether the adaptive features are to be reset or not.
	   */
	  private final String clearFeatures;

	public DocumentClassificationSampleStream(final String clearFeatures, ObjectStream<String> samples) {
	    super(samples);
	    this.clearFeatures = clearFeatures;
	  }

	  public DocumentClassificationSample read() throws IOException {
	    String sampleString = samples.read();
	    
	    boolean isClearAdaptiveData = false;

	    if (sampleString != null) {

	      // Whitespace tokenize entire string
	      String tokens[] = WhitespaceTokenizer.INSTANCE.tokenize(sampleString);

	      DocumentClassificationSample sample;
	      
	      // check if we need to clear features every sentence
	      if (this.clearFeatures.equalsIgnoreCase("yes")) {
	        isClearAdaptiveData = true;
	      }
	      if (this.clearFeatures.equalsIgnoreCase("docstart")
	              && sampleString.startsWith("-DOCSTART-")) {
	            isClearAdaptiveData = true;
	      }

	      if (tokens.length > 1) {
	        String category = tokens[0];
	        String docTokens[] = new String[tokens.length - 1];
	        System.arraycopy(tokens, 1, docTokens, 0, tokens.length -1);

	        sample = new DocumentClassificationSample(category, docTokens,isClearAdaptiveData);
	      }
	      else {
	        throw new IOException("Empty lines, or lines with only a category string are not allowed!");
	      }

	      return sample;
	    }
	    else {
	      return null;
	    }
	  }
}
