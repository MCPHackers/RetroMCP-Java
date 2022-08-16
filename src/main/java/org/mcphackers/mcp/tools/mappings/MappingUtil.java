package org.mcphackers.mcp.tools.mappings;

public final class MappingUtil {

	public static String getObfuscatedName(int number) {
		// Default obfuscation scheme
		return getObfuscatedName('a', 'z', number);
	}

	public static String getObfuscatedName(char from, char to, int number) {
		if(number == 0) {
			return String.valueOf(from);
		}
		int num = number;
		int allChars = to - from  + 1;
		StringBuilder retName = new StringBuilder();
		while(num >= 0) {
			char c = (char) (from + (num % allChars));
			retName.insert(0, c);
			num = num / allChars - 1;
		}
		return retName.toString();
	}

}
