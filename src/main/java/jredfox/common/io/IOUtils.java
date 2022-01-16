package jredfox.common.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {
	
	public static final byte[] buffer = new byte[1048576/2];
	
	public static void copy(InputStream in, OutputStream out) throws IOException
	{
		copy(in, out, true);
	}
	
	public static void copy(InputStream in, OutputStream out, boolean close) throws IOException
	{
		int length;
   	 	while ((length = in.read(buffer)) > 0)
		{
			out.write(buffer, 0, length);
		}
   	 	if(close)
   	 	{
   	 		in.close();
   	 		out.close();
   	 	}
	}
	
	public static void closeQuietly(Closeable clos)
	{
		close(clos, false);
	}
	
	public static void close(Closeable clos)
	{
		close(clos, true);
	}

	public static void close(Closeable clos, boolean print)
	{
		try 
		{
			if(clos != null)
				clos.close();
		}
		catch (IOException e)
		{
			if(print)
				e.printStackTrace();
		}
	}
	
	/**
	 * Overwrites entire file default behavior no per line modification removal/addition
	 */
	public static void saveFileLines(List<String> list, File f, boolean utf8)
	{
		makeParentDirs(f);
		BufferedWriter writer = null;
		try
		{
			if(!utf8)
			{
				writer = new BufferedWriter(new FileWriter(f));
			}
			else
			{
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8 ) );
			}
			
			for(String s : list)
			{
				writer.write(s + System.lineSeparator());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			close(writer);
		}
	}
	
	public static void makeParentDirs(File f)
	{
		File parent = f.getParentFile();
		if(!parent.exists())
			parent.mkdirs();
	}

	public static void makeExe(File f) throws IOException
	{
		f.setReadable(true);
		f.setWritable(true);
		f.setExecutable(true);
	}

	/**
	 * Equivalent to Files.readAllLines() but, works way faster
	 */
	public static List<String> getFileLines(File f)
	{
		return getFileLines(getReader(f));
	}
	
	public static List<String> getFileLines(String input) 
	{
		return getFileLines(getReader(input));
	}
	
	public static List<String> getFileLines(BufferedReader reader) 
	{
		List<String> list = null;
		try
		{
			list = new ArrayList<>();
			String s = reader.readLine();
			
			if(s != null)
			{
				list.add(s);
			}
			
			while(s != null)
			{
				s = reader.readLine();
				if(s != null)
				{
					list.add(s);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.close(reader);
		}
		return list;
	}
	
	/**
	 * even though it's utf8 writing it's the fastest one I tried 5 different other options from different objects
	 */
	public static BufferedWriter getWriter(File f) throws FileNotFoundException, IOException
	{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
	}
	
	public static BufferedReader getReader(File f)
	{
		 try
		 {
			 return new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
		 }
		 catch(Throwable t)
		 {
			 return null;
		 }
	}
	
	public static BufferedReader getReader(String input)
	{
		return new BufferedReader(new InputStreamReader(IOUtils.class.getClassLoader().getResourceAsStream(input)));
	}

	public static void deleteDirectory(File file)
	{
		if(!file.exists())
			return;
	    if(file.isDirectory())
	        for(File f : file.listFiles()) 
	            if(!Files.isSymbolicLink(f.toPath())) 
	            	deleteDirectory(f);
	    
	    if(!file.delete())
	    	System.err.println("unable to delete file:" + file);
	}

}
