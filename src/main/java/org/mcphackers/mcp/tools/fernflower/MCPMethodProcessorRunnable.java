package org.mcphackers.mcp.tools.fernflower;

import de.fernflower.main.DecompilerContext;
import de.fernflower.main.rels.MethodProcessorRunnable;
import de.fernflower.modules.decompiler.vars.VarProcessor;
import de.fernflower.struct.StructMethod;

public class MCPMethodProcessorRunnable extends MethodProcessorRunnable {
    public MCPMethodProcessorRunnable(StructMethod method, VarProcessor varProc, DecompilerContext parentContext) {
        super(method, varProc, parentContext);
    }
}
