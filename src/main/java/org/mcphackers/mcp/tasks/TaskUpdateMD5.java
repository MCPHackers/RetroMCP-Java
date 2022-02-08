package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.ProgressInfo;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;

public class TaskUpdateMD5 extends Task {
	private int total;
	private int progress;
	public boolean recompile;
	private TaskRecompile recompTask;
	
	private static final int RECOMPILE = 1;
	private static final int MD5 = 2;
	
	
	public TaskUpdateMD5(int side, TaskInfo info) {
		super(side, info);
		this.total = 1;
		this.progress = 0;
		this.recompile = true;
		this.recompTask = new TaskRecompile(side, info);
	}

	@Override
	public void doTask() throws Exception {
		updateMD5(false);
	}

	public void updateMD5(boolean reobf) throws Exception {
		Path binPath 	= Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, MCPConfig.SERVER_BIN));
		Path md5 = Paths.get(reobf ? chooseFromSide(MCPConfig.CLIENT_MD5_RO, MCPConfig.SERVER_MD5_RO)
				  				   : chooseFromSide(MCPConfig.CLIENT_MD5, 	 MCPConfig.SERVER_MD5));
		step();
		if(recompile) {
			new TaskRecompile(side, info).doTask();
		}
		step();
		if (Files.exists(binPath)) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(md5.toFile()));
			this.total = (int)Files.walk(binPath)
					.parallel()
					.filter(p -> !p.toFile().isDirectory())
					.count();
			Files.walkFileTree(binPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						String md5_hash = Util.getMD5OfFile(file.toFile());
						String fileName = Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, MCPConfig.SERVER_BIN)).relativize(file).toString().replace("\\", "/").replace(".class", "");
						writer.append(fileName).append(" ").append(md5_hash).append("\n");
						progress++;
					} catch (NoSuchAlgorithmException ex) {
						ex.printStackTrace();
					}
					return FileVisitResult.CONTINUE;
				}
			});
			writer.close();
		} else {
			throw new IOException(chooseFromSide("Client", "Server") + " classes not found!");
		}
	}

	@Override
	public ProgressInfo getProgress() {
		int total = 100;
		int current = 0;
		switch(step) {
		case RECOMPILE: {
			current = 1;
			ProgressInfo info = recompTask.getProgress();
			int percent = (int) ((double)info.getCurrent() / info.getTotal() * 2);
			return new ProgressInfo(info.getMessage(), current + percent, total); }
		case MD5:
			current = 50 + (int)((double)progress / this.total * 50);
			return new ProgressInfo("Updating MD5...", current, total);
		}
		return super.getProgress();
	}
}
