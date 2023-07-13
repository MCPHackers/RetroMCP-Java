package org.mcphackers.mcp.api.command;

import org.mcphackers.mcp.api.command.task.Task;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
	private final List<Command> commands = new ArrayList<>();

	public List<Command> getCommands() {
		return this.commands;
	}

	public void clearCommands() {
		this.commands.clear();
	}

	public boolean addCommand(Command command) {
		if (command instanceof Task) {
			return false;
		}
		return this.commands.add(command);
	}

	public boolean removeCommand(Command command) {
		return this.commands.remove(command);
	}
}
