package prediction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

import learner.ModelLearner;

public class GenePredictor {
	private HashMap<String, double[]> probabilityModel = new HashMap<String, double[]>();
	private String[] labels = ModelLearner.getLabels();
	
	public GenePredictor() {
	}
	
	public int getLabelsPos(String key) {
		for(int i = 0; i < labels.length; i++) {
			if (key.equals(labels[i])) return i;
		}
		return -1;
	}
	
	public void loadProbabilityModel(String fileName) {
		int size = labels.length;
		for(String k : labels) probabilityModel.put(k, new double[size]);
		probabilityModel.put(ModelLearner.INIT_KEY, new double[size]);
		
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();
			String[] data;
			while(line != null) {
				//key;label;count;odds
				data = line.split(";");
				double odds = Double.parseDouble(data[3]);
				double logOdds = odds > 0? Math.log(odds) : Double.NEGATIVE_INFINITY;
				int pos = getLabelsPos(data[1]);
				
				double[] logOddsArray = probabilityModel.get(data[0]);
				logOddsArray[pos] = logOdds;
				
				line = br.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		double[] baseOdds = probabilityModel.get(ModelLearner.INIT_KEY);
		for(int i = 0; i < baseOdds.length; i++) baseOdds[i] = Math.log(1/baseOdds.length);
	}
}
