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

package eus.ixa.ixa.pipe.ml.polarity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import opennlp.tools.tokenize.WhitespaceTokenizer;

public class DocumentClassificationTagger {
	
	private final static char tabDelimiter = '\t';
	private final static char NewLineDelimiter = '\n';
	private final DocumentClassificationModel model;
	
	public DocumentClassificationTagger(final String inputModel) throws IOException {
		
		InputStream is = new FileInputStream(inputModel);
		model = new DocumentClassificationModel(is);
	}
	
	public String classify(final String document) {
		
		DocumentClassificationME myCategorizer = new DocumentClassificationME(model);
		// Whitespace tokenize entire string
	    String tokens[] = WhitespaceTokenizer.INSTANCE.tokenize(document);
		double[] outcomes = myCategorizer.categorize(tokens, Collections.emptyMap());
		String category = myCategorizer.getBestCategory(outcomes);
		return document + tabDelimiter + category + NewLineDelimiter;
	}
}
