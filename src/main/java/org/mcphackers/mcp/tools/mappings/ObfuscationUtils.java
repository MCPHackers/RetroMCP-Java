package org.mcphackers.mcp.tools.mappings;

public class ObfuscationUtils {

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
		String retName = "";
		while(num >= 0) {
			char c = Character.valueOf((char)(from + (num % allChars)));
			retName = c + retName;
			num = num / allChars - 1;
		}
		return retName;
	}
	
}
