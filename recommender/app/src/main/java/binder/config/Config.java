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
package binder.config;

import org.slf4j.Logger;

import java.util.List;

public class Config implements AbstractConfig {

	private String data; /* Set if the data will be local or online*/
	private String dataset; /* Path to the dataset file */
	private String resultPath;
	private String keyPath;
	private String testPath;
	private String ABPath;
	private int nbUserPerFile;
	private int nbRecommendation;
	private List<String> configs;
	private boolean normalize; /* Normalize dataset by subtracting user mean rating */
	private boolean binarize; /* Binarize ratings into 0 or 1 */


	public void setData(String s) {
		this.data = s;
	}

	public String getData() {
		return this.data;
	}

	public void setResultPath(String s) {
		this.resultPath= s;
	}

	public String getResultPath() {
		return this.resultPath;
	}

	public void setKeyPath(String s) {
		this.keyPath = s;
	}

	public String getKeyPath() {
		return this.keyPath;
	}

	public void setTestPath(String s) {
		this.testPath = s;
	}

	public String getTestPath() {
		return this.testPath;
	}

	public void setABPath(String s) {
		this.ABPath = s;
	}

	public String getABPath() {
		return this.ABPath;
	}

	public void setNbUserPerFile(int n) {
		this.nbUserPerFile = n;
	}

	public int getNbUserPerFile() {
		return this.nbUserPerFile;
	}

	public void setNbRecommendation(int n) {
		this.nbRecommendation = n;
	}

	public int getNbRecommendation() {
		return this.nbRecommendation;
	}

	public void setDataset(String s) {
		this.dataset = s;
	}

	public String getDataset() {
		return this.dataset;
	}

	public void setConfigs(List<String> l) {
		this.configs = l;
	}

	public List<String> getConfigs() {
		return this.configs;
	}

	public void setNormalize(boolean b) {
		this.normalize = b;
	}

	public boolean getNormalize() {
		return this.normalize;
	}

	public void setBinarize(boolean b) {
		this.binarize = b;
	}

	public boolean getBinarize() {
		return this.binarize;
	}

	@Override
	public void logConfig(Logger logger) {

		logger.info("Data: {}", this.data);
		logger.info("Dataset path: {}", this.dataset);
		logger.info("Result path: {}", this.resultPath);
		logger.info("Key path: {}", this.keyPath);
		logger.info("Tests path: {}", this.testPath);
		logger.info("AB path: {}", this.ABPath);
		logger.info("Number of recommendations: {}", this.nbRecommendation);
		logger.info("Number of users per file: {}", this.nbUserPerFile);
		logger.info("List of the configuration files of recommender algorithms to run: {}", this.configs.toString());
		logger.info("Normalize: {}", this.normalize);
		logger.info("Binarize: {}", this.binarize);
	}

}
