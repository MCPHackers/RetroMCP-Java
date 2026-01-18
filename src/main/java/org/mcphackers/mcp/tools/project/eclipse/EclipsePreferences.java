package org.mcphackers.mcp.tools.project.eclipse;

import java.io.IOException;

import org.mcphackers.mcp.tools.project.PairWriter;

public class EclipsePreferences {
	private String sourceVer;

	public void setSourceVersion(String sourceVer) {
		this.sourceVer = sourceVer;
	}

	public String getSourceVer() {
		return this.sourceVer;
	}

	public void toString(PairWriter writer) throws IOException {
		writer.writePair("eclipse.preferences.version", 1);
		writer.writePair("org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode", "enabled");
		writer.writePair("org.eclipse.jdt.core.compiler.codegen.methodParameters", "do not generate");
		writer.writePair("org.eclipse.jdt.core.compiler.codegen.targetPlatform", this.getSourceVer());
		writer.writePair("org.eclipse.jdt.core.compiler.codegen.unusedLocal", "preserve");
		writer.writePair("org.eclipse.jdt.core.compiler.compliance", this.getSourceVer());
		writer.writePair("org.eclipse.jdt.core.compiler.debug.lineNumber", "generate");
		writer.writePair("org.eclipse.jdt.core.compiler.debug.localVariable", "generate");
		writer.writePair("org.eclipse.jdt.core.compiler.debug.sourceFile", "generate");
		writer.writePair("org.eclipse.jdt.core.compiler.problem.assertIdentifier", "error");
		writer.writePair("org.eclipse.jdt.core.compiler.problem.enablePreviewFeatures", "disabled");
		writer.writePair("org.eclipse.jdt.core.compiler.problem.enumIdentifier", "error");
		writer.writePair("org.eclipse.jdt.core.compiler.problem.reportPreviewFeatures", "warning");
		writer.writePair("org.eclipse.jdt.core.compiler.release", "disabled");
		writer.writePair("org.eclipse.jdt.core.compiler.source", this.getSourceVer());
	}
}
