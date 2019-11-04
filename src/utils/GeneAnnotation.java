package utils;
import java.util.Arrays;

public class GeneAnnotation {
	public int posInit;
	public int posFin;
	public boolean isPositiveStrand;
	
	public GeneAnnotation(String line) throws IndexOutOfBoundsException {
		String[] data = line.split("\t");
		//System.out.println(Arrays.toString(data));
		
		posInit = Integer.parseInt(data[3]);
		posFin = Integer.parseInt(data[4]);
		isPositiveStrand = data[6].equals("+"); 
	}
	
	
	
	public GeneAnnotation(int posInit, int posFin, boolean isPositiveStrand) {
		this.posInit = posInit;
		this.posFin = posFin;
		this.isPositiveStrand = isPositiveStrand;
	}



	public static boolean isValidAnnotation(String line) {
		return line.contains("gene\t");
	}
}
