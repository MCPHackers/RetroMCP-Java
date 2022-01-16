package jredfox.common.thread;

public abstract class ShutdownThread extends Thread {
	
	public ShutdownThread()
	{
		this.setDaemon(true);
	}
	
	@Override
	public void run()
	{
		super.run();
		this.shutdown();
	}

	/**
	 * normal shutdown method CTRL+CLOSE or CTRL+BREAK signal. if the program hangs on shutdown it will stay that way until the os decides to give a SIGTERM or SIGKILL
	 */
	public abstract void shutdown();
	
	/**
	 * warning you have about 2s-3s to handle SIGTERM signal before the process is terminated
	 * this won't fire if SIGKILL happens
	 */
	public abstract void kill();
}
