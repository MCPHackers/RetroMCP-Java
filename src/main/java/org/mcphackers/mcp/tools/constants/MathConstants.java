package org.mcphackers.mcp.tools.constants;

public class MathConstants extends Constants {
	
	protected String replace_constants(String code) {

		code = replaceValue(code, Math.PI, "Math.PI");
		code = replaceValue(code, (float)Math.PI, "(float)Math.PI");
		code = replaceValue(code, (float)Math.PI / 2F, "(float)Math.PI / 2F");
		code = replaceValue(code, (float)Math.PI / 4.5F, "(float)Math.PI / 4.5F");
		code = replaceValue(code, (double)(float)Math.PI, "(double)(float)Math.PI");
		code = replaceValue(code, Math.PI * 2D, "Math.PI * 2D");
		code = replaceValue(code, Math.PI / 2D, "Math.PI / 2D");
		for (int i = 1; i <= 100; i++) {
			double d = i * 0.01D;
			if(d != (double)(float)d) { // if imprecise
				code = floatCastedToDouble(code, d);
			}
		}
		code = floatCastedToDouble(code, 0.0075D);
		code = replaceValue(code, (double)0.999F, "(double)0.999F");
		code = replaceValue(code, 1.0D / 128, "1.0D / 128");
		code = replaceValue(code, (double)0.997F, "(double)0.997F");
		code = replaceValue(code, (double)1.62F, "(double)1.62F");
		return code;
	}
	
	private String floatCastedToDouble(String code, double value) {
		return code.replace((double)(float)value + "D", "(double)" + (float)value + "F");
	}
	
	private String replaceValue(String code, double value, String replace) {
		return code.replace(value + "D", replace);
	}
	
	private String replaceValue(String code, float value, String replace) {
		return code.replace(value + "F", replace);
	}
}
