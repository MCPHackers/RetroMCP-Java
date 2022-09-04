package org.mcphackers.mcp.tools.injector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.rdi.injector.visitors.ClassVisitor;
import org.mcphackers.rdi.util.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class GLConstants extends ClassVisitor {

	private static final JSONObject json = getJson();
	private static final List<String> PACKAGES = json == null ? Collections.emptyList() : toList(json.getJSONArray("PACKAGES"));
	private static final JSONArray CONSTANTS = json == null ? null : json.getJSONArray("CONSTANTS");
	private static final JSONObject CONSTANTS_KEYBOARD = json == null ? null : json.getJSONObject("CONSTANTS_KEYBOARD");
	
	private String className;
	
	private static JSONObject getJson() {
		try {
			return Util.parseJSON(GLConstants.class.getClassLoader().getResourceAsStream("gl_constants.json"));
		} catch (JSONException | IOException e) {
			return null;
		}
	}
	
	private static List<String> toList(JSONArray packages) {
		if(packages == null) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>();
		for(int i = 0; i < packages.length(); i++) {
			list.add(packages.optString(i));
		}
		return list;
	}
	
	private static boolean isOpenGLClass(String name) {
		for(String pkg : PACKAGES) {
			if(("org/lwjgl/opengl/" + pkg).equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public GLConstants(ClassVisitor classVisitor) {
		super(classVisitor);
	}
	
	protected void visitClass(ClassNode node) {
		className = node.name;
	}

	protected void visitMethod(MethodNode node) {
		if(json == null) return;
        
		InsnList instructions = node.instructions;
		List<MethodInsnNode> glCalls = new ArrayList<>();
		List<Pair<AbstractInsnNode, FieldInsnNode>> keyboardConstants = new ArrayList<>();
		for(AbstractInsnNode insn : instructions) {
			if(insn instanceof MethodInsnNode) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				if(isOpenGLClass(invoke.owner)) {
					glCalls.add(invoke);
				}
				if(invoke.owner.equals("org/lwjgl/input/Keyboard")) {
					if(invoke.name.equals("isKeyDown") || invoke.name.equals("getKeyName")) {
						AbstractInsnNode insn2 = invoke.getPrevious();
						if(insn2 == null) {
							continue;
						}
						Integer value = intValue(insn2);
						if(value == null) {
							continue;
						}
						FieldInsnNode getField = getKeyboardConstant(value);
						if(getField != null) {
							keyboardConstants.add(new Pair<>(insn2, getField));
						}
					}
					else if(invoke.name.equals("getEventKey")) {
						AbstractInsnNode insn2 = invoke.getNext();
						if(insn2 == null) {
							continue;
						}
						AbstractInsnNode insn3 = insn2.getNext();
						boolean hasCompare = false;
						int count = 0;
						// if next instruction is compare or any instruction + iadd and then compare
						while(insn3 != null && count < 3 && !hasCompare) {
							if(count == 1 && insn3.getOpcode() != Opcodes.IADD) break; 
							if(isIntComp(insn3.getOpcode())) hasCompare = true;
							count++;
							insn3 = insn3.getNext();
						}
						if(hasCompare) {
							Integer value = intValue(insn2);
							if(value != null) {
								FieldInsnNode getField = getKeyboardConstant(value);
								if(getField != null) {
									keyboardConstants.add(new Pair<>(insn2, getField));
								}
							}
						}
					}
				}
			}
		}
		
		for(Pair<AbstractInsnNode, FieldInsnNode> pair : keyboardConstants) {
			instructions.insert(pair.getLeft(), pair.getRight());
			instructions.remove(pair.getLeft());
		}
		
		try {
			if(glCalls.isEmpty()) return;
			IdentifyCall sources = IdentifyCall.getInputs(className, node);
			for(MethodInsnNode invoke : glCalls) {
	
				for(AbstractInsnNode insn : sources.getAllInputsOf(invoke)) {
					
					if(insn == null) {
						continue;
					}
					InsnList newinsns = null;
					Integer intValue = intValue(insn);
					if(intValue != null) {
						newinsns = getGLConstant(invoke, intValue);
						if(newinsns != null) {
							instructions.insert(insn, newinsns);
							instructions.remove(insn);
						}
					}
				}
			}
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean isIntComp(int opcode) {
		switch (opcode) {
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPNE:
			return true;
		}
		return false;
	}
	
	public static Integer intValue(AbstractInsnNode insn) {
		if(insn.getOpcode() == Opcodes.ICONST_0) {
			return 0;
		}
		if(insn.getOpcode() == Opcodes.ICONST_1) {
			return 1;
		}
		if(insn.getOpcode() == Opcodes.ICONST_2) {
			return 2;
		}
		if(insn.getOpcode() == Opcodes.ICONST_3) {
			return 3;
		}
		if(insn.getOpcode() == Opcodes.ICONST_4) {
			return 4;
		}
		if(insn.getOpcode() == Opcodes.ICONST_5) {
			return 5;
		}
		if(insn.getOpcode() == Opcodes.ICONST_M1) {
			return -1;
		}
		if(insn.getOpcode() == Opcodes.SIPUSH || insn.getOpcode() == Opcodes.BIPUSH) {
			return ((IntInsnNode)insn).operand;
		}
		if(insn.getOpcode() == Opcodes.LDC) {
			LdcInsnNode ldc = (LdcInsnNode)insn;
			if(ldc.cst instanceof Integer) {
				return (Integer)ldc.cst;
			}
		}
		return null;
	}
	
	public static FieldInsnNode getKeyboardConstant(int constant) {
		String constantString = CONSTANTS_KEYBOARD.optString(String.valueOf(constant), null);
		return constantString == null ? null : new FieldInsnNode(Opcodes.GETSTATIC, "org/lwjgl/input/Keyboard", constantString, "I");
	}
	
	public static InsnList getGLConstant(MethodInsnNode invoke, int constant) {
		String constantKey = String.valueOf(constant);
		String pkg = invoke.owner.replace("org/lwjgl/opengl/", "");
		for (Object groupg : CONSTANTS) {
			if(!(groupg instanceof JSONArray)) {
				continue;
			}
			JSONArray group = (JSONArray)groupg;
			JSONObject jsonObj1 = group.getJSONObject(0);
			if (jsonObj1.has(pkg) && jsonObj1.getJSONArray(pkg).toList().contains(invoke.name)) {
				JSONObject jsonObj = group.getJSONObject(1);
				Iterator<String> keys = jsonObj.keys();
				while(keys.hasNext()) {
					String key = keys.next();
					JSONObject value = jsonObj.getJSONObject(key);
					if(value.has(constantKey)) {
						String constantString = value.getString(constantKey);
						String[] constants = constantString.split("\\|");
						InsnList instructions = new InsnList();
						instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "org/lwjgl/opengl/" + key, constants[0].trim(), "I"));
						for(int i = 1; i < constants.length; i++) {
							instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "org/lwjgl/opengl/" + key, constants[i].trim(), "I"));
							instructions.add(new InsnNode(Opcodes.IOR));
						}
						return instructions;
					}
				}
			}
		}
		return null;
	}
}
