package jredfox.common.log.printer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;

import jredfox.common.io.IOUtils;

public class LogPrinter extends Printer {

	public LogWrapper out;
	public LogWrapper err;
	public volatile boolean hasStarted;
	
	public LogPrinter(File log, PrintStream out, PrintStream err) throws IOException
	{
		this(log, out, err, true, true);
	}

	public LogPrinter(File log, PrintStream out, PrintStream err, boolean cHeader, boolean logHeader) throws IOException
	{
		super(log);

		// reset the log file
		if(log.exists())
			log.delete();

		// set the streams
		if(out != null)
		{
			this.out = new LogPrinter.LogWrapper(this, out, false, cHeader, logHeader);
			System.setOut(this.out);
		}
		
		if(err != null)
		{
			this.err = new LogPrinter.LogWrapper(this, err, true, cHeader, logHeader);
			System.setErr(this.err);
		}
	}

	@Override
	public void load() throws IOException { this.setPrintWriter(); }
	@Override
	public void parse(String line) {}
	@Override
	public void save(BufferedWriter writer) {IOUtils.close(writer);}
	@Override
	public boolean contains(String key) {return false;}

	public class LogWrapper extends PrintStream {
		
		public LogPrinter printer;
		public PrintStream child;
		public final boolean isErr;
		public volatile boolean childHeader;
		public volatile boolean logHeader;

		public LogWrapper(LogPrinter printer, PrintStream out, boolean isErr, boolean cHeader, boolean lHeader)
		{
			super(out, true);
			this.printer = printer;
			this.child = out;
			this.isErr = isErr;
			this.childHeader = cHeader;
			this.logHeader = lHeader;
		}

		public boolean hasChildHeader() { return this.childHeader;}
		public boolean hasLogHeader(){ return this.logHeader; }
		public void setChildHeader(boolean b) { this.childHeader = b;}
		public void setLogHeader(boolean b) { this.logHeader = b; }
		
		@Override
		public void close()
		{
			IOUtils.close(this.child);
		}

		@Override
		public void print(boolean b)
		{
			this.print(String.valueOf(b));
		}

		@Override
		public void print(char c)
		{
			this.print(String.valueOf(c));
		}

		@Override
		public void print(int i)
		{
			this.print(String.valueOf(i));
		}

		@Override
		public void print(long l) 
		{
			this.print(String.valueOf(l));
		}

		@Override
		public void print(float f)
		{
			this.print(String.valueOf(f));
		}

		@Override
		public void print(double d) 
		{
			this.print(String.valueOf(d));
		}

		@Override
		public void print(Object obj)
		{
			this.print(String.valueOf(obj));
		}

		@Override
		public void print(char[] s) 
		{
			super.print(s);
			listen(String.valueOf(s));
		}

		@Override
		public PrintStream append(CharSequence csq, int start, int end)
		{
			listen(String.valueOf(csq));
			return super.append(csq, start, end);
		}

		@Override
		public void println(boolean x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(char x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(int x) 
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(long x) 
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(float x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(double x)
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(char[] x) 
		{
			this.println(String.valueOf(x));
		}

		@Override
		public void println(Object x)
		{
			this.println(String.valueOf(x));
		}
		
		@Override
		public void print(String s) 
		{
			listen(s);
		}
		
		@Override
		public void println()
		{
			this.listenln("");
		}

		@Override
		public void println(String x)
		{
			listenln(x);
		}
		
		public void listen(String text) 
		{
			boolean n = text.contains("\n");
			String[] lines = text.split("\n");
			if(lines.length == 0)
				lines = new String[]{text.replace("\n", "")};
			
			for(String s : lines)
			{
				this.writeDirect(s + (n ? "\n" : ""));
			}
		}

		public void listenln(String line) 
		{
			this.listen(line + "\n");
		}
		
		protected void writeDirect(String s)
		{
			String starter = this.printer.hasStarted ? "" : this.getLogMsg();
			this.child.print(this.childHeader ? (starter + s) : s);
			this.printer.print(this.logHeader ? (starter + s) : s);
			this.printer.hasStarted = !s.contains("\n");
		}

		public String getLogMsg()
		{
			return "[" + Instant.now() + "]" + " [" + (this.isErr ? "Err" : "STD") + "]" + ": ";
		}
	}

}
