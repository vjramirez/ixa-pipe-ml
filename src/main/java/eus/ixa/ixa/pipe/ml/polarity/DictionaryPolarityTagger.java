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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.utils.StringUtils;

public class DictionaryPolarityTagger {

	private final static char tabDelimiter = '\t';
	
	private final Map<String, String> dictMap = new HashMap<String, String>();
	String[] splitted = new String[64];
	
  /**
   * Construct a hashmap from the input tab separated dictionary.
   *
   * The input file should have, for each line, word\tabpolarity
   *
   * @param lexicon
   *          the input dictionary via path string
   * @throws IOException
   *           if io problems
   */
	
	public DictionaryPolarityTagger(final String lexicon) throws IOException {

		final BufferedReader breader = new BufferedReader(new InputStreamReader(
	        new FileInputStream(lexicon), Charset.forName("UTF-8")));
	    String line;
	    while ((line = breader.readLine()) != null) {
	      StringUtils.splitLine(line, tabDelimiter, this.splitted);
	      this.dictMap.put(this.splitted[0], this.splitted[1]);
	    }
	    breader.close();
	  }
	
  /**
   * Get the Map containing the dictionary.
   *
   * @return dictMap the Map
   */
	public Map<String, String> getDictMap() {
	    return this.dictMap;
	}

  /**
   * Lookup word in a dictionary. Outputs "O" if not found.
   * 
   * @param word
   *          the word
   * @return the polarity
   */
	public String apply(final String word) {
		String polarity = null;
		final String key = word;
		// lookup lemma as value of the map
		final String keyValue = this.dictMap.get(key);
		if (keyValue != null) {
			polarity = keyValue;
		} else {
			polarity = "O";
		}
		return polarity;
	}
	
	
}
