package org.mcphackers.mcp.tools.fernflower;

import de.fernflower.main.DecompilerContext;
import de.fernflower.main.collectors.CounterContainer;
import de.fernflower.main.collectors.VarNamesCollector;
import de.fernflower.main.extern.IFernflowerLogger;
import de.fernflower.main.rels.ClassWrapper;
import de.fernflower.main.rels.MethodProcessorRunnable;
import de.fernflower.main.rels.MethodWrapper;
import de.fernflower.modules.decompiler.stats.RootStatement;
import de.fernflower.modules.decompiler.vars.VarProcessor;
import de.fernflower.modules.decompiler.vars.VarVersionPair;
import de.fernflower.struct.StructClass;
import de.fernflower.struct.StructField;
import de.fernflower.struct.StructMethod;
import de.fernflower.struct.attr.StructLocalVariableTableAttribute;
import de.fernflower.struct.gen.MethodDescriptor;
import de.fernflower.util.InterpreterUtil;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.tree.MappingTree;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MCPClassWrapper extends ClassWrapper {
    private final MappingTree tree;

    public MCPClassWrapper(StructClass classStruct) {
        super(classStruct);

        this.tree = Decompiler.tinyJavadocProvider.mappingTree;
    }

    @Override
    public void init() throws IOException {
        DecompilerContext.setProperty("CURRENT_CLASS", this.getClassStruct());
        DecompilerContext.setProperty("CURRENT_CLASS_WRAPPER", this);
        DecompilerContext.getLogger().startClass(this.getClassStruct().qualifiedName);
        Set<String> setFieldNames = new HashSet<>();

        for (StructField fd : this.getClassStruct().getFields()) {
            setFieldNames.add(fd.getName());
        }

        int maxSec = Integer.parseInt(DecompilerContext.getProperty("mpm").toString());
        boolean testMode = DecompilerContext.getOption("__unit_test_mode__");

        for(Iterator<StructMethod> methodIterator = this.getClassStruct().getMethods().iterator(); methodIterator.hasNext(); DecompilerContext.getLogger().endMethod()) {
            StructMethod mt = methodIterator.next();
            DecompilerContext.getLogger().startMethod(mt.getName() + " " + mt.getDescriptor());
            VarNamesCollector vc = new VarNamesCollector();
            DecompilerContext.setVarNamesCollector(vc);
            CounterContainer counter = new CounterContainer();
            DecompilerContext.setCounterContainer(counter);
            DecompilerContext.setProperty("CURRENT_METHOD", mt);
            DecompilerContext.setProperty("CURRENT_METHOD_DESCRIPTOR", MethodDescriptor.parseDescriptor(mt.getDescriptor()));
            VarProcessor varProc = new VarProcessor();
            DecompilerContext.setProperty("CURRENT_VAR_PROCESSOR", varProc);
            RootStatement root = null;
            boolean isError = false;

            try {
                if (mt.containsCode()) {
                    if (maxSec != 0 && !testMode) {
                        MethodProcessorRunnable mtProc = new MethodProcessorRunnable(mt, varProc, DecompilerContext.getCurrentContext());
                        Thread mtThread = new Thread(mtProc, "Java decompiler");
                        long stopAt = System.currentTimeMillis() + (maxSec * 1000L);
                        mtThread.start();

                        while(!mtProc.isFinished()) {
                            try {
                                synchronized(mtProc.lock) {
                                    mtProc.lock.wait(200L);
                                }
                            } catch (InterruptedException var19) {
                                killThread(mtThread);
                                throw var19;
                            }

                            if (System.currentTimeMillis() >= stopAt) {
                                String message = "Processing time limit exceeded for method " + mt.getName() + ", execution interrupted.";
                                DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.ERROR);
                                killThread(mtThread);
                                isError = true;
                                break;
                            }
                        }

                        if (!isError) {
                            root = mtProc.getResult();
                        }
                    } else {
                        root = MethodProcessorRunnable.codeToJava(mt, varProc);
                    }
                } else {
                    boolean thisVar = !mt.hasModifier(8);
                    MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());
                    int paramCount = 0;
                    if (thisVar) {
                        varProc.getThisVars().put(new VarVersionPair(0, 0), this.getClassStruct().qualifiedName);
                        paramCount = 1;
                    }

                    paramCount = paramCount + md.params.length;
                    int varIndex = 0;

                    for(int i = 0; i < paramCount; ++i) {
                        String s = vc.getFreeName(varIndex);
                        if (i > 0 || !thisVar) {
                            int i2 = i;
                            if (!thisVar) {
                                i2 = i + 1;
                            }

                            s = vc.getFreeName(varIndex, md.params[i2 - 1]);
                        }
                        String className = mt.getClassStruct().qualifiedName;
                        String methodName = mt.getName();
                        String descriptor = mt.getDescriptor();
                        MappingTree.MethodMapping mapping = tree.getMethod(className, methodName, descriptor, -1);
                        // TODO: Set variable name based off of mappings.
                        varProc.setVarName(new VarVersionPair(varIndex, 0), s);
                        if (thisVar) {
                            if (i == 0) {
                                ++varIndex;
                            } else {
                                varIndex += md.params[i - 1].stackSize;
                            }
                        } else {
                            varIndex += md.params[i].stackSize;
                        }
                    }
                }
            } catch (Throwable var20) {
                DecompilerContext.getLogger().writeMessage("Method " + mt.getName() + " " + mt.getDescriptor() + " couldn't be decompiled.", var20);
                isError = true;
            }

            MethodWrapper methodWrapper = new MethodWrapper(root, varProc, mt, counter);
            methodWrapper.decompiledWithErrors = isError;
            this.getMethods().addWithKey(methodWrapper, InterpreterUtil.makeUniqueKey(mt.getName(), mt.getDescriptor()));
            varProc.refreshVarNames(new VarNamesCollector(setFieldNames));
            if (DecompilerContext.getOption("udv")) {
                StructLocalVariableTableAttribute attr = (StructLocalVariableTableAttribute)mt.getAttributes().getWithKey("LocalVariableTable");
                if (attr != null) {
                    varProc.setDebugVarNames(attr.getMapVarNames());
                }
            }
        }

        DecompilerContext.getLogger().endClass();
    }
}
