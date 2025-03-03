package org.mcphackers.mcp.tools.versions;

public interface IDownload {

	String downloadPath();

	String downloadURL();

	long downloadSize();

	String downloadHash();

	boolean verify();
}
