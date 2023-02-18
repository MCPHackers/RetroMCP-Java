package org.mcphackers.mcp.tools.injector;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class TransformUtil {

	public static final void fixTryCatchRange(ClassStorage storage) {
		for(ClassNode node : storage) {
			for(MethodNode m : node.methods) {
				for(TryCatchBlockNode tryCatch : m.tryCatchBlocks) {
					AbstractInsnNode endNode = tryCatch.handler.getPrevious();
					if(endNode.getOpcode() == Opcodes.RETURN || endNode.getOpcode() == Opcodes.GOTO) {
						if(endNode.getPrevious() != tryCatch.end) {
							System.out.println("Found a bad try catch in " + m.name + m.desc + " at index " + m.tryCatchBlocks.indexOf(tryCatch));
						}
					}
				}
			}
		}
	}	
}
