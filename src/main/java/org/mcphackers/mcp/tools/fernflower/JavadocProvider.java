package org.mcphackers.mcp.tools.fernflower;

import de.fernflower.main.providers.IJavadocProvider;
import de.fernflower.struct.StructClass;
import de.fernflower.struct.StructField;
import de.fernflower.struct.StructMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JavadocProvider implements IJavadocProvider {

    private static final Map<String, String> classes = new HashMap<>();
    private static final Map<String, String> methods = new HashMap<>();
    private static final Map<String, String> fields = new HashMap<>();

    public JavadocProvider(File javadocFile) throws IOException {
        readMappings(javadocFile);
    }

    @Override
    public String getClassDoc(StructClass structClass) {
        return classes.get(structClass.qualifiedName.replace(".", "/"));
    }

    @Override
    public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
        return methods.get(structClass.qualifiedName.replace(".", "/") + "/" + structMethod.getName() + structMethod.getDescriptor());
    }


    @Override
    public String getFieldDoc(StructClass structClass, StructField structField) {
        return fields.get(structClass.qualifiedName.replace(".", "/") + "/" + structField.getName() + "(" + structField.getDescriptor() + ")");
    }

    private static void readMappings(File inputFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line = reader.readLine();
            while (line != null) {
                String[] splitLine = line.split("=");

                if (splitLine[0].startsWith("c ")) {
                    // Class Javadoc: c net/minecraft/client/Minecraft="Hello\nWorld"
                    classes.put(splitLine[0].replaceFirst("c ", ""), splitLine[1].replaceAll("\"", ""));
                } else if (splitLine[0].startsWith("m ")) {
                    // Method Javadoc: m net/minecraft/client/Minecraft/runTick()V="Hello\nWorld"
                    methods.put(splitLine[0].replace("m ", ""), splitLine[1].replaceAll("\"", ""));
                } else if (splitLine[0].startsWith("f ")) {
                    // Field Javadoc: f net/minecraft/client/Minecraft/fullscreen(Z)="Hello\nWorld"
                    fields.put(splitLine[0].replace("f ", ""), splitLine[1].replaceAll("\"", ""));
                } else {
                    System.err.println("Invalid keyword with: " + line);
                }

                // Read next line
                line = reader.readLine();
            }
        }
    }
}
