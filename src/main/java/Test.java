import java.util.Scanner;

public class Test {
	
	public static void main(String[] args) {
		for (int i = 1; i <= 100; i++) {
			double d = i * 0.01D;
			if(d != (double)(float)d) { // if imprecise
				System.out.println((double)(float)d + "D " + "(double)" + (float)d + "F");
			}
		}
		Scanner ss = new Scanner(System.in);
		String s = ss.nextLine();
	}
}
