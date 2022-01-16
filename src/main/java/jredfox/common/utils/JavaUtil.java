package jredfox.common.utils;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JavaUtil {
	
	public static boolean containsAny(String string, String invalid) 
	{
		if(string.isEmpty())
			return invalid.isEmpty();
		
		for(int i=0; i < string.length(); i++)
		{
			String s = string.substring(i, i + 1);
			if(invalid.contains(s))
			{
				return true;
			}
		}
		return false;
	}
	
	public static String getLastSplit(String str, String sep) 
    {
        String[] arr = str.split(sep);
        return arr[arr.length - 1];
    }
	
	public static <T> T[] toArray(Collection<T> col, Class<T> clazz)
	{
	    @SuppressWarnings("unchecked")
		T[] li = (T[]) Array.newInstance(clazz, col.size());
	    int index = 0;
	    for(T obj : col)
	    {
	        li[index++] = obj;
	    }
	    return li;
	}
	
	public static String[] splitFirst(String str, char sep, char lquote, char rquote)
	{
		return split(str, 1, sep, lquote, rquote);
	}
	
	public static String[] split(String str, char sep, char lquote, char rquote) 
	{
		return split(str, -1, sep, lquote, rquote);
	}
	
	/**
	 * split with quote ignoring support
	 * @param limit is the amount of times it will attempt to split
	 * TODO: make it work with multiple quotes
	 */
	public static String[] split(String str, int limit, char sep, char lquote, char rquote) 
	{
		if(str.isEmpty())
			return new String[]{str};
		List<String> list = new ArrayList<>();
		boolean inside = false;
		int count = 0;
		for(int i = 0; i < str.length(); i += 1)
		{
			if(limit != -1 && count >= limit)
				break;
			String a = str.substring(i, i + 1);
			char firstChar = a.charAt(0);
			char prev = i == 0 ? 'a' : str.substring(i-1, i).charAt(0);
			boolean escape = prev == '\\';
			if(firstChar == '\\' && prev == '\\')
			{
				prev = '/';
				firstChar = '/';//escape the escape
			}
			if(!escape && (a.equals("" + lquote) || a.equals("" + rquote)))
			{
				inside = !inside;
			}
			if(a.equals("" + sep) && !inside)
			{
				String section = str.substring(0, i);
				list.add(section);
				str = str.substring(i + ("" + sep).length());
				i = -1;
				count++;
			}
		}
		list.add(str);//add the rest of the string
		return toArray(list, String.class);
	}
	
	public static String getExtensionFull(File file) 
	{
		String ext = FileUtil.getExtension(file);
		return ext.isEmpty() ? "" : "." + ext;
	}
	
	public static String inject(String str, char before, char toInject)
	{
		int index = str.indexOf(before);
		return index != -1 ? str.substring(0, index) + toInject + str.substring(index) : str;
	}
	
	public static String parseQuotes(String s, char lq, char rq) 
	{
		return parseQuotes(s, 0, lq, rq);
	}

	public static String parseQuotes(String s, int index, char lq, char rq)
	{
		StringBuilder builder = new StringBuilder();
		char prev = 'a';
		int count = 0;
		boolean hasQuote = hasQuote(s.substring(index, s.length()), lq);
		for(int i=index;i<s.length();i++)
		{
			String c = s.substring(i, i + 1);
			char firstChar = c.charAt(0);
			if(firstChar == '\\' && prev == '\\')
			{
				prev = '/';
				firstChar = '/';//escape the escape
			}
			boolean escaped = prev == '\\';
			if(hasQuote && !escaped && (count == 0 && c.equals("" + lq) || count == 1 && c.equals("" + rq)))
			{
				count++;
				if(count == 2)
					break;
				prev = firstChar;//set previous before skipping
				continue;
			}
			if(!hasQuote || count == 1)
			{
				builder.append(c);
			}
			prev = firstChar;//set the previous char here
		}
		return lq == rq ? builder.toString().replaceAll("\\\\" + lq, "" + lq) : builder.toString().replaceAll("\\\\" + lq, "" + lq).replaceAll("\\\\" + rq, "" + rq);
	}

	public static boolean hasQuote(String str, char lq) 
	{
		char prev = 'a';
		for(char c : str.toCharArray())
		{
			if(c == lq && prev != '\\')
				return true;
			prev = c;
		}
		return false;
	}

	public static <V> V getFirst(Map<?, V> m)
	{
		for(Object k : m.keySet())
		{
			return m.get(k);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> void setFirst(LinkedHashMap<K, V> map, V v) 
	{
		map.put((K) JavaUtil.getFirst(map), v);
	}

	public static void removeStarts(List<String> list, String match, boolean all)
	{
		Iterator<String> i = list.iterator();
		while(i.hasNext())
		{
			String s = i.next();
			if(s.startsWith(match))
			{
				i.remove();
				if(!all)
					return;
			}
		}
	}

	public static <T> List<T> asArray(T[] li) 
	{
		ArrayList<T> arr = new ArrayList<>(li.length);
		for(T t : li)
			arr.add(t);
		return arr;
	}
	
	public static <T> List<T> asArray(Collection<T> li) 
	{
		ArrayList<T> arr = new ArrayList<>(li.size());
		arr.addAll(li);
		return arr;
	}

	public static String toCommand(Collection<String> args) 
	{
		StringBuilder b = new StringBuilder();
		int index = 0;
		for(String s : args)
		{
			b.append("" + s + "" + (index + 1 == args.size() ? "" : " "));
			index++;
		}
		return b.toString();
	}
	
	public static String codepointToString(int cp) 
	{
	    StringBuilder sb = new StringBuilder();
	    if (Character.isBmpCodePoint(cp)) 
	    	sb.append((char) cp);
	    else if (Character.isValidCodePoint(cp))
	    {
	       sb.append(Character.highSurrogate(cp));
	       sb.append(Character.lowSurrogate(cp));
	    }
	    else
	    	sb.append('?');
	   return sb.toString();
	}
	
	/**
	 * get the codepoint from the unicode number. from there you can convert it to a unicode escape sequence using {@link JavaUtil#getUnicodeEsq(int)}
	 * "U+hex" for unicode number
	 * "&#codePoint;" or "&#hex;" for html
	 * "\hex" for css
	 * "hex" for lazyness
	 */
	public static int parseUnicodeNumber(String num)
	{
		num = num.toLowerCase();
		if(num.startsWith("u+"))
			num = num.substring(2);
		else if(num.startsWith("&#"))
			return num.startsWith("&#x") ? Integer.parseInt(num.substring(3, num.length() - 1), 16) : Integer.parseInt(num.substring(2, num.length() - 1)); 
		else if(num.startsWith("\\"))
			num = num.substring(1);
		return Integer.parseInt(num, 16);
	}
	
	/**
	 * convert a unicode number directly to unicode escape sequence in java
	 */
	public static String unicodeNumberToEsq(String num)
	{
		return toUnicodeEsq(parseUnicodeNumber(num));
	}
	
	/**
	 * return the java unicode string from the utf-8 string
	 * TODO: add an option to change the unicode number strings to not just the codepoints
	 * ascii esq in java
	 * ascii un-escape in java
	   unicode un-escape in java
	   escape in java does both unicode and ascii
	   un-escape in java does both unicode and ascii
	 */
	public static String toUnicodeEsq(String unicode)
	{
		StringBuilder b = new StringBuilder();
		int[] arr = unicode.codePoints().toArray();
		for(int i : arr)
			b.append(toUnicodeEsq(i));
		return b.toString();
	}
	
	public static String toUnicodeEsq(int cp)
	{
		return isAscii(cp) ? "" + (char) cp : Character.isBmpCodePoint(cp) ? "\\u" + String.format("%04x", cp) : "\\u" + String.format("%04x", (int)Character.highSurrogate(cp)) + "\\u" + String.format("%04x", (int)Character.lowSurrogate(cp) );
	}
	
	public static boolean isAscii(char c)
	{
		return isAscii((int)c);
	}

	public static boolean isAscii(int cp) 
	{
		return cp <= Byte.MAX_VALUE;
	}

	public static String[] parseCommandLine(String line)
	{
		return parseCommandLine(line, '\\', '"');
	}

	public static String[] parseCommandLine(String line, char esq, char q)
	{
		List<String> args = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		String previous = "";
		boolean quoted = false;
		String replaceEsq = esq == '\\' ? "\\\\" : "" + esq;
		for(int index = 0; index < line.length(); index++)
		{
			String character = line.substring(index, index + 1);
			String compare = character;
			
			//escape the escape sequence
			if(previous.equals("" + esq) && compare.equals("" + esq))
			{
				previous = "aa";
				compare = "aa";
			}
			
			boolean escaped = previous.equals("" + esq);
			
			if(!escaped && compare.equals("" + q))
				quoted = !quoted;
			
			if(!quoted && compare.equals(" "))
			{
				args.add(replaceAll(builder.toString(), q, "", esq).replaceAll(replaceEsq + q, "" + q));
				builder = new StringBuilder();
				previous = compare;
				continue;
			}
			builder.append(character);
			previous = compare;
		}
		if(!builder.toString().isEmpty())
			args.add(replaceAll(builder.toString(), q, "", esq).replaceAll(replaceEsq + q, "" + q));
		
		return JavaUtil.toArray(args, String.class);
	}

	public static String replaceAll(String str, char what, String with, char esq)
	{
		StringBuilder builder = new StringBuilder();
		String previous = "";
		for(int index = 0; index < str.length(); index++)
		{
			String character = str.substring(index, index + 1);
			if(previous.equals("" + esq) && character.equals("" + esq))
			{
				previous = "aa";
				character = "aa"; 
			}
			boolean escaped = previous.equals("" + esq);
			previous = character;
			if(!escaped && character.equals("" + what))
				character = with;
			builder.append(character);
		}
		return builder.toString();
	}

}
