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
