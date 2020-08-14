/*
	Copyright 2020 Florestan De Moor & Guillaume Longrais

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

import binder.config.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

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
import org.json.simple.parser.JSONParser;
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

	static String prefix = "src/main/resources/";
	private static Logger logger = LoggerFactory.getLogger(Evaluator.class);

	public static void main(String[] args) throws IOException {

		DateFormat format = new SimpleDateFormat("dd_MM_yyyy");

		String cfgFileName = prefix + "default_config.yml";
		Boolean checkMode = false;
		Date checkDate = null;

		/* Check command line arguments */
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("c", "config", true, "Path of config file, otherwise default used");
		options.addOption("ch", "check", false, "Run the app in check mode without the recommendations");
		options.addOption("chd", "checkDate", true, "Change this actual date");
		try {
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("config")) {
				cfgFileName = line.getOptionValue("config");
			}
			if (line.hasOption("check")) {
				checkMode = true;
			}
			if (line.hasOption("checkDate")) {
				checkDate = format.parse(line.getOptionValue("checkDate"));
			}
		} catch (ParseException exp) {
			exp.printStackTrace();
			System.exit(1);
		} catch (java.text.ParseException e) {
			e.printStackTrace();
			System.exit(1);
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
			System.exit(1);
		}

		if (cfg.getNbUserPerFile() < 0) {
			logger.error("The number of users per file can't be negative");
			System.exit(1);
		}

		if(!checkMode){
			logger.info("=== MAIN CONFIGURATION ===");
			cfg.logConfig(logger);
		}

		/* Read configuration files of all tests specified */

		JSONParser jsonParser = new JSONParser();
		JSONObject currentTest = null;
		Boolean everyday_refresh = null;
		Date today = null;

		try (FileReader reader = new FileReader(cfg.getTestPath())) {

			Object obj = jsonParser.parse(reader);

			if (checkMode) {
				today = checkDate;
			} else {
				today = new Date();
			}
			Date tmpDate = null;

			String endDateString = (String) ((JSONObject) obj).get("end_date");
			Date endDate = format.parse(endDateString);

			if (today.after(endDate)) {
				logger.error("Test period completed : {}", endDate);
				System.exit(1);
			}

			JSONArray tests = (JSONArray) ((JSONObject) obj).get("tests");

			for (Object test : tests) {
				Date d = format.parse((String) ((JSONObject) test).get("start_date"));
				if (tmpDate == null) {
					tmpDate = d;
					currentTest = (JSONObject) test;
				}

				if (d.compareTo(tmpDate) > 0 && d.compareTo(today) <= 0) {
					tmpDate = d;
					currentTest = (JSONObject) test;
				}
			}

			if (tmpDate == null || tmpDate.after(today)) {
				logger.error("No tests to do today : {}", today);
				System.exit(1);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (java.text.ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
		if(checkMode){
			logger.info("Current test : {}",currentTest.toString());
		}

		/* Load dataset */

		logger.info("Loading dataset");

		Grade g = null;
		;

		ArrayList<String> games = new ArrayList<>();
		ArrayList<String> users = new ArrayList<>();

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

			QueryJobConfiguration queryConfig;
			QueryJobConfiguration queryConfigGames;
			QueryJobConfiguration queryConfigClick;
			QueryJobConfiguration queryConfigUsers;

			String streamQuery;
			String gamesQuery;
			String clickQuery;
			String usersQuery;

			if(currentTest.containsKey("streamsQuery")){
				streamQuery = (String) currentTest.get("streamsQuery");
			}else{
				streamQuery = "SELECT * from external_share.streams";
			}

			if(currentTest.containsKey("gamesQuery")){
				gamesQuery = (String) currentTest.get("gamesQuery");
			}else{
				gamesQuery = "SELECT * from external_share.games";
			}

			if(currentTest.containsKey("clickQuery")){
				clickQuery = (String) currentTest.get("clickQuery");
			}else{
				// TODO complete with the right query provided by Blacknut
				clickQuery = "SELECT * from external_share.streams";
			}

			if(currentTest.containsKey("usersQuery")){
				usersQuery = (String) currentTest.get("usersQuery");
			}else{
				usersQuery = "SELECT * from external_share.users";
			}

			if (checkMode) {
				queryConfig = QueryJobConfiguration.newBuilder(streamQuery+" limit 1")
						.setUseLegacySql(false).build();
				queryConfigGames = QueryJobConfiguration.newBuilder(gamesQuery+" limit 1")
						.setUseLegacySql(false).build();
				queryConfigClick = QueryJobConfiguration.newBuilder(clickQuery+" limit 1")
						.setUseLegacySql(false).build();
				queryConfigUsers = QueryJobConfiguration.newBuilder(usersQuery+" limit 1")
						.setUseLegacySql(false).build();
			} else {
				queryConfig = QueryJobConfiguration.newBuilder(streamQuery)
						.setUseLegacySql(false).build();
				queryConfigGames = QueryJobConfiguration.newBuilder(gamesQuery)
						.setUseLegacySql(false).build();
				queryConfigClick = QueryJobConfiguration.newBuilder(clickQuery)
						.setUseLegacySql(false).build();
				queryConfigUsers = QueryJobConfiguration.newBuilder(usersQuery)
						.setUseLegacySql(false).build();
			}

			JobId jobId = JobId.of(UUID.randomUUID().toString());
			JobId jobIdGames = JobId.of(UUID.randomUUID().toString());
			JobId jobIdClick = JobId.of(UUID.randomUUID().toString());
			JobId jobIdUsers = JobId.of(UUID.randomUUID().toString());
			Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
			Job queryJobGames = bigquery.create(JobInfo.newBuilder(queryConfigGames).setJobId(jobIdGames).build());
			Job queryJobClick = bigquery.create(JobInfo.newBuilder(queryConfigClick).setJobId(jobIdClick).build());
			Job queryJobUsers = bigquery.create(JobInfo.newBuilder(queryConfigUsers).setJobId(jobIdUsers).build());

			// Wait for the query to complete.

			try {
				queryJob = queryJob.waitFor();
				queryJobGames = queryJobGames.waitFor();
				queryJobClick = queryJobClick.waitFor();
				queryJobUsers = queryJobUsers.waitFor();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				System.exit(1);
			}

			TableResult result = null;
			TableResult gamesTable = null;
			TableResult clickTable = null;
			TableResult usersTable = null;
			try {

				result = queryJob.getQueryResults();
				gamesTable = queryJobGames.getQueryResults();
				clickTable = queryJobClick.getQueryResults();
				usersTable = queryJobUsers.getQueryResults();
				g = new Grade(result);
				ABStats.ABTestStat(clickTable, cfg.getABPath());
			} catch (JobException e1) {
				e1.printStackTrace();
				System.exit(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				System.exit(1);
			}

			for (FieldValueList row : gamesTable.iterateAll()) {
				games.add(row.get("global_id").getStringValue());
			}
			for (FieldValueList row : usersTable.iterateAll()) {
				users.add(row.get("_id").getStringValue());
			}
		} else if (cfg.getData().equals("local")) {
			g = new Grade(cfg.getDataset(), true);
		} else {
			logger.error("Data config is not set local or online");
			System.exit(1);
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
				System.exit(1);
			}
		}
		if (cfg.getBinarize()) {
			try {
				logger.info("Binarizing dataset");
				model = DataPreprocessing.binarize(model, 3.0f);
			} catch (TasteException e) {
				e.printStackTrace();
				logger.error("Couldn't binarize dataset");
				System.exit(1);
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
						System.exit(1);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					logger.error("Couldn't read specific configuration file {}", s);
					System.exit(1);
				}
			}
			configs.put(s, c);
		}

		// Assigning algo to display to every users

		everyday_refresh = (Boolean) currentTest.get("everydayRefresh");

		JSONArray algos = (JSONArray) currentTest.get("algos");
		HashMap<String, Integer> algoUsers = new HashMap<>();

		if (everyday_refresh) {
			Random rand = new Random();

			for (String user : g.getAllOldUserId()) {
				int i = rand.nextInt(algos.size());
				algoUsers.put(user, i);
			}
		} else {
			try {
				Date d = format.parse((String) ((JSONObject) currentTest).get("start_date"));

				if (d.compareTo(today) == 0) {
					Random rand = new Random();

					for (String user : g.getAllOldUserId()) {
						int i = rand.nextInt(algos.size());
						algoUsers.put(user, i);
					}
					setAlgoUsers(algoUsers,
							((Long) currentTest.get("id")) + "_" + ((String) currentTest.get("start_date")) + ".json");
				} else {
					algoUsers = getAlgoUsers(
							((Long) currentTest.get("id")) + "_" + ((String) currentTest.get("start_date")) + ".json",
							g.getAllOldUserId(), algos.size());
				}
			} catch (java.text.ParseException e) {
				System.exit(1);
				e.printStackTrace();
			}

		}

		/* Recommendations */

		try {
			if(!checkMode){
				logger.info("Starting recommendations");
			}

			JSONArray usersJSON = new JSONArray();
			FileWriter f;
			LongPrimitiveIterator it_user = model.getUserIDs();
			int numUser = model.getNumUsers();
			int nbFile = 0;
			if (cfg.getNbUserPerFile() != 0) {
				nbFile = numUser / cfg.getNbUserPerFile() + 1;
			}
			int fileNb = 1;
			int index = 0;

			while (it_user.hasNext() && !checkMode) {
				long id = it_user.next();
				String idUser = g.getOldUserId((int) id);

				if(!users.contains(idUser)){
					continue;
				}

				if (cfg.getNbUserPerFile() > 0 && cfg.getNbUserPerFile() == index) {
					String str = cfg.getResultPath();
					f = new FileWriter(str.substring(0, str.length() - 5) + fileNb + "_" + nbFile + ".json");
					f.write(usersJSON.toJSONString());
					f.flush();
					f.close();
					usersJSON = new JSONArray();
					fileNb++;
					index = 0;
				}

				JSONObject user = new JSONObject();
				user.put("user_id", idUser);
				Iterator<Entry<String, AbstractConfig>> it = configs.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, AbstractConfig> pair = it.next();
					AbstractConfig c = (AbstractConfig) pair.getValue();
					// c.logConfig(logcfg);
					String nameAlgo = pair.getKey();

					RecommenderBuilder builder = new BinderRecommenderBuilder(c, 3.0f);
					List<RecommendedItem> itemRecommendations = builder.buildRecommender(model).recommend(id,
							cfg.getNbRecommendation());
					JsonArray reco = new JsonArray();
					for (RecommendedItem itemRecommendation : itemRecommendations) {
						String idGame = g.getOldGameId((int) itemRecommendation.getItemID());
						if (cfg.getData().equals("local") || games.contains(idGame)) {
							reco.add(idGame);
						}
					}

					user.put(nameAlgo, reco);
				}

				user.put("display", algos.get(algoUsers.get(idUser)));

				user.put("test_id", currentTest.get("id"));
				usersJSON.add(user);
				index++;

			}
			if (cfg.getNbUserPerFile() == 0) {
				f = new FileWriter(cfg.getResultPath());
			} else if (index != 0) {
				f = new FileWriter("result" + fileNb + "_" + nbFile + ".json");
			} else {
				System.exit(1);
				return;
			}
			f.write(usersJSON.toJSONString());
			f.flush();
			f.close();

			logger.info("Recommendations finished");

		} catch (TasteException e) {
			e.printStackTrace();
			logger.error("Error during recommendations");
			System.exit(1);
		}

		System.exit(0);

	}

	private static HashMap<String, Integer> getAlgoUsers(String s, Set<String> users, int nbAlgos)
			throws FileNotFoundException {
		JSONParser jsonParser = new JSONParser();

		HashMap<String, Integer> result = new HashMap<>();
		try (FileReader reader = new FileReader(s)) {
			Object obj = jsonParser.parse(reader);

			for (String user : users) {
				if (((JSONObject) obj).containsKey(user)) {
					result.put(user, (int)(long) ((JSONObject) obj).get(user));
				} else {
					Random rand = new Random();
					int i = rand.nextInt(nbAlgos);
					result.put(user, i);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return result;
	}

	private static void setAlgoUsers(HashMap<String, Integer> algoUsers, String name) throws IOException {
		JSONObject result = new JSONObject(algoUsers);
		FileWriter f = new FileWriter(name);
		f.write(result.toJSONString());
		f.flush();
		f.close();
	}

}
