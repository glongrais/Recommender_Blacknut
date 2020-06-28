package binder;

import binder.config.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.impl.common.DataPreprocessing;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.gson.JsonArray;

import java.util.UUID;

public class Evaluator {

	final static String prefix = "src/main/resources/";
	private static Logger logger = LoggerFactory.getLogger(Evaluator.class);

	public static void main(String[] args) throws IOException {

		String cfgFileName = prefix + "default_config.yml";

		/* Check command line arguments */
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("c", "config", true, "path of config file, otherwise default used");
		try {
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("config")) {
				cfgFileName = line.getOptionValue("config");
			}
		} catch (ParseException exp) {
		}

		/* Load configuration file */
		logger.info("Using {} as configuration file", cfgFileName);
		Config cfg = null;
		Yaml yaml = new Yaml();
		try (InputStream in = Files.newInputStream(Paths.get(cfgFileName))) {
			cfg = yaml.loadAs(in, Config.class);
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Couldn't read main configuration file");
			return;
		}

		logger.info("=== MAIN CONFIGURATION ===");
		cfg.logConfig(logger);

		/* Load dataset */
		logger.info("Loading dataset");

		Grade g;

		ArrayList<String> games = new ArrayList<>();

		if (cfg.getData().equals("online")) {

			String projectId = "blacknut-analytics";
			File credentialsPath = new File(cfg.getKeyPath());

			// Load credentials from JSON key file.

			GoogleCredentials credentials;
			try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
				credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
			}

			// Instantiate a client.
			BigQuery bigquery = BigQueryOptions.newBuilder().setCredentials(credentials).setProjectId(projectId).build()
					.getService();

			QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder("SELECT * from external_share.streams")
					.setUseLegacySql(false).build();
			QueryJobConfiguration queryConfigGames = QueryJobConfiguration.newBuilder("SELECT * from external_share.games")
					.setUseLegacySql(false).build();

			JobId jobId = JobId.of(UUID.randomUUID().toString());
			JobId jobIdGames = JobId.of(UUID.randomUUID().toString());
			Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
			Job queryJobGames = bigquery.create(JobInfo.newBuilder(queryConfigGames).setJobId(jobIdGames).build());

			// Wait for the query to complete.
			try {
				queryJob = queryJob.waitFor();
				queryJobGames = queryJobGames.waitFor();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			TableResult result;
			TableResult gamesTable;
			try {

				result = queryJob.getQueryResults();
				gamesTable = queryJobGames.getQueryResults();
				g = new Grade(result);
			} catch (JobException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}

			for (FieldValueList row : gamesTable.iterateAll()) {
				games.add(row.get("global_id").getStringValue());
			}
		} else if (cfg.getData().equals("local")) {
			g = new Grade(cfg.getDataset(), true);
		} else {
			logger.error("Data config is not set local or online");
			return;
		}

		DataModel model = null;
		model = g.NumberOfSession();

		if (cfg.getNormalize()) {
			try {
				logger.info("Normalizing dataset");
				model = DataPreprocessing.normalize(model);
			} catch (TasteException e) {
				e.printStackTrace();
				logger.error("Couldn't normalize dataset");
				return;
			}
		}
		if (cfg.getBinarize()) {
			try {
				logger.info("Binarizing dataset");
				model = DataPreprocessing.binarize(model, 3.0f);
			} catch (TasteException e) {
				e.printStackTrace();
				logger.error("Couldn't binarize dataset");
				return;
			}
		}
		logger.info("Done with dataset");

		/* Read configuration files of all recommender algorithms specified */
		HashMap<String, AbstractConfig> configs = new HashMap<String, AbstractConfig>(cfg.getConfigs().size());
		for (String s : cfg.getConfigs()) {
			AbstractConfig c = null;
			Yaml yml = new Yaml();
			if (s.equals("random")) {
				c = new RandomConfig();
			} else if (s.equals("itemavg")) {
				c = new ItemAvgConfig();
			} else if (s.equals("itemuseravg")) {
				c = new ItemUserAvgConfig();
			} else {
				try (InputStream in = Files.newInputStream(Paths.get(prefix + s))) {
					if (s.contains("ubknn")) {
						c = yml.loadAs(in, UBKNNConfig.class);
					} else if (s.contains("ibknn")) {
						c = yml.loadAs(in, IBKNNConfig.class);
					} else if (s.contains("mf")) {
						c = yml.loadAs(in, MFConfig.class);
					} else if (s.contains("coclustr")) {
						c = yml.loadAs(in, COCLUSTRConfig.class);
					} else if (s.contains("coclust")) {
						c = yml.loadAs(in, COCLUSTConfig.class);
					} else if (s.contains("nbcf")) {
						c = yml.loadAs(in, NBCFConfig.class);
					} else if (s.contains("bbcf")) {
						c = yml.loadAs(in, BBCFConfig.class);
					} else if (s.contains("bicainet")) {
						c = yml.loadAs(in, BicaiNetConfig.class);
					} else if (s.contains("bcn")) {
						c = yml.loadAs(in, BCNConfig.class);
					} else {
						logger.error("Unrecognized algorithm");
						return;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					logger.error("Couldn't read specific configuration file {}", s);
					return;
				}
			}
			configs.put(s, c);
		}
		try {
			logger.info("Starting recommendations");

			JSONArray users = new JSONArray();
			FileWriter f;
			LongPrimitiveIterator it_user = model.getUserIDs();
			int numUser = model.getNumUsers();
			int nbFile = 0;
			if(cfg.getNbUserPerFile() != 0){
				nbFile = numUser/cfg.getNbUserPerFile() + 1;
			}
			int fileNb = 1;
			int index = 0;

			while (it_user.hasNext()) {
				long id = it_user.next();

				if (cfg.getNbUserPerFile() > 0 && cfg.getNbUserPerFile() == index) {
					f = new FileWriter("result" + fileNb + "_" + nbFile + ".json");
					f.write(users.toJSONString());
					f.flush();
					f.close();
					users = new JSONArray();
					fileNb++;
					index=0;
				}

				JSONObject user = new JSONObject();
				user.put("user_id", g.getOldUserId((int) id));
				Iterator<Entry<String, AbstractConfig>> it = configs.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, AbstractConfig> pair = it.next();
					AbstractConfig c = (AbstractConfig) pair.getValue();
					// c.logConfig(logcfg);
					String name = pair.getKey();

					RecommenderBuilder builder = new BinderRecommenderBuilder(c, 3.0f);
					List<RecommendedItem> itemRecommendations = builder.buildRecommender(model).recommend(id,
							cfg.getNbRecommendation());
					JsonArray reco = new JsonArray();
					for (RecommendedItem itemRecommendation : itemRecommendations) {
						String idGame = g.getOldGameId((int) itemRecommendation.getItemID());
						if(cfg.getData().equals("local") || games.contains(idGame)){
							reco.add(idGame);
						}
					}

					user.put(name, reco);
				}
				Random rand = new Random();
				if (rand.nextInt() < 0.5) {
					user.put("display", "mf/config79.yml");
				} else {
					user.put("display", "ibknn/config0.yml");
				}
				users.add(user);
				index++;

			}
			if (cfg.getNbUserPerFile() == 0) {
				f = new FileWriter(cfg.getResultPath());
			} else if (index != 0) {
				f = new FileWriter("result" + fileNb + "_" + nbFile + ".json");
			} else {
				return;
			}
			f.write(users.toJSONString());
			f.flush();
			f.close();

			logger.info("Recommendations finished");

		} catch (TasteException e) {
			e.printStackTrace();
			logger.error("Error during recommendations");
			return;
		}

	}

}
