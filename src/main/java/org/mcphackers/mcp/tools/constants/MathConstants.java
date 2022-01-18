package org.mcphackers.mcp.tools.constants;

import java.util.regex.Pattern;

public class MathConstants extends Constants {
	
	// Used to prevent strings from being captured, such as "2.0D"
	private static final Pattern _CONSTANT_REGEX = Pattern.compile("(?![\\\"\\'][.\\w\\s]*)-*\\d+\\.*\\w*(?![.\\w\\s]*[\\\"\\'])");
	
	protected String replace_constants(String code) {
		return replaceTextOfMatchGroup(code, _CONSTANT_REGEX, match1 -> {
			String constant = match1.group(0);

			constant = replaceValue(constant, Math.PI, "Math.PI");
			constant = replaceValue(constant, (float)Math.PI, "(float)Math.PI");
			constant = replaceValue(constant, (float)Math.PI / 2F, "(float)Math.PI / 2F");
			constant = replaceValue(constant, (float)Math.PI / 4.5F, "(float)Math.PI / 4.5F");
			constant = replaceValue(constant, (double)(float)Math.PI, "(double)(float)Math.PI");
			constant = replaceValue(constant, Math.PI * 2D, "Math.PI * 2D");
			constant = replaceValue(constant, Math.PI / 2D, "Math.PI / 2D");
			for (int i = 1; i <= 100; i++) {
				double d = i * 0.01D;
				if(d != (double)(float)d) { // if imprecise
					constant = floatCastedToDouble(constant, (float)d);
				}
			}
			constant = floatCastedToDouble(constant, 0.0075F);
			constant = floatCastedToDouble(constant, 0.999F);
			constant = floatCastedToDouble(constant, 0.997F);
			constant = floatCastedToDouble(constant, 1.62F);
			constant = replaceValue(constant, 0xFFFFFF, "0xFFFFFF"); // TODO Might do this in fernflower at some point
			constant = replaceValue(constant, 0x20200000, "0x20200000");
			constant = replaceValue(constant, 0x20400000, "0x20400000");
			constant = replaceValue(constant, 0xFF000000, "0xFF000000");
			//brugh
			constant = replaceValue(constant, 2.0D / 256D, "2.0D / 256D");
			constant = replaceValue(constant, 6.0D / 256D, "6.0D / 256D");
			constant = replaceValue(constant, 7.0D / 256D, "7.0D / 256D");
			constant = replaceValue(constant, 8.0D / 256D, "8.0D / 256D");
			constant = replaceValue(constant, 9.0D / 256D, "9.0D / 256D");
			return constant;
		});
	}
	
	private String floatCastedToDouble(String code, float value) {
		return code.replace((double)value + "D", "(double)" + value + "F");
	}
	
	private String replaceValue(String code, double value, String replace) {
		return code.replace(value + "D", replace);
	}
	
	private String replaceValue(String code, float value, String replace) {
		return code.replace(value + "F", replace);
	}
	
	private String replaceValue(String code, int value, String replace) {
		return code.replace(value + "", replace);
	}
	
	private String replaceValue(String code, long value, String replace) {
		return code.replace(value + "L", replace);
	}
}
