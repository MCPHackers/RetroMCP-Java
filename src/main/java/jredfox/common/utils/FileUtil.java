package jredfox.common.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class FileUtil {
	
	public static File getFile(URL url)
	{
		try 
		{
			return new File(url.toURI());
		}
		catch (URISyntaxException e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getTrueName(File file)
	{
		String name = file.getName();
		if(file.isDirectory())
			return name;
		int index = name.lastIndexOf('.');
		return index != -1 ? name.substring(0, index) : name;
	}
	
	public static String getRealtivePath(File dir, File file) 
	{
		String path = file.getPath();
		String dirPath = dir.getPath();
		boolean plus1 = path.contains(File.separator);
		return path.substring(path.indexOf(dirPath) + dirPath.length() + (plus1 ? 1 : 0));
	}
	
	public static String getExtensionFull(File file) 
	{
		String ext = getExtension(file);
		return ext.isEmpty() ? "" : "." + ext;
	}

	/**
	 * get a file extension. Note directories do not have file extensions
	 */
	public static String getExtension(File file) 
	{
		String name = file.getName();
		int index = name.lastIndexOf('.');
		return index != -1 && !file.isDirectory() ? name.substring(index + 1) : "";
	}


}
