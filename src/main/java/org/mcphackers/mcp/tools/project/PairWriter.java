package org.mcphackers.mcp.tools.project;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class PairWriter extends BufferedWriter {
	public PairWriter(Writer out) {
		super(out);
	}

	public void writePair(String key, String value) throws IOException {
		this.write(key + "=" + value + System.lineSeparator());
	}

	public void writePair(String key, int value) throws IOException {
		this.write(key + "=" + value + System.lineSeparator());
	}
}
