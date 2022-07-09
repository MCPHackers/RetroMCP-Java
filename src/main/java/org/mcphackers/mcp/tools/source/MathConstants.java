package org.mcphackers.mcp.tools.source;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MathConstants extends Constants {
	
	// Used to prevent strings from being captured, such as "2.0D"
	private static final Pattern _CONSTANT_REGEX = Pattern.compile("(?![\"'][.\\w\\s]*)-*\\d+\\.*\\w*(?![.\\w\\s]*[\"'])");
	private static final Map<String, String> _CONSTANTS = new HashMap<>();
	
	static {
		for (int i = 1; i <= 100; i++) {
			double d = i * 0.01D;
			if(d != (double)(float)d) { // if imprecise
				floatCastedToDouble((float)d);
			}
		}
		floatCastedToDouble(0.0075F);
		floatCastedToDouble(0.999F);
		floatCastedToDouble(0.997F);
		floatCastedToDouble(1.62F);
		replaceValue(Math.PI, "Math.PI");
		replaceValue((float)Math.PI, "(float)Math.PI");
		replaceValue((float)Math.PI / 2F, "(float)Math.PI / 2F");
		replaceValue((float)Math.PI / 4.5F, "(float)Math.PI / 4.5F");
		replaceValue((double)(float)Math.PI, "(double)(float)Math.PI");
		replaceValue(Math.PI * 2D, "Math.PI * 2D");
		replaceValue(Math.PI / 2D, "Math.PI / 2D");
		replaceValue(0xFFFFFF, "0xFFFFFF");
		replaceValue(0x20200000, "0x20200000");
		replaceValue(0x20400000, "0x20400000");
		replaceValue(0xFF000000, "0xFF000000");
		replaceValue(1.0D / 256D, "1.0D / 256D");
		replaceValue(2.0D / 256D, "2.0D / 256D");
		replaceValue(6.0D / 256D, "6.0D / 256D");
		replaceValue(7.0D / 256D, "7.0D / 256D");
		replaceValue(8.0D / 256D, "8.0D / 256D");
		replaceValue(9.0D / 256D, "9.0D / 256D");
	}
	
	protected String replace_constants(String code) {
		return Source.replaceTextOfMatchGroup(code, _CONSTANT_REGEX, match1 -> {
			String constant = match1.group(0);
			return _CONSTANTS.getOrDefault(constant, constant);
		});
	}
	
	private static String floatCastedToDouble(float value) {
		return _CONSTANTS.put((double)value + "D", "(double)" + value + "F");
	}
	
	private static String replaceValue(double value, String replace) {
		return _CONSTANTS.put(value + "D", replace);
	}
	
	private static String replaceValue(float value, String replace) {
		return _CONSTANTS.put(value + "F", replace);
	}
	
	private static String replaceValue(int value, String replace) {
		return _CONSTANTS.put(String.valueOf(value), replace);
	}
	
	@SuppressWarnings("unused")
	private static String replaceValue(long value, String replace) {
		return _CONSTANTS.put(value + "L", replace);
	}
}
