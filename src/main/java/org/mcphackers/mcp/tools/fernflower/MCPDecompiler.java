package org.mcphackers.mcp.tools.fernflower;

import de.fernflower.main.ClassesProcessor;
import de.fernflower.main.DecompilerContext;
import de.fernflower.main.Fernflower;
import de.fernflower.main.decompiler.BaseDecompiler;
import de.fernflower.main.extern.IBytecodeProvider;
import de.fernflower.main.extern.IFernflowerLogger;
import de.fernflower.main.extern.IResultSaver;
import de.fernflower.main.providers.IJavadocProvider;
import de.fernflower.modules.renamer.IdentifierConverter;
import de.fernflower.struct.StructContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class MCPDecompiler extends BaseDecompiler {
    public MCPDecompiler(IBytecodeProvider provider, IResultSaver saver, Map<String, Object> options, IFernflowerLogger logger) {
        super(provider, saver, options, logger);
    }

    public MCPDecompiler(IBytecodeProvider provider, IResultSaver saver, Map<String, Object> options, IFernflowerLogger logger, IJavadocProvider javadocProvider) {
        super(provider, saver, options, logger, javadocProvider);
    }

    @Override
    public void decompileContext() {
        try {
            Fernflower fernflower = (Fernflower) BaseDecompiler.class.getDeclaredField("fernflower").get(this);
            StructContext structContext = (StructContext) fernflower.getClass().getDeclaredField("structContext").get(fernflower);

            if (DecompilerContext.getOption("ren")) {
                (new IdentifierConverter()).rename(structContext);
            }

            ClassesProcessor classesProcessor = new MCPClassesProcessor(structContext);
            fernflower.classesProcessor = classesProcessor;
            DecompilerContext.setClassProcessor(classesProcessor);
            DecompilerContext.setStructContext(structContext);
            structContext.saveContext();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            DecompilerContext.setCurrentContext(null);
        }
    }
}
