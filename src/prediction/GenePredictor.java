package prediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import learner.ModelLearner;
import utils.BaseSymbol;
import utils.FlipFasta;
import utils.GeneAnnotation;

public class GenePredictor {
	//Parameters
	private static int MIN_GENE_LENGTH = 40;
	
	private HashMap<String, double[]> probabilityModel = new HashMap<String, double[]>();
	private String[] labels = ModelLearner.getLabels();
	private BaseSymbol[] currentTable;
	private boolean[][] decisions;
	private int tableSize;
	private String genomeFileName;
	private String processName;
	
	//Table handling variables
	private int lastTableId;
	private int tablePointer;
	private BaseSymbol lastSymbol;
	
	public GenePredictor(int tableSize, String genomeFileName, String processName) {
		this.tableSize = tableSize;
		this.genomeFileName = genomeFileName;
		this.processName = processName;
	}
	
	public void generateAnnotation() throws FileNotFoundException, IOException {
		//Starting parameters
		BufferedReader br = new BufferedReader(new FileReader(genomeFileName));
		currentTable = new BaseSymbol[tableSize];
		decisions = new boolean[tableSize][2];
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
		
		dumpTables();
		br.close();
		
		buildAnnotations();
	}
	
	private void buildAnnotations() throws IOException {
		ArrayList<GeneAnnotation> annotations = new ArrayList<GeneAnnotation>();
		
		int currentSlot = (lastSymbol.p1 > lastSymbol.p2)? 0 : 1;
		int count = 0;
		boolean inGeneFlag = false;
		GeneAnnotation currentGene = null;
		
		for(int i = 0; i < lastTableId; i++) {
			String fileName = String.format("./Temp/%s_tableDump_%d.dump", processName, lastTableId - 1);
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			
			String line = br.readLine();
			while (line != null) {
				String[] data = line.split(";");
				
				if (currentSlot == 0) {
					if (inGeneFlag) {
						currentGene.posFin++;
					} else {
						inGeneFlag = true;
						currentGene = new GeneAnnotation(count, count, true);
					}
				} else if (inGeneFlag) {
					inGeneFlag = false;
					if (isValidGene(currentGene)) {
						annotations.add(currentGene);
						currentGene = null;
					} else {
						System.out.println("GENE WAS NOT VALID!!");
					}
				}
				
				currentSlot = data[currentSlot].equals("0") ? 0 : 1;
				count++;
				line = br.readLine();
			}
			
			br.close();
		}
		
		
		ArrayList<GeneAnnotation> reversed = new ArrayList<GeneAnnotation>();
		for(int k = annotations.size() - 1; k >= 0; k--) {
			GeneAnnotation annotation = annotations.get(k);
			int temp = annotation.posInit;
			annotation.posInit = count - annotation.posFin;
			annotation.posFin = count - temp;
			reversed.add(annotation);
		}
		
		PrintWriter pw = new PrintWriter(new File(String.format("./Results/%s_annotations.gff3", processName)));
		pw.println("type\tstart\tfinish\tstrand\tlength");
		for (GeneAnnotation k : reversed) {
			pw.println(String.format("gene\t%d\t%d\t%s\t%d", k.posInit, k.posFin, "+", k.posFin - k.posInit));
		}
		
		pw.flush();
		pw.close();
	}
	
	private boolean isValidGene(GeneAnnotation currentGene) {
		return Math.abs(currentGene.posFin - currentGene.posInit) > MIN_GENE_LENGTH;
	}

	private void process(String line) throws FileNotFoundException {
		for(char key : line.toCharArray()) {
			String keyPositive = key + "+";
			String keyNegative = key + "-";
			int posPositive = getLabelsPos(keyPositive);
			int posNegative = getLabelsPos(keyNegative);
			
			double p11 = lastSymbol.p1 + probabilityModel.get(lastSymbol.positive)[posPositive];
			double p12 = lastSymbol.p2 + probabilityModel.get(lastSymbol.negative)[posPositive];
			double p21 = lastSymbol.p1 + probabilityModel.get(lastSymbol.positive)[posNegative];
			double p22 = lastSymbol.p2 + probabilityModel.get(lastSymbol.negative)[posNegative];
			
			//Si p1 > p2, entonces esa posición es S+, de lo contrario es S-
			double p1 = Math.max(p11,p12);
			double p2 = Math.max(p21,p22);
			
			decisions[tablePointer][0] = (p1 == p11);
			decisions[tablePointer][1] = (p2 == p21);
			
			lastSymbol = new BaseSymbol(p1, p2, key + "");
			currentTable[tablePointer++] = lastSymbol;
			if(tablePointer >= currentTable.length) {
				dumpTables();
			}
		}
	}
	
	private void dumpTables() throws FileNotFoundException {
		System.out.println(String.format("Current progress DUMP == ID%d", lastTableId));
		String newFileName = String.format("./Temp/%s_tableDump_%d.dump", processName, lastTableId++);
		PrintWriter pw = new PrintWriter(new File(newFileName));
		for(int i = tablePointer-1; i >= 0; i--) {
			BaseSymbol symbol = currentTable[i];
			pw.println(String.format("%d;%d;%s", (decisions[i][0]? 0 : 1), (decisions[i][1]? 0 : 1), symbol.baseSymbol));
			//pw.println(String.format("%s;%f;%f", symbol.baseSymbol, symbol.p1, symbol.p2));
		}
		
		tablePointer = 0;
		currentTable = new BaseSymbol[tableSize];
		decisions = new boolean[tableSize][2];
		pw.flush();
		pw.close();
	}
	
	private int getLabelsPos(String key) {
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
				double logOdds = odds > 0? Math.log10(odds) : Double.NEGATIVE_INFINITY;
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
		String fileName = String.format("./Data/%s.fasta", args[0]);
		String processName = args[1];
		String modelData = args[2];
		try {
			GenePredictor instance = new GenePredictor(1000000, fileName, processName);
			instance.loadProbabilityModel(modelData);
			instance.generateAnnotation();
			
			instance = new GenePredictor(1000000, String.format("%s.flip", fileName), String.format("%s.flip", processName));
			instance.loadProbabilityModel(modelData);
			instance.generateAnnotation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
