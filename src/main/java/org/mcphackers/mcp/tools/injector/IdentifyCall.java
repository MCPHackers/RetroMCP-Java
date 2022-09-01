package org.mcphackers.mcp.tools.injector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class IdentifyCall {
    private final InsnList instructions;
    private final Map<AbstractInsnNode, Set<SourceValue>> sources;
    private final TreeMap<int[],AbstractInsnNode> conditionals;

    private IdentifyCall(InsnList il,
            Map<AbstractInsnNode, Set<SourceValue>> s, TreeMap<int[], AbstractInsnNode> c) {
        instructions = il;
        sources = s;
        conditionals = c;
    }

    Set<AbstractInsnNode> getAllInputsOf(AbstractInsnNode instr) {
        Set<AbstractInsnNode> source = new HashSet<>();
        List<SourceValue> pending = new ArrayList<>(sources.get(instr));
        for (int pIx = 0; pIx < pending.size(); pIx++) {
            SourceValue sv = pending.get(pIx);
            final boolean branch = sv.insns.size() > 1;
            for(AbstractInsnNode in: sv.insns) {
                if(source.add(in))
                    pending.addAll(sources.getOrDefault(in, Collections.emptySet()));
                if(branch) {
                    int ix = instructions.indexOf(in);
                    conditionals.forEach((b,i) -> {
                        if(b[0] <= ix && b[1] >= ix && source.add(i))
                            pending.addAll(sources.getOrDefault(i, Collections.emptySet()));
                    });
                }
            }
        }
        return source;
    }

    public static IdentifyCall getInputs(
        String internalClassName, MethodNode toAnalyze) throws AnalyzerException {

        InsnList instructions = toAnalyze.instructions;
        Map<AbstractInsnNode, Set<SourceValue>> sources = new HashMap<>();
        SourceInterpreter i = new SourceInterpreter(Opcodes.ASM9) {
            @Override
            public SourceValue unaryOperation(AbstractInsnNode insn, SourceValue value) {
                sources.computeIfAbsent(insn, x -> new HashSet<>()).add(value);
                return super.unaryOperation(insn, value);
            }

            @Override
            public SourceValue binaryOperation(AbstractInsnNode insn, SourceValue v1, SourceValue v2) {
                addAll(insn, Arrays.asList(v1, v2));
                return super.binaryOperation(insn, v1, v2);
            }

            @Override
            public SourceValue ternaryOperation(AbstractInsnNode insn, SourceValue v1, SourceValue v2, SourceValue v3) {
                addAll(insn, Arrays.asList(v1, v2, v3));
                return super.ternaryOperation(insn, v1, v2, v3);
            }

            @Override
            public SourceValue naryOperation(AbstractInsnNode insn, List<? extends SourceValue> values) {
                addAll(insn, values);
                return super.naryOperation(insn, values);
            }

            private void addAll(AbstractInsnNode insn, List<? extends SourceValue> values) {
                sources.computeIfAbsent(insn, x -> new HashSet<>()).addAll(values);
            }
        };
        TreeMap<int[],AbstractInsnNode> conditionals = new TreeMap<>(
            Comparator.comparingInt((int[] a) -> a[0]).thenComparingInt(a -> a[1]));
        Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(i) {
            @Override
            protected void newControlFlowEdge(int insn, int successor) {
                if(insn != successor - 1) {
                    AbstractInsnNode instruction = instructions.get(insn);
                    Set<SourceValue> dep = sources.get(instruction);
                    if(dep != null && !dep.isEmpty())
                        conditionals.put(new int[]{ insn, successor }, instruction);
                }
            }
        };
        analyzer.analyze(internalClassName, toAnalyze);
        return new IdentifyCall(instructions, sources, conditionals);
    }
}