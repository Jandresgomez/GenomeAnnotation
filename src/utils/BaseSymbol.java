package utils;

public class BaseSymbol {
	public double p1;
	public double p2;
	public String baseSymbol;
	public String positive;
	public String negative;
	
	public BaseSymbol(double p1, double p2, String symbol) {
		super();
		this.p1 = p1;
		this.p2 = p2;
		this.baseSymbol = symbol;
		this.positive = this.baseSymbol + "+";
		this.negative = this.baseSymbol + "-";
	}
}
