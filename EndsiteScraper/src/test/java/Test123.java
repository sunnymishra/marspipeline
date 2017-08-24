
public class Test123 {
	public static void main(String[] args) {
//		testParsingFloats();
		testCeling();
		
	}
	
	private static void testCeling(){
		int a = 332,b=50;
		double x = (double)a/b;
		double ceilX = Math.ceil(x);
		int result=(int)ceilX*b;
		System.out.println(x +" "+result);
		////////////////////////////////////////
		int containerWidth=751,itemWidth=180;
		int noOfItemsInEachRow = containerWidth / itemWidth;
		System.out.println(noOfItemsInEachRow);
	}
	private static void testParsingFloats(){
		System.out.println(Double.parseDouble("10.01"));
	}
}
