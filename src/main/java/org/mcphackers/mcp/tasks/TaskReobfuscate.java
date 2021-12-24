package org.mcphackers.mcp.tasks;

import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tasks.info.TaskInfoUpdateMD5;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;
import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TaskReobfuscate extends Task {
    private final Map<String, String> recompHashes = new HashMap<>();
    private final Map<String, String> originalHashes = new HashMap<>();

    private final Map<String, String> defaultReobfClasses = new HashMap<>();
    private final Map<String, String> defaultReobfMethods = new HashMap<>();
    private final Map<String, String> defaultReobfFields = new HashMap<>();

    private final Map<String, String> extraReobfClasses = new HashMap<>();
    private final Map<String, String> extraReobfMethods = new HashMap<>();
    private final Map<String, String> extraReobfFields = new HashMap<>();

    public TaskReobfuscate(int side, TaskInfo info) {
        super(side, info);
    }

    @Override
    public void doTask() throws Exception {
        boolean binCheck = checkBins(side);

        if (binCheck) {
            if (Files.exists(Utility.getPath(side == 1 ? MCPConfig.SERVER_REOBF : MCPConfig.CLIENT_REOBF))) {
                Utility.deleteDirectoryStream(Utility.getPath(side == 1 ? MCPConfig.SERVER_REOBF : MCPConfig.CLIENT_REOBF));
            }

            // Create recompilation hashes and compare them to the original hashes
            new TaskUpdateMD5(side, new TaskInfoUpdateMD5()).updateMD5(true);
            // Recompiled hashes
            gatherMD5Hashes(true, this.side);
            // Original hashes
            gatherMD5Hashes(false, this.side);

            //Compacting bin directory
            readDeobfuscationMappings(this.side);
            writeReobfuscationMappings(this.side);

            Path reobfJar = Utility.getPath(side == 1 ? MCPConfig.SERVER_REOBF_JAR : MCPConfig.CLIENT_REOBF_JAR);
            Utility.compress(Utility.getPath(side == 1 ? MCPConfig.SERVER_BIN : MCPConfig.CLIENT_BIN), reobfJar);
            TinyRemapper remapper = null;

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(reobfJar).build()) {
                remapper = TaskDecompile.remap(TinyUtils.createTinyMappingProvider(Paths.get(side == 1 ? MCPConfig.SERVER_MAPPINGS_RO : MCPConfig.CLIENT_MAPPINGS_RO), "official", "named"), reobfJar, outputConsumer, Paths.get("jars", "bin"));
                outputConsumer.addNonClassFiles(reobfJar, NonClassCopyMode.FIX_META_INF, remapper);
            } finally {
                if (remapper != null) {
                    remapper.finish();
                }
            }
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Reobfuscating...", 0, 1);
    }

    // Utility methods
    private void writeReobfuscationMappings(int side) throws IOException {
        Files.walkFileTree(Paths.get(side == 1 ? MCPConfig.SERVER_BIN : MCPConfig.CLIENT_BIN), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
                        private String className = "";

                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            className = name;
                            if (!defaultReobfClasses.containsKey(name)) {
                                extraReobfClasses.put(name, name.replace("net/minecraft/src/", ""));
                            }
                            super.visit(version, access, name, signature, superName, interfaces);
                        }

                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            if (!defaultReobfFields.containsKey(className + "/" + name) && !name.equals("$VALUES")) {
                                //extraReobfFields.put(className + "/" + name, className + "/" + name);
                                //System.out.println("Class-name: " + className + ", Field: " + name + ", Signature: " + descriptor);
                            }
                            return super.visitField(access, name, descriptor, signature, value);
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (!defaultReobfMethods.containsKey(className + "/" + name) && !name.equals("<init>") && !name.equals("<clinit>")) {
                                //System.out.println("Class-name: " + className + ", Method name: " + name);
                            }
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    };
                    ClassReader reader = new ClassReader(Files.readAllBytes(file));
                    reader.accept(visitor, 0);
                }
                return super.visitFile(file, attrs);
            }
        });

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(side == 1 ? MCPConfig.SERVER_MAPPINGS_RO : MCPConfig.CLIENT_MAPPINGS_RO).toFile()))) {
            writer.write("tiny\t2\t0\tofficial\tnamed\n");

            for (Map.Entry<String, String> classKeyPair : defaultReobfClasses.entrySet()) {
                String deobfuscatedClassName = classKeyPair.getKey();
                String reobfuscatedClassName = classKeyPair.getValue();
                writer.write("c\t" + deobfuscatedClassName + "\t" + reobfuscatedClassName + "\n");

                for (Map.Entry<String, String> methodKeyPair : defaultReobfMethods.entrySet()) {
                    String deobfuscatedFullName = methodKeyPair.getKey();
                    if (deobfuscatedFullName.startsWith(deobfuscatedClassName) && !deobfuscatedFullName.endsWith("<init>") && !deobfuscatedFullName.endsWith("<clinit>")) {
                        String deobfuscatedMethodName = deobfuscatedFullName.substring(methodKeyPair.getKey().lastIndexOf("/") + 1);
                        String reobfuscatedMethodName = methodKeyPair.getValue();

                        // 	m	(Lho;IIIII)V	a	renderQuad
                        String signature = reobfuscatedMethodName.substring(reobfuscatedMethodName.lastIndexOf("("));
                        String remappedSignature = remapSignature(signature);
                        writer.write("\tm\t" + remappedSignature + "\t" + deobfuscatedMethodName + "\t" + reobfuscatedMethodName.replace(signature, "") + "\n");
                    }
                }
                writer.flush();

                for (Map.Entry<String, String> fieldKeyPair : defaultReobfFields.entrySet()) {
                    String deobfuscatedFullName = fieldKeyPair.getKey();
                    if (deobfuscatedFullName.startsWith(deobfuscatedClassName)) {
                        String deobfuscatedFieldName = deobfuscatedFullName.substring(fieldKeyPair.getKey().lastIndexOf("/") + 1);
                        String reobfuscatedFieldName = fieldKeyPair.getValue();

                        // 	m	(Lho;IIIII)V	a	renderQuad
                        String signature = reobfuscatedFieldName.substring(reobfuscatedFieldName.lastIndexOf("(") + 1, reobfuscatedFieldName.length() - 1);
                        String remappedSignature = remapSignature(signature);
                        writer.write("\tf\t" + remappedSignature + "\t" + deobfuscatedFieldName + "\t" + reobfuscatedFieldName.substring(0, reobfuscatedFieldName.indexOf("(")) + "\n");
                    }
                }
                writer.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String remapSignature(String signature) {
        String remappedSignature = "";
        StringBuilder builder = new StringBuilder();
        if (remapSignature(builder, signature, 0, signature.length())) {
            remappedSignature = builder.toString();
            builder.setLength(0);
        }
        if (remappedSignature.equals("") || remappedSignature.equals("()")) remappedSignature = signature;
        return remappedSignature;
    }

    private void readDeobfuscationMappings(int side) {
        if (side == 0) {
            try (BufferedReader reader = new BufferedReader(new FileReader(Paths.get(side == 1 ? MCPConfig.SERVER_MAPPINGS : MCPConfig.CLIENT_MAPPINGS).toFile()))) {
                String line = reader.readLine();
                String currentClassName = "";
                while (line != null) {
                    String[] tokens = line.split("\t");

                    // Tiny v2 uses indentation to denote level, so classes are always before methods and fields
                    if (line.startsWith("c")) {
                        // Class
                        // c	aa	net/minecraft/src/TextureCompassFX
                        defaultReobfClasses.put(tokens[2], tokens[1]);
                        currentClassName = tokens[2];
                    } else if (line.startsWith("\tm")) {
                        // Method
                        // m	(Lho;IIIII)V	a	renderQuad
                        defaultReobfMethods.put(currentClassName + "/" + tokens[4], tokens[3] + tokens[2]);
                    } else if (line.startsWith("\tf")) {
                        // Field
                        // 	f	D	k	currentAngle
                        defaultReobfFields.put(currentClassName + "/" + tokens[4], tokens[3] + "(" + tokens[2] + ")");
                    }

                    // Read next line
                    line = reader.readLine();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean remapSignature(StringBuilder signatureOut, String signature, int start, int end) {
        if (start == end) {
            return false;
        }
        int type = signature.codePointAt(start++);
        switch (type) {
            case 'T':
                // generics type parameter
                // fall-through intended as they are similar enough in format compared to objects
            case 'L':
                // object
                // find the end of the internal name of the object
                int endObject = start;
                while(true) {
                    // this will skip a character, but this is not interesting as class names have to be at least 1 character long
                    int codepoint = signature.codePointAt(++endObject);
                    if (codepoint == ';') {
                        String name = signature.substring(start, endObject);
                        String newName = getKeyByValue(defaultReobfClasses, name);
                        boolean modified = false;
                        if (newName != null) {
                            name = newName;
                            modified = true;
                        }
                        signatureOut.appendCodePoint(type);
                        signatureOut.append(name);
                        signatureOut.append(';');
                        modified |= remapSignature(signatureOut, signature, ++endObject, end);
                        return modified;
                    } else if (codepoint == '<') {
                        // generics - please no
                        // post scriptum: well, that was a bit easier than expected
                        int openingBrackets = 1;
                        int endGenerics = endObject;
                        while(true) {
                            codepoint = signature.codePointAt(++endGenerics);
                            if (codepoint == '>' ) {
                                if (--openingBrackets == 0) {
                                    break;
                                }
                            } else if (codepoint == '<') {
                                openingBrackets++;
                            }
                        }
                        String name = signature.substring(start, endObject);
                        String newName = getKeyByValue(defaultReobfClasses, name);
                        boolean modified = false;
                        if (newName != null) {
                            name = newName;
                            modified = true;
                        }
                        signatureOut.append('L');
                        signatureOut.append(name);
                        signatureOut.append('<');
                        modified |= remapSignature(signatureOut, signature, endObject + 1, endGenerics++);
                        signatureOut.append('>');
                        // apparently that can be rarely be a '.', don't ask when or why exactly this occours
                        signatureOut.appendCodePoint(signature.codePointAt(endGenerics));
                        modified |= remapSignature(signatureOut, signature, ++endGenerics, end);
                        return modified;
                    }
                }
            case '+':
                // idk what this one does - but it appears that it works good just like it does right now
            case '*':
                // wildcard - this can also be read like a regular primitive
                // fall-through intended
            case '(':
            case ')':
                // apparently our method does not break even in these cases, so we will consider them raw primitives
            case '[':
                // array - fall through intended as in this case they behave the same
            default:
                // primitive
                signatureOut.appendCodePoint(type);
                return remapSignature(signatureOut, signature, start, end); // Did not modify the signature - but following operations could
        }
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void gatherMD5Hashes(boolean reobf, int side) {
        Path clientMD5 = reobf ? Paths.get("temp", "client_reobf.md5") : Paths.get("temp", "client.md5");
        Path serverMD5 = reobf ? Paths.get("temp", "server_reobf.md5") : Paths.get("temp", "server.md5");

        try (BufferedReader reader = new BufferedReader(new FileReader(side == 0 ? clientMD5.toFile() : serverMD5.toFile()))) {
            String line = reader.readLine();
            while (line != null) {
                String[] tokens = line.split(" ");
                if (reobf) {
                    recompHashes.put(tokens[0], tokens[1]);
                } else {
                    originalHashes.put(tokens[0], tokens[1]);
                }

                // Read next line
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkBins(int side) {
        Path minecraft1 = Paths.get("bin", "minecraft", "net", "minecraft", "client", "Minecraft.class");
        Path minecraft2 = Paths.get("bin", "minecraft", "net", "minecraft", "src", "Minecraft.class");

        Path minecraftServer1 = Paths.get("bin", "minecraft_server", "net", "minecraft", "server", "MinecraftServer.class");
        Path minecraftServer2 = Paths.get("bin", "minecraft_server", "net", "minecraft", "src", "MinecraftServer.class");
        if (side == 0 && (Files.exists(minecraft1) || Files.exists(minecraft2))) {
            return true;
        } else if (side == 0) {
            return false;
        }

        if (side == 1 && (Files.exists(minecraftServer1) || Files.exists(minecraftServer2))) {
            return true;
        } else if (side == 1) {
            return false;
        }

        return false;
    }
}
