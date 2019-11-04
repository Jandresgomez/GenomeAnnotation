import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

public class ModelLearner {
	private static final String INIT_KEY = "START";
	
	private HashMap<String, int[]> modelOcurrences = new HashMap<String, int[]>();
	private String[] labels = { "A+", "C+", "G+", "T+", "A-", "C-", "G-", "T-" };
	private BufferedReader genomeBr;
	private BufferedReader annotationBr;
	private GeneAnnotation currentGene;
	private boolean canUpdate = true;

	public ModelLearner(String genomeFilePath, String annotationFilePath) throws IOException, FileNotFoundException {
		for (String item : labels) {
			int[] quants = new int[labels.length];
			modelOcurrences.put(item, quants);
		}

		modelOcurrences.put(INIT_KEY, new int[labels.length]);

		genomeBr = new BufferedReader(new FileReader(new File(genomeFilePath)));
		annotationBr = new BufferedReader(new FileReader(new File(annotationFilePath)));
		
		updateCurrentGene();
	}

	public void addOcurrences() throws IOException {
		//Skip first line (genome description)
		genomeBr.readLine();
		String line = genomeBr.readLine();
		
		int i = 1;
		String key = "START";
		String nextKey = "";
		while (line != null) {
			char[] arr = line.toCharArray();
			for(int j = 0; j < arr.length; j++) {
				char currentChar = arr[j];
				
				nextKey = currentChar + "";
				nextKey += (isGene(i++) ? "+" : "-");
				int pos = getLabelsPos(nextKey);
				
				int[] counts = modelOcurrences.get(key);
				counts[pos] += 1;
				modelOcurrences.put(key, counts);
				
				key = nextKey;
				
				if(i%500000 == 0) System.out.println(String.format("PROGRESS UPDATE (%d)", i));
			}
			
			line = genomeBr.readLine();
		}
	}
	
	public int getLabelsPos(String key) {
		for(int i = 0; i < labels.length; i++) {
			if (key.equals(labels[i])) return i;
		}
		return -1;
	}
	
	public void printOcurrences() {
		for(String key : modelOcurrences.keySet()) {
			int[] counts = modelOcurrences.get(key);
			
			double total = 0;
			for(int i = 0; i < counts.length; i++) total += counts[i];
			
			for(int i = 0; i < counts.length; i++) {
				String label = labels[i];
				double rate = total > 0? 100*(counts[i]/total) : 0.0;
				System.out.println(String.format("%s -> %s (%d, %f%%)", key, label, counts[i], rate));
			}
		}
	}
	
	public void printOcurrencesToFile(String filename) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(filename));
		for(String key : modelOcurrences.keySet()) {
			int[] counts = modelOcurrences.get(key);
			
			double total = 0;
			for(int i = 0; i < counts.length; i++) total += counts[i];
			
			for(int i = 0; i < counts.length; i++) {
				String label = labels[i];
				double rate = total > 0? 100*(counts[i]/total) : 0.0;
				pw.println(String.format("%s -> %s (%d, %f%%)", key, label, counts[i], rate));
			}
		}
		pw.flush();
		pw.close();
	}

	public boolean isGene(int pos) throws IOException {
		if (pos > currentGene.posFin) {
			if (canUpdate) {
				updateCurrentGene();
				return isGene(pos);
			} else {
				return false;
			}
		}
		
		if (pos <= currentGene.posFin && pos >= currentGene.posInit) return true;
		
		return false;
	}
	
	public void updateCurrentGene() throws IOException {
		boolean updated = false;
		String line = "";
		while(!updated) {
			line = annotationBr.readLine();
			if(line == null) {
				updated = true;
				canUpdate = false;
			} else if (GeneAnnotation.isValidAnnotation(line)) {
				GeneAnnotation annotation = new GeneAnnotation(line);
				if (annotation.isPositiveStrand) {
					currentGene = annotation;
					updated = true;
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			for(String genome : args) {
				ModelLearner ml = new ModelLearner("./Data/" + genome + ".fasta", "./Data/" + genome + ".gff3");
				ml.addOcurrences();
				ml.printOcurrencesToFile("./Results/" + genome + ".hmm");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}


