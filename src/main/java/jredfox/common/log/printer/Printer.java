package jredfox.common.log.printer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import jredfox.common.io.IOUtils;

public abstract class Printer implements Closeable{

	//Object vars
	public File logDir;
	public String logDirPath;
	public boolean dirty;
	
	//what matters
	public File log;
	public PrintWriter out;
	protected boolean isLoading;
	
	public Printer(File log) throws IOException 
	{
		log = log.getAbsoluteFile();
		this.logDir = log.getParentFile();
		this.logDirPath = logDir.getPath();
		this.log = log;
		this.sanityCheck();
	}
	
	public abstract void parse(String line);
	public abstract void save(BufferedWriter writer);
	public abstract boolean contains(String key);
	
	public void load() throws IOException
	{
		this.setPrintWriter();
		BufferedReader reader = IOUtils.getReader(this.log);
		this.isLoading = true;
		try
		{
			String s = reader.readLine();	
			if(s != null)
			{
				s = s.trim();
				if(!s.isEmpty())
					this.parse(s);
			}
			while(s != null)
			{
				s = reader.readLine();
				if(s != null)
				{
					s = s.trim();
					if(!s.isEmpty())
						this.parse(s);
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
		try
		{
			if(this.dirty) 
				this.save();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		this.isLoading = false;
	}
	
	public void save() throws FileNotFoundException, IOException
	{
		this.save(getWriter(this.log));
		this.dirty = false;
	}
	
	public static BufferedWriter getWriter(File file)
	{
		try
		{
			return IOUtils.getWriter(file);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public void setPrintWriter() throws IOException 
	{
		this.sanityCheck();
		if(this.out != null)
			IOUtils.close(this.out);
		this.out = new PrintWriter(new BufferedWriter(new FileWriter(this.log, true)), true);
	}

	public void sanityCheck() throws IOException 
	{
		if(!this.logDir.exists() && !this.logDir.mkdirs())
		{
			throw new IOException("Log Directory cannot be found nor created for:" + this.log);
		}
	}

	public File getLog()
	{
		return this.log;
	}
	
	/**
	 * appends it in memory and prints it to the file
	 */
	public void append(String line)
	{
		this.parse(line);
		this.println(line);
	}
	
	public void println(Object obj)
	{
		this.println(String.valueOf(obj));
	}
	
	public void print(String line)
	{
		this.out.print(line);
    	this.out.flush();
	}
	
	public void println(String line)
	{
		this.out.println(line);
	}

	@Override
	public void close()
	{
		IOUtils.close(this.out);
	}

}
