package org.mcphackers.mcp;

import org.mcphackers.mcp.tools.versions.IDownload;

public interface DownloadListener {
	void notify(IDownload object, long totalSize);
}
