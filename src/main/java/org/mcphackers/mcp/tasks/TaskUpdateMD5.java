package org.mcphackers.mcp.tasks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tools.Util;

public class TaskUpdateMD5 extends TaskStaged {
	/*
	 * Indexes of stages for plugin overrides
	 */
	public static final int STAGE_RECOMPILE = 0;
	public static final int STAGE_MD5 = 1;

	private int progress = 0;

	public TaskUpdateMD5(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskUpdateMD5(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[] {
			stage(getLocalizedStage("recompile"),
			() -> {
				new TaskRecompile(side, mcp, this).doTask();
			}),
			stage(getLocalizedStage("updatemd5"), 50,
			() -> {
				updateMD5(false);
			})
		};
	}
	
	public void setProgress(int progress) {
		switch (step) {
		case 0: {
			int percent = (int)((double)progress * 0.50D);
			super.setProgress(percent);
			break;
		}
		default:
			super.setProgress(progress);
			break;
		}
	}

	public void updateMD5(boolean reobf) throws IOException {
		final Path binPath 	= MCPPaths.get(mcp, MCPPaths.COMPILED, side);
		final Path md5 = MCPPaths.get(mcp, reobf ? MCPPaths.MD5_RO : MCPPaths.MD5, side);

		if (!Files.exists(binPath)) {
			throw new IOException(side.name + " classes not found!");
		}
		BufferedWriter writer = Files.newBufferedWriter(md5);
		progress = 0;
		int total;
		try(Stream<Path> pathStream = Files.walk(binPath)) {
			total = (int) pathStream.parallel()
					.filter(p -> !p.toFile().isDirectory())
					.count();
		}
		Files.walkFileTree(binPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String md5_hash = Util.getMD5(file);
				String fileName = binPath.relativize(file).toString().replace("\\", "/").replace(".class", "");
				writer.append(fileName).append(" ").append(md5_hash).append("\n");
				progress++;
				setProgress(50 + (int)((double)progress/(double)total * 50));
				return FileVisitResult.CONTINUE;
			}
		});
		writer.close();
	}
}
