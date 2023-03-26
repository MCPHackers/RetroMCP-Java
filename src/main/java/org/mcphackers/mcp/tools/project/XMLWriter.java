package org.mcphackers.mcp.tools.project;

import java.io.BufferedWriter;
import java.io.IOException;

public class XMLWriter extends BufferedWriter {

	private int indent;

	public XMLWriter(BufferedWriter writer) {
		super(writer);
	}

	private void appendInd(StringBuilder s) {
		for(int i = 0; i < indent; i++) {
			s.append("\t");
		}
	}

	public void writeln(String s) throws IOException {
		StringBuilder b = new StringBuilder();
		appendInd(b);
		b.append(s);
		super.write(b.toString());
		newLine();
	}

	public void writeAttribute(String attribute) throws IOException {
		writeln("<" + attribute + "/>");
	}

	public void startAttribute(String attribute) throws IOException {
		writeln("<" + attribute + ">");
		indent++;
	}

	public void writeSelfEndingAttribute(String attribute) throws IOException {
		writeln("<" + attribute + " />");
	}

	public void closeAttribute(String attribute) throws IOException {
		indent--;
		writeln("</" + attribute + ">");
	}

	public void stringAttribute(String attribute, String value) throws IOException {
		writeln("<" + attribute + ">" + value + "</" + attribute + ">");
	}

}
