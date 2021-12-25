package org.mcphackers.mcp.tasks.info;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.tasks.Task;

public abstract class TaskInfo {
	private List<String> completionInfo = new ArrayList<String>();

	public abstract String title();

	public abstract String successMsg();

	public abstract String failMsg();

	public abstract Task newTask(int side);

	public abstract boolean isMultiThreaded();

	public void clearInfoList() {
		this.completionInfo.clear();
	}

	public List<String> getInfoList() {
		return this.completionInfo;
	}

	public void addInfo(String err) {
		this.completionInfo.add(err);
	}
}
