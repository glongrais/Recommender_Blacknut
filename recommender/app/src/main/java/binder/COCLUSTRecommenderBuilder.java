/*
	Copyright 2019 Florestan De Moor

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
package binder;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.impl.eval.Fold;
import org.apache.mahout.cf.taste.impl.recommender.COCLUSTRecommender;
import org.apache.mahout.cf.taste.impl.recommender.TrainingItemsCandidateItemStrategy;

public class COCLUSTRecommenderBuilder implements RecommenderBuilder {
	
	private final int k;
	private final int l;
	private final int iter;
	
	COCLUSTRecommenderBuilder(int k, int l, int iter) throws TasteException {
		this.k = k;
		this.l = l;
		this.iter = iter;
	}
	
	@Override
	public Recommender buildRecommender(DataModel dataModel) throws TasteException {
		return new COCLUSTRecommender(dataModel, this.k, this.l, this.iter, new TrainingItemsCandidateItemStrategy(dataModel), 0);
	}

	@Override
	public Recommender buildRecommender(DataModel dataModel, Fold f) throws TasteException {
		return buildRecommender(dataModel);
	}

}
