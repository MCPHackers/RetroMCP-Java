package org.mcphackers.mcp.tools.constants;

public class MathConstants extends Constants {
	
	protected static String replace_constants(String code) {

		code = code.replace(Math.PI + "D", "Math.PI");
		code = code.replace((float)Math.PI + "F", "(float)Math.PI");
		code = code.replace((float)Math.PI / 2F + "F", "(float)Math.PI / 2F");
		code = code.replace((float)Math.PI / 4.5F + "F", "(float)Math.PI / 4.5F");
		
		for (int i = 1; i <= 100; i++) {
			double d = i * 0.01D;
			if(d != (double)(float)d) { // if imprecise
				code = code.replace((double)(float)d + "D", "(double)" + (float)d + "F");
			}
		}
		return code;
	}
}
