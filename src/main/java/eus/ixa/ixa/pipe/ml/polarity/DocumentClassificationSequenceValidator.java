package eus.ixa.ixa.pipe.ml.polarity;

import opennlp.tools.util.SequenceValidator;

public class DocumentClassificationSequenceValidator implements SequenceValidator<String> {
	
	@Override
	public boolean validSequence(final int i, final String[] inputSequence,
	      final String[] outcomesSequence, final String outcome) {

	    return true;
	  
	}

}
