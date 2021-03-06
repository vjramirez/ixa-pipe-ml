/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eus.ixa.ixa.pipe.ml.sequence;

import opennlp.tools.util.SequenceValidator;

public class BilouSequenceValidator implements SequenceValidator<String> {

  @Override
  public boolean validSequence(final int i, final String[] inputSequence,
      final String[] outcomesSequence, final String outcome) {

    if (outcome.endsWith(BilouCodec.CONTINUE)
        || outcome.endsWith(BilouCodec.LAST)) {

      final int li = outcomesSequence.length - 1;

      if (li == -1) {
        return false;
      } else if (outcomesSequence[li].endsWith(BilouCodec.OTHER)
          || outcomesSequence[li].endsWith(BilouCodec.UNIT)) {
        return false;
      } else if (outcomesSequence[li].endsWith(BilouCodec.CONTINUE)
          || outcomesSequence[li].endsWith(BilouCodec.START)) {
        // if it is continue, we have to check if previous match was of the same
        // type
        final String previousNameType = SequenceLabelerME
            .extractNameType(outcomesSequence[li]);
        final String nameType = SequenceLabelerME.extractNameType(outcome);
        if (previousNameType != null || nameType != null) {
          if (nameType != null) {
            if (nameType.equals(previousNameType)) {
              return true;
            }
          }
          return false; // outcomes types are not equal
        }
      }
    }

    if (outcomesSequence.length - 1 > 0) {
      if (outcome.endsWith(BilouCodec.OTHER)) {
        if (outcomesSequence[outcomesSequence.length - 1]
            .endsWith(BilouCodec.START)
            || outcomesSequence[outcomesSequence.length - 1]
                .endsWith(BilouCodec.CONTINUE)) {
          return false;
        }
      }
    }

    return true;
  }
}