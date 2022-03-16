package org.mcphackers.mcp.tools.fernflower;

import de.fernflower.main.ClassWriter;
import de.fernflower.main.ClassesProcessor;
import de.fernflower.main.DecompilerContext;
import de.fernflower.main.TextBuffer;
import de.fernflower.main.collectors.BytecodeSourceMapper;
import de.fernflower.main.collectors.CounterContainer;
import de.fernflower.main.collectors.ImportCollector;
import de.fernflower.main.extern.IFernflowerPreferences;
import de.fernflower.main.rels.ClassWrapper;
import de.fernflower.main.rels.LambdaProcessor;
import de.fernflower.main.rels.NestedClassProcessor;
import de.fernflower.main.rels.NestedMemberAccess;
import de.fernflower.struct.StructClass;
import de.fernflower.struct.StructContext;

import java.io.IOException;

public class MCPClassesProcessor extends ClassesProcessor {

    public MCPClassesProcessor(StructContext context) {
        super(context);
    }

    @Override
    public void writeClass(StructClass cl, TextBuffer buffer) throws IOException {
        ClassNode root = this.getMapRootClasses().get(cl.qualifiedName);
        if (root.type != ClassNode.CLASS_ROOT) {
            return;
        }

        DecompilerContext.getLogger().startReadingClass(cl.qualifiedName);
        try {
            ImportCollector importCollector = new ImportCollector(root);
            DecompilerContext.setImportCollector(importCollector);
            DecompilerContext.setCounterContainer(new CounterContainer());
            DecompilerContext.setBytecodeSourceMapper(new BytecodeSourceMapper());

            new LambdaProcessor().processClass(root);

            // add simple class names to implicit import
            addClassnameToImport(root, importCollector);

            // build wrappers for all nested classes (that's where actual processing takes place)
            initWrappers(root);

            new NestedClassProcessor().processClass(root, root);

            new NestedMemberAccess().propagateMemberAccess(root);

            TextBuffer classBuffer = new TextBuffer(AVERAGE_CLASS_SIZE);
            new ClassWriter().classToJava(root, classBuffer, 0, null);

            int index = cl.qualifiedName.lastIndexOf("/");
            if (index >= 0) {
                String packageName = cl.qualifiedName.substring(0, index).replace('/', '.');

                buffer.append("package ");
                buffer.append(packageName);
                buffer.append(";");
                buffer.appendLineSeparator();
                buffer.appendLineSeparator();
            }

            int import_lines_written = importCollector.writeImports(buffer);
            if (import_lines_written > 0) {
                buffer.appendLineSeparator();
            }

            int offsetLines = buffer.countLines();

            buffer.append(classBuffer);

            if (DecompilerContext.getOption(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING)) {
                BytecodeSourceMapper mapper = DecompilerContext.getBytecodeSourceMapper();
                mapper.addTotalOffset(offsetLines);
                if (DecompilerContext.getOption(IFernflowerPreferences.DUMP_ORIGINAL_LINES)) {
                    buffer.dumpOriginalLineNumbers(mapper.getOriginalLinesMapping());
                }
                if (DecompilerContext.getOption(IFernflowerPreferences.UNIT_TEST_MODE)) {
                    buffer.appendLineSeparator();
                    mapper.dumpMapping(buffer, true);
                }
            }
        }
        finally {
            destroyWrappers(root);
            DecompilerContext.getLogger().endReadingClass();
        }
    }

    private static void initWrappers(ClassesProcessor.ClassNode node) throws IOException {
        if (node.type != 8) {
            ClassWrapper wrapper = new MCPClassWrapper(node.classStruct);
            wrapper.init();
            node.wrapper = wrapper;

            for (ClassNode nestedClassNode : node.nested) {
                initWrappers(nestedClassNode);
            }

        }
    }
}
