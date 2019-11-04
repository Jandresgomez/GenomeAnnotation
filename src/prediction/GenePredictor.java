package prediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import learner.ModelLearner;
import utils.BaseSymbol;

public class GenePredictor {
	private HashMap<String, double[]> probabilityModel = new HashMap<String, double[]>();
	private String[] labels = ModelLearner.getLabels();
	private BaseSymbol[] currentTable;
	private int tableSize;
	private String genomeFileName;
	private String processName;
	
	//Table handling variables
	private int lastTableId;
	private int tablePointer;
	private BaseSymbol lastSymbol;
	
	public GenePredictor(int tableSize, String genomeFileName, String processName) {
		this.tableSize = tableSize;
		this.genomeFileName = "./Data/" + genomeFileName;
		this.processName = processName;
	}
	
	public void generateAnnotation() throws FileNotFoundException, IOException {
		//Starting parameters
		BufferedReader br = new BufferedReader(new FileReader(genomeFileName));
		currentTable = new BaseSymbol[tableSize];
		lastTableId = 0;
		tablePointer = 0;
		lastSymbol = new BaseSymbol(0, 0, ModelLearner.INIT_KEY);
		
		//Skip first line, this is genome header data
		br.readLine();
		String line = br.readLine();
		while(line != null) {
			process(line);
			line = br.readLine();
		}
		
		dumpTable();
		br.close();
	}
	
	public void process(String line) throws FileNotFoundException {
		for(char key : line.toCharArray()) {
			String keyPositive = key + "+";
			String keyNegative = key + "-";
			int posPositive = getLabelsPos(keyPositive);
			int posNegative = getLabelsPos(keyNegative);
			
			double p1 = Math.max(lastSymbol.p1 + probabilityModel.get(lastSymbol.positive)[posPositive],
					lastSymbol.p2 + probabilityModel.get(lastSymbol.negative)[posPositive]);
			double p2 = Math.max(lastSymbol.p1 + probabilityModel.get(lastSymbol.positive)[posNegative],
					lastSymbol.p2 + probabilityModel.get(lastSymbol.negative)[posNegative]);
			
			lastSymbol = new BaseSymbol(p1, p2, key + "");
			currentTable[tablePointer++] = lastSymbol;
			if(tablePointer >= currentTable.length) {
				dumpTable();
			}
		}
	}
	
	public void dumpTable() throws FileNotFoundException {
		System.out.println(String.format("Current progress DUMP == %d", lastTableId));
		String newFileName = String.format("./Temp/%s_tableDump_%d.dump", processName, lastTableId++);
		PrintWriter pw = new PrintWriter(new File(newFileName));
		for(int i = tablePointer-1; i >= 0; i--) {
			BaseSymbol symbol = currentTable[i];
			pw.println(String.format("%s;%f;%f", symbol.baseSymbol, symbol.p1, symbol.p2));
		}
		
		tablePointer = 0;
		currentTable = new BaseSymbol[tableSize];
		pw.flush();
		pw.close();
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
		probabilityModel.put(ModelLearner.INIT_KEY + "+", new double[size]);
		probabilityModel.put(ModelLearner.INIT_KEY + "-", new double[size]);
		
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
	
	public static void main(String[] args) {
		try {
			GenePredictor instance = new GenePredictor(1000000, "Aeropyrum.fasta", "AeropyrumTest");
			instance.loadProbabilityModel("./Results/Aeropyrum.hmm");
			instance.generateAnnotation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
