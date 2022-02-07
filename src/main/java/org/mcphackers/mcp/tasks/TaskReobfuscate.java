package org.mcphackers.mcp.tasks;

import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.ProgressInfo;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.objectweb.asm.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskReobfuscate extends Task {
    private final Map<String, String> recompHashes = new HashMap<>();
    private final Map<String, String> originalHashes = new HashMap<>();

    private final Map<String, String> reobfClasses = new HashMap<>();
    private final Map<String, String> reobfPackages = new HashMap<>();
    private final Map<String, String> reobfMethods = new HashMap<>();
    private final Map<String, String> reobfFields = new HashMap<>();
    
	private final TaskUpdateMD5 md5Task = new TaskUpdateMD5(side, info);

    public TaskReobfuscate(int side, TaskInfo info) {
        super(side, info);
    }

    @Override
    public void doTask() throws Exception {

        Path reobfJar = Paths.get(chooseFromSide(MCPConfig.CLIENT_REOBF_JAR, 	MCPConfig.SERVER_REOBF_JAR));
        Path reobfBin = Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, 			MCPConfig.SERVER_BIN));
    	Path reobfDir = Paths.get(chooseFromSide(MCPConfig.CLIENT_REOBF, 		MCPConfig.SERVER_REOBF));
    	Path mappings = Paths.get(chooseFromSide(MCPConfig.CLIENT_MAPPINGS_RO, 	MCPConfig.SERVER_MAPPINGS_RO));

        step();
        md5Task.updateMD5(true);

        if (Files.exists(reobfBin)) {
            FileUtil.deleteDirectoryIfExists(reobfDir);
            step();
            gatherMD5Hashes(true);
            gatherMD5Hashes(false);

            step();
            readDeobfuscationMappings();
            writeReobfuscationMappings();
            
            Files.deleteIfExists(reobfJar);
            TinyRemapper remapper = null;

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(reobfJar).build()) {
                remapper = TaskDecompile.remap(TinyUtils.createTinyMappingProvider(mappings, "named", "official"), reobfBin, outputConsumer, TaskDecompile.getLibraryPaths(side));
            } finally {
                if (remapper != null) {
                    remapper.finish();
                }
            }
            step();
            unpack(reobfJar, reobfDir);
        } else {
        	throw new IOException(chooseFromSide("Client", "Server") + " classes not found!");
        }
    }

    @Override
    public ProgressInfo getProgress() {
    	int total = 100;
    	int current;
    	switch (step) {
	        case 1: {
	    	    current = 1;
	    	    ProgressInfo info = md5Task.getProgress();
	    	    int percent = info.getCurrent() / info.getTotal() * 50;
	            return new ProgressInfo(info.getMessage(), current + percent, total); }
	        case 2:
        	    current = 51;
                return new ProgressInfo("Gathering MD5 hashes...", current, total);
	        case 3:
        	    current = 52;
                return new ProgressInfo("Reobfuscating...", current, total);
	        case 4:
        	    current = 54;
                return new ProgressInfo("Unpacking...", current, total);
	        default:
	    	    return super.getProgress();
	    }
    }

    // Utility methods
    private void writeReobfuscationMappings() throws IOException {
    	
        Path reobfBin = Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, 			MCPConfig.SERVER_BIN));
    	Path mappings = Paths.get(chooseFromSide(MCPConfig.CLIENT_MAPPINGS_RO, 	MCPConfig.SERVER_MAPPINGS_RO));
    	
        Files.walkFileTree(reobfBin, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
                        private String className = "";

                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            className = name;
                            if (!reobfClasses.containsKey(name) && name.contains("Start")) {
                            	String key = name.lastIndexOf("/") >= 0 ? name.substring(0, name.lastIndexOf("/") + 1) : null;
                                String obfPackage = reobfPackages.get(key);
                                if(obfPackage == null) {
                                	obfPackage = "";
                                }
                            	String clsName = name.lastIndexOf("/") >= 0 ? name.substring(name.lastIndexOf("/") + 1) : name;
                            	reobfClasses.put(name, obfPackage + clsName);
                                //extraReobfClasses.put(name, name.replace("net/minecraft/src/", ""));
                            }
                            super.visit(version, access, name, signature, superName, interfaces);
                        }

                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            return super.visitField(access, name, descriptor, signature, value);
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    };
                    ClassReader reader = new ClassReader(Files.readAllBytes(file));
                    reader.accept(visitor, 0);
                }
                return super.visitFile(file, attrs);
            }
        });

        try (BufferedWriter writer = Files.newBufferedWriter(mappings)) {
            writer.write("tiny\t2\t0\tnamed\tofficial\n");

            Map<String, String> flippedReobfClasses = reobfClasses.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

            for (Map.Entry<String, String> classKeyPair : reobfClasses.entrySet()) {
                String deobfuscatedClassName = classKeyPair.getKey();
                String reobfuscatedClassName = classKeyPair.getValue();
                writer.write("c\t" + deobfuscatedClassName + "\t" + reobfuscatedClassName + "\n");

                for (Map.Entry<String, String> methodKeyPair : reobfMethods.entrySet()) {
                    String deobfuscatedFullName = methodKeyPair.getKey();
                    if (deobfuscatedFullName.startsWith(deobfuscatedClassName) && !deobfuscatedFullName.endsWith("<init>") && !deobfuscatedFullName.endsWith("<clinit>")) {
                        String deobfuscatedMethodName = deobfuscatedFullName.substring(methodKeyPair.getKey().lastIndexOf("/") + 1);
                        String reobfuscatedMethodName = methodKeyPair.getValue();

                        // 	m	(Lho;IIIII)V	a	renderQuad
                        String signature = reobfuscatedMethodName.substring(reobfuscatedMethodName.lastIndexOf("("));
                        String remappedSignature = MappingUtil.mapDesc(signature, flippedReobfClasses);
                        writer.write("\tm\t" + remappedSignature + "\t" + deobfuscatedMethodName + "\t" + reobfuscatedMethodName.replace(signature, "") + "\n");
                    }
                }
                writer.flush();

                for (Map.Entry<String, String> fieldKeyPair : reobfFields.entrySet()) {
                    String deobfuscatedFullName = fieldKeyPair.getKey();
                    if (deobfuscatedFullName.startsWith(deobfuscatedClassName)) {
                        String deobfuscatedFieldName = deobfuscatedFullName.substring(fieldKeyPair.getKey().lastIndexOf("/") + 1);
                        String reobfuscatedFieldName = fieldKeyPair.getValue();

                        // 	m	(Lho;IIIII)V	a	renderQuad
                        String signature = reobfuscatedFieldName.substring(reobfuscatedFieldName.lastIndexOf("(") + 1, reobfuscatedFieldName.length() - 1);
                        String remappedSignature = MappingUtil.mapDesc(signature, flippedReobfClasses);
                        writer.write("\tf\t" + remappedSignature + "\t" + deobfuscatedFieldName + "\t" + reobfuscatedFieldName.substring(0, reobfuscatedFieldName.indexOf("(")) + "\n");
                    }
                }
                writer.flush();
            }
        }
    }

    private void readDeobfuscationMappings() throws IOException {
    	Path mappings = Paths.get(chooseFromSide(MCPConfig.CLIENT_MAPPINGS, 	MCPConfig.SERVER_MAPPINGS));
    	
        try (BufferedReader reader = Files.newBufferedReader(mappings)) {
            String line = reader.readLine();
            String currentClassName = "";
            while (line != null) {
                String[] tokens = line.split("\t");

                // Tiny v2 uses indentation to denote level, so classes are always before methods and fields
                if (line.startsWith("c")) {
                    // Class
                    // c	aa	net/minecraft/src/TextureCompassFX
                    reobfClasses.put(tokens[2], tokens[1]);
                    String deobfPackage = tokens[2].lastIndexOf("/") >= 0 ? tokens[2].substring(0, tokens[2].lastIndexOf("/") + 1) : "";
                    if(!reobfPackages.containsKey(deobfPackage)) {
                        String obfPackage 	= tokens[1].lastIndexOf("/") >= 0 ? tokens[1].substring(0, tokens[1].lastIndexOf("/") + 1) : "";
                    	reobfPackages.put(deobfPackage, obfPackage);
                    }
                    currentClassName = tokens[2];
                } else if (line.startsWith("\tm")) {
                    // Method
                    // m	(Lho;IIIII)V	a	renderQuad
                    reobfMethods.put(currentClassName + "/" + tokens[4], tokens[3] + tokens[2]);
                } else if (line.startsWith("\tf")) {
                    // Field
                    // 	f	D	k	currentAngle
                    reobfFields.put(currentClassName + "/" + tokens[4], tokens[3] + "(" + tokens[2] + ")");
                }

                // Read next line
                line = reader.readLine();
            }
        }
    }

    private void gatherMD5Hashes(boolean reobf) throws IOException {
        Path md5 = Paths.get(reobf ? chooseFromSide(MCPConfig.CLIENT_MD5_RO, MCPConfig.SERVER_MD5_RO)
        							  : chooseFromSide(MCPConfig.CLIENT_MD5, MCPConfig.SERVER_MD5));

        try (BufferedReader reader = Files.newBufferedReader(md5)) {
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
        }
    }

    private void unpack(final Path src, final Path destDir) throws IOException {
    	FileUtil.unzip(src, destDir, entry -> {
            String fileName = entry.getName();
            String deobfName = Util.getKey(reobfClasses, fileName.replace(".class", ""));
            String hash = originalHashes.get(deobfName);
            return !entry.isDirectory() && (hash == null || !hash.equals(recompHashes.get(deobfName)));
    	});
    }
}
