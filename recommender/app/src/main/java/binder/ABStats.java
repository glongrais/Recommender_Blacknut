/*
    Copyright 2020 Guillaume Longrais

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

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import com.google.cloud.bigquery.TableResult;

import org.apache.commons.math3.special.Gamma;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ABStats {

    // Compute to chi-square test, return true if algos are independents, false in the other case
    public static boolean chiSquare(HashMap<String, Double> algos, double significance) {
        double mean = 0.0, dist = 0.0;

        for (double v : algos.values()) {
            mean += v;
        }

        mean /= algos.size();

        for (double v : algos.values()) {
            dist += ((v - mean) * (v - mean)) / mean;
        }

        double prob = Gamma.regularizedGammaQ(algos.size() - 1, dist);

        return prob < significance;
    }

    // Parse the TableResult from the BigQuery request and return hashmap with the average clickrate for each algo from it ( HashMap<AlgoName, clickrate> )
    public static HashMap<String, Double> clickrate(TableResult t) {
        // TODO Implementation of the function
        return new HashMap<>();
    }

    // The main function to call for the ABStat with the TableResult from the BigQuery request in parameters
    public static void ABTestStat(TableResult t, String name) {

        HashMap<String, Double> click = clickrate(t);
        boolean nullHypothesis = chiSquare(click, 0.05);

        JSONArray algos = new JSONArray();
        JSONArray clickrate = new JSONArray();

        for(String algo: click.keySet()){
            name += "_"+algo;
            algos.add(algo);
            JSONObject tmp = new JSONObject();
            tmp.put(algo, click.get(algo));
            clickrate.add(tmp);
        }

        JSONObject result = new JSONObject();
        result.put("algos", algos);
        result.put("nullHypothesis", nullHypothesis);
        result.put("clickrate", clickrate);

        FileWriter f;
        try {
            f = new FileWriter(name);
            f.write(result.toJSONString());
            f.flush();
            f.close();
        } catch (IOException e) {
            System.exit(1);
            e.printStackTrace();
        }
    }
}