package org.mcphackers.mcp.tools.injector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.rdi.injector.visitors.ClassVisitor;
import org.mcphackers.rdi.util.IdentifyCall;
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

public final class GLConstants extends ClassVisitor {

	private static final boolean INIT;
	private static final List<Pair<Map<String, List<String>>, Map<String, Map<Integer, String>>>> CONSTANTS;
	private static final Map<Integer, String> CONSTANTS_KEYBOARD;
	private static final List<String> PACKAGES;
	private static final char[] OPERATORS = {'|', '&', '^'};

	static {
		JSONObject json = getJson();
		if(json != null) {
			CONSTANTS = getConstants(json.optJSONArray("CONSTANTS"));
			CONSTANTS_KEYBOARD = toMap(json.optJSONObject("CONSTANTS_KEYBOARD"));
			PACKAGES = getPackages(CONSTANTS);
			INIT = true;
		} else {
			CONSTANTS = null;
			CONSTANTS_KEYBOARD = null;
			PACKAGES = null;
			INIT = false;
		}
	}

	private ClassNode classNode;

	public GLConstants(ClassVisitor classVisitor) {
		super(classVisitor);
	}

	@Override
	protected void visitClass(ClassNode node) {
		classNode = node;
	}

	@Override
	protected void visitMethod(MethodNode node) {
		if(!INIT) return;
		InsnList instructions = node.instructions;
		List<MethodInsnNode> glCalls = new ArrayList<>();
		List<Pair<AbstractInsnNode, FieldInsnNode>> keyboardConstants = new ArrayList<>();
		for(AbstractInsnNode insn : instructions) {
			if(insn instanceof MethodInsnNode) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				if(PACKAGES.contains(invoke.owner)) {
					glCalls.add(invoke);
				}
				if(invoke.owner.equals("org/lwjgl/input/Keyboard")) {
					if(invoke.name.equals("isKeyDown") || invoke.name.equals("getKeyName")) {
						AbstractInsnNode iconst = invoke.getPrevious();
						if(iconst == null) {
							continue;
						}
						Integer value = intValue(iconst);
						if(value != null) {
							FieldInsnNode getField = getKeyboardInsn(value);
							if(getField != null) {
								keyboardConstants.add(Pair.of(iconst, getField));
							}
						}
					}
					else if(invoke.name.equals("getEventKey")) {
						AbstractInsnNode iconst = invoke.getNext();
						if(iconst == null) {
							continue;
						}
						AbstractInsnNode insn2 = iconst.getNext();
						// INVOKE, ICONST, (ANY INSTRUCTION), IADD, ICMP
						// or
						// INVOKE, ICONST, ICMP
						boolean hasCompare = false;
						int count = 0;
						while(insn2 != null && count < 3 && !hasCompare) {
							if(count == 1 && insn2.getOpcode() != Opcodes.IADD)
								break;
							if(isICmp(insn2.getOpcode()))
								hasCompare = true;
							count++;
							insn2 = insn2.getNext();
						}
						if(hasCompare) {
							Integer value = intValue(iconst);
							if(value != null) {
								FieldInsnNode getField = getKeyboardInsn(value);
								if(getField != null) {
									keyboardConstants.add(Pair.of(iconst, getField));
								}
							}
						}
					}
				}
			}
		}

		for(Pair<AbstractInsnNode, FieldInsnNode> pair : keyboardConstants) {
			instructions.set(pair.getLeft(), pair.getRight());
		}

		if(glCalls.isEmpty()) return;
		try {
			IdentifyCall sources = IdentifyCall.getInputs(classNode.name, node);
			for(MethodInsnNode invoke : glCalls) {
				for(AbstractInsnNode insn : sources.getAllInputsOf(invoke)) {
					if(insn == null) {
						continue;
					}
					Integer intValue = intValue(insn);
					if(intValue != null) {
						InsnList newinsns = getGLInsn(invoke, intValue);
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

	private static boolean isICmp(int opcode) {
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

	public static FieldInsnNode getKeyboardInsn(int constant) {
		String constantString = CONSTANTS_KEYBOARD.get(constant);
		return constantString == null ? null : new FieldInsnNode(Opcodes.GETSTATIC, "org/lwjgl/input/Keyboard", constantString, "I");
	}

	public static InsnList getGLInsn(MethodInsnNode invoke, int constant) {
		String pkg = invoke.owner.substring(17); //invoke.owner.replace("org/lwjgl/opengl/", "");
		for (Pair<Map<String, List<String>>, Map<String, Map<Integer, String>>> group : CONSTANTS) {
			Map<String, List<String>> methodKeys = group.getLeft();
			List<String> methodList = methodKeys.get(pkg);
			if (methodList != null && methodList.contains(invoke.name)) {
				Map<String, Map<Integer, String>> methodValues = group.getRight();
				for(Entry<String, Map<Integer, String>> entry : methodValues.entrySet()) {
					String key = entry.getKey();
					Map<Integer, String> value = entry.getValue();
					String constantValue = value.get(constant);
					if(constantValue == null) continue;

					InsnList instructions = new InsnList();
					int i = -1;
					do {
						char operator = i == -1 ? 0 : constantValue.charAt(i);
						int index1 = i + 1;
						i = indexOf(OPERATORS, index1, constantValue);
						int index2 = i == -1 ? constantValue.length() : i;
						String name = constantValue.substring(index1, index2).trim();
						instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "org/lwjgl/opengl/" + key, name, "I"));
						switch (operator) {
						case '|':
							instructions.add(new InsnNode(Opcodes.IOR));
							break;
						case '&':
							instructions.add(new InsnNode(Opcodes.IAND));
							break;
						case '^':
							instructions.add(new InsnNode(Opcodes.IXOR));
							break;
						}
					} while(i != -1);
					return instructions;
				}
			}
		}
		return null;
	}

    public static int indexOf(char[] ch, int fromIndex, String string) {
        final int max = string.length();
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            return -1;
        }

        final char[] value = string.toCharArray();
        for (int i = fromIndex; i < max; i++) {
            for (int i2 = 0; i2 < ch.length; i2++) {
	            if (value[i] == ch[i2]) {
	                return i;
	            }
            }
        }
        return -1;
    }

    // Private methods for initialization

	private static JSONObject getJson() {
		try {
			return Util.parseJSON(GLConstants.class.getClassLoader().getResourceAsStream("gl_constants.json"));
		} catch (JSONException | IOException e) {
			return null;
		}
	}

	private static List<Pair<Map<String, List<String>>, Map<String, Map<Integer, String>>>> getConstants(JSONArray jsonArray) {
		if(jsonArray == null || jsonArray.isEmpty()) {
			return Collections.emptyList();
		}
		List<Pair<Map<String, List<String>>, Map<String, Map<Integer, String>>>> list = new ArrayList<>();
		for(int i = 0; i < jsonArray.length(); i++) {
			JSONArray a = jsonArray.optJSONArray(i);
			if(a == null || a.length() < 2) continue;
			JSONObject methodKeys = a.optJSONObject(0);
			JSONObject methodValues = a.optJSONObject(1);
			if(methodKeys == null || methodValues == null) continue;

			Map<String, List<String>> map = new HashMap<>();
			Iterator<String> keys = methodKeys.keys();
			while(keys.hasNext()) {
				String key = keys.next();
				JSONArray value = methodKeys.optJSONArray(key);
				if(value == null) continue;
				map.put(key, toList(value));
			}

			Map<String, Map<Integer, String>> map2 = new HashMap<>();
			Iterator<String> keys2 = methodValues.keys();
			while(keys2.hasNext()) {
				String key = keys2.next();
				JSONObject value = methodValues.optJSONObject(key);
				if(value == null) continue;
				map2.put(key, toMap(value));
			}

			list.add(Pair.of(map, map2));
		}
		return list;
	}

	private static Map<Integer, String> toMap(JSONObject jsonObject) {
		if(jsonObject == null) {
			return Collections.emptyMap();
		}
		Map<Integer, String> map = new HashMap<>();
		Iterator<String> keys = jsonObject.keys();
		while(keys.hasNext()) {
			String key = keys.next();
			String value = jsonObject.optString(key, null);
			if(value == null) continue;
			try {
				int i = Integer.parseInt(key);
				map.put(i, value);
			}
			catch (NumberFormatException e) {}
		}
		return map;
	}

	private static List<String> toList(JSONArray packages) {
		if(packages == null) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>();
		for(int i = 0; i < packages.length(); i++) {
			String s = packages.optString(i, null);
			if(s == null) continue;
			list.add(s);
		}
		return list;
	}

	private static List<String> getPackages(List<Pair<Map<String, List<String>>, Map<String, Map<Integer, String>>>> constants) {
		if(constants == null || constants.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>();
		for(Pair<Map<String, List<String>>, ?> pair : constants) {
			Map<String, ?> methodKeys = pair.getLeft();
			for(Entry<String, ?> entry : methodKeys.entrySet()) {
				list.add("org/lwjgl/opengl/" + entry.getKey());
			}
		}
		return list;
	}
}
