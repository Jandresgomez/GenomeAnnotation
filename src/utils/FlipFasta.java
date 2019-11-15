package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class FlipFasta {
	private static int LINE_LENGTH = 70;
	
	private char[] bases = {'A', 'C', 'G', 'T'};
	private char[] pair = {'T', 'G', 'C', 'A'};
	
	public void flip(String fileName) throws IOException {
		ArrayList<String> sequences = new ArrayList<String>();
		String newSeq = "";
		int count = 0;
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		
		String description = br.readLine();
		String line = br.readLine();
		while(line != null) {
			for (char c : line.toCharArray()) {
				int k = indexOfBase(c);
				newSeq = pair[k] + newSeq;
			}
			if (++count%1000 == 0) {
				sequences.add(newSeq);
				//System.out.println(newSeq.length()*count/1000);
				newSeq = "";
			}
			line = br.readLine();
		} sequences.add(newSeq);
		
		br.close();
		
		String baseSeq = "";
		for (int j = sequences.size()-1; j >= 0; j--) {
			String currentSeq = sequences.get(j);
			baseSeq = baseSeq.concat(currentSeq);
		}

		PrintWriter pw = new PrintWriter(new File(String.format("%s.flip", fileName)));
		pw.println(description);
		for (int i = 0; i < baseSeq.length(); i+=LINE_LENGTH) {
			int end = i+LINE_LENGTH > baseSeq.length()? baseSeq.length() : i+LINE_LENGTH;
			pw.println(baseSeq.substring(i, end));
			pw.flush();
		}
		pw.close();
	}
	
	public int indexOfBase(char k) {
		for (int i = 0; i < bases.length; i++) if (bases[i] == k) return i;
		return -1;
	}
	
	public static void main(String[] args) throws IOException {
		String fileName = args[0];
		FlipFasta instance = new FlipFasta();
		instance.flip(fileName);
	}
}
