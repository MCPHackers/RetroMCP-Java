package jredfox.terminal.app;

public interface ITerminalApp {
	
	/**
	 * called when launching or rebooting. Make sure to re-instantiate your TerminalApp configurable variables when this gets called
	 */
	public TerminalApp newApp(String[] args);

}
