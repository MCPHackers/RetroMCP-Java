package org.mcphackers.mcp.tools.injector;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.injector.transform.Injection;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Post-{@code applyMappings} transform that rewrites <em>string-literal</em>
 * references to vanilla MCP names inside selected user packages.
 *
 * <p>The standard rdi remapper rewrites bytecode-level type/field/method
 * references (TypeInsnNode owners, FieldInsnNode/MethodInsnNode owner+name+desc,
 * Type-typed LDC constants) but leaves <em>{@link String}-typed LDC constants</em>
 * alone — necessarily, since most string literals are ordinary data and a blind
 * rewrite would corrupt them. That's a problem for code that looks up vanilla
 * symbols through strings:
 *
 * <ul>
 *   <li>ASM transformers building {@code new FieldInsnNode(GETFIELD,
 *       "net/minecraft/src/Block", "blockID", "I")} — all three strings are LDC
 *       constants in the transformer's bytecode, none of which the remapper
 *       touches, so after reobfuscation the constructed instruction names
 *       fields/classes that no longer exist.</li>
 *   <li>Reflection callers like {@code Entity.class.getDeclaredField("fallDistance")}
 *       — the {@code Entity.class} reference IS rewritten (it's a Type LDC), but
 *       the {@code "fallDistance"} string isn't, so the lookup throws
 *       {@code NoSuchFieldException}.</li>
 * </ul>
 *
 * <p>This transform fixes both by walking only classes whose internal name
 * starts with one of the configured package prefixes (driven by the
 * {@code STRING_REMAP_PACKAGES} task parameter), so it's opt-in and a no-op
 * for any user that doesn't request it.</p>
 *
 * <h3>Rules</h3>
 * <ol>
 *   <li><b>Internal class names</b> — slash form, e.g.
 *       {@code "net/minecraft/src/Block"} → mapped obf name. Detected by
 *       direct equality with a {@link Mappings#classes} key.</li>
 *   <li><i>(Future)</i> Dotted class names, e.g.
 *       {@code "net.minecraft.src.Block"} — converted to slash form before
 *       lookup, replaced in place using the same separator the source used.</li>
 *   <li><i>(Future)</i> Type descriptors, e.g.
 *       {@code "(Lnet/minecraft/src/IBlockAccess;IIII)Z"} — parsed token by
 *       token, each {@code L<class>;} rewritten.</li>
 *   <li><i>(Future)</i> Member names with explicit context — bare strings
 *       like {@code "blockID"} only when the surrounding bytecode pattern
 *       (ASM node constructor / {@code Class.getDeclaredField} call)
 *       unambiguously identifies the owning class.</li>
 * </ol>
 */
public final class StringLiteralRemapper implements Injection {

    private final Mappings mappings;
    private final String[] packagePrefixes;
    /** Set during {@link #transform} so member helpers can look up class data
     *  without threading the storage through every method. Cleared on exit. */
    private ClassStorage storage;

    /** Deobf method name → obf method name. Populated once per {@link #transform}
     *  by walking every class in storage and recording each (deobf-name) →
     *  (obf-name) mapping. Names that map to multiple distinct obf names across
     *  different classes are NOT stored — Rule 5 won't rewrite them, since we
     *  can't pick the right one without owner context. */
    private Map<String, String> uniqueMethodNameMap;
    private Map<String, String> uniqueFieldNameMap;

    /** Higher-precision lookup: deobf name + "|" + deobf descriptor → obf name.
     *  Uses descriptor context to disambiguate cases where the same method
     *  name appears on multiple classes with different obf renames (e.g.,
     *  Beta has {@code handleKeyPress(IZ)V} on EntityPlayerSP mapped to "a"
     *  AND {@code handleKeyPress(LRailLogic;)Z} on another class mapped to
     *  "c" — same name, different desc, different obf). The unique-name map
     *  drops these conflicts; this map keeps them apart. */
    private Map<String, String> nameDescToObfMethodMap;
    private Map<String, String> nameDescToObfFieldMap;

    /**
     * @param mappings        the deobf↔obf mapping data (from
     *                        {@code TaskReobfuscate.getMappings}).
     * @param packagePrefixes internal-name prefixes (slash form, ending with
     *                        {@code /}) of classes to process. Empty array =
     *                        no-op.
     */
    public StringLiteralRemapper(Mappings mappings, String[] packagePrefixes) {
        this.mappings = mappings;
        this.packagePrefixes = normalize(packagePrefixes);
    }

    /**
     * Accepts user input in any of {@code me.M41G.nclient},
     * {@code me.M41G.nclient.}, {@code me/M41G/nclient}, {@code me/M41G/nclient/}
     * and normalizes them all to slash-form ending with a trailing slash, which
     * is what JVM internal names use. Removes empty entries while we're at it.
     *
     * <p>Also strips a leading {@code key=} fragment (e.g. someone pasted the
     * whole {@code options.cfg} line {@code stringremap=me/M41G/nclient/}). JVM
     * internal class names can't legally contain {@code =} so anything before
     * the first {@code =} is always bogus.</p>
     */
    private static String[] normalize(String[] raw) {
        if (raw == null) return new String[0];
        java.util.List<String> out = new java.util.ArrayList<>(raw.length);
        for (String s : raw) {
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty()) continue;
            int eq = t.indexOf('=');
            if (eq >= 0) t = t.substring(eq + 1).trim();
            if (t.isEmpty()) continue;
            t = t.replace('.', '/');
            if (!t.endsWith("/")) t = t + "/";
            out.add(t);
        }
        return out.toArray(new String[0]);
    }

    @Override
    public void transform(ClassStorage storage) {
        if (packagePrefixes.length == 0) return;
        if (mappings == null || mappings.classes == null || mappings.classes.isEmpty()) {
            // Nothing to remap to — bail rather than walking every class for nothing.
            return;
        }

        // Diagnostic: surface the configured prefixes + total class count so a
        // mismatch (typo, wrong separator, missing slash) is debuggable from the
        // build log without rebuilding RetroMCP.
        int totalClasses = storage.getClasses().size();
        StringBuilder prefixes = new StringBuilder();
        for (int i = 0; i < packagePrefixes.length; i++) {
            if (i > 0) prefixes.append(", ");
            prefixes.append(packagePrefixes[i]);
        }
        System.out.println("[stringremap] " + totalClasses + " class(es) in storage; "
                + "prefix(es): " + prefixes);

        this.storage = storage;
        this.uniqueMethodNameMap = buildUniqueNameIndex(true);
        this.uniqueFieldNameMap  = buildUniqueNameIndex(false);
        this.nameDescToObfMethodMap = buildNameDescIndex(true);
        this.nameDescToObfFieldMap  = buildNameDescIndex(false);
        try {
            int classesProcessed = 0;
            int rewrites = 0;
            for (ClassNode cls : storage.getClasses()) {
                if (!isTarget(cls.name)) continue;
                classesProcessed++;
                if (cls.methods == null) continue;
                for (MethodNode m : cls.methods) {
                    // Pass 1: context-driven member-name rewrites. Must run
                    // before pass 2 so owner LDCs are still in deobf form when
                    // looking up names against the mapping tables.
                    rewrites += rewriteAsmNodeConstructors(m);
                    rewrites += rewriteReflectionLookups(m);
                    rewrites += rewriteAsmNodeNameChecks(m);
                    // Pass 2: standalone LDC strings (Rules 1-3). Already-
                    // rewritten LDCs from pass 1 are now obf names that won't
                    // match deobf mapping keys, so they're left alone here.
                    rewrites += rewriteMethod(m);
                }
            }

            System.out.println("[stringremap] processed " + classesProcessed
                    + " class(es), " + rewrites + " string literal(s) rewritten");
        } finally {
            this.storage = null;
            this.uniqueMethodNameMap = null;
            this.uniqueFieldNameMap = null;
            this.nameDescToObfMethodMap = null;
            this.nameDescToObfFieldMap = null;
        }
    }

    /**
     * Build a "deobf name + '|' + deobf desc → obf name" index. The
     * combined key is unique enough to disambiguate the conflict cases
     * unique-name index drops. As with the unique-name index, conflicts on
     * the same (name, desc) key (rare but possible across non-related
     * classes) are dropped to avoid wrong rewrites.
     */
    private Map<String, String> buildNameDescIndex(boolean methods) {
        Map<String, String> result = new HashMap<>();
        Set<String> conflicts = new HashSet<>();
        for (ClassNode cls : storage.getClasses()) {
            if (methods) {
                if (cls.methods == null) continue;
                for (MethodNode m : cls.methods) {
                    if ("<init>".equals(m.name) || "<clinit>".equals(m.name)) continue;
                    String obf = mappings.methods.optGet(cls.name, m.desc, m.name);
                    if (obf == null || obf.equals(m.name)) continue;
                    indexUnique(result, conflicts, m.name + "|" + m.desc, obf);
                }
            } else {
                if (cls.fields == null) continue;
                for (FieldNode f : cls.fields) {
                    String obf = mappings.fields.optGet(cls.name, f.desc, f.name);
                    if (obf == null || obf.equals(f.name)) continue;
                    indexUnique(result, conflicts, f.name + "|" + f.desc, obf);
                }
            }
        }
        return result;
    }

    /**
     * Build a "deobf member name → obf member name" lookup by walking every
     * class in storage and querying the rename map for each member. Only names
     * with a UNIQUE obf mapping (same obf name across every class that owns
     * a member of that name) are kept — ambiguous ones are dropped because we
     * can't pick the right obf name without owner context, and a wrong choice
     * silently breaks runtime behavior.
     *
     * <p>For most overridden virtual methods (e.g. {@code shouldSideBeRendered}
     * inherited from Block) the obfuscator preserves a single obf name across
     * the whole hierarchy, so the unique check passes. For one-off helper
     * names like {@code startGame} on Minecraft the unique check trivially
     * passes too. Generic names like {@code init} or {@code render} are
     * usually ambiguous and get skipped — that's fine, transformers don't
     * tend to match against names that generic anyway.</p>
     */
    private Map<String, String> buildUniqueNameIndex(boolean methods) {
        Map<String, String> result = new HashMap<>();
        Set<String> conflicts = new HashSet<>();
        for (ClassNode cls : storage.getClasses()) {
            if (methods) {
                if (cls.methods == null) continue;
                for (MethodNode m : cls.methods) {
                    if ("<init>".equals(m.name) || "<clinit>".equals(m.name)) continue;
                    String obf = mappings.methods.optGet(cls.name, m.desc, m.name);
                    if (obf == null || obf.equals(m.name)) continue;
                    indexUnique(result, conflicts, m.name, obf);
                }
            } else {
                if (cls.fields == null) continue;
                for (FieldNode f : cls.fields) {
                    String obf = mappings.fields.optGet(cls.name, f.desc, f.name);
                    if (obf == null || obf.equals(f.name)) continue;
                    indexUnique(result, conflicts, f.name, obf);
                }
            }
        }
        return result;
    }

    private static void indexUnique(Map<String, String> result, Set<String> conflicts,
                                     String deobf, String obf) {
        if (conflicts.contains(deobf)) return;
        String existing = result.get(deobf);
        if (existing == null) {
            result.put(deobf, obf);
        } else if (!existing.equals(obf)) {
            // Saw a different obf name for the same deobf name on another class.
            // Can't disambiguate without owner context; drop both.
            result.remove(deobf);
            conflicts.add(deobf);
        }
    }

    /**
     * Walks the instruction list once and rewrites any LDC string constant that
     * matches one of the implemented rules. Returns the number of LDCs changed.
     */
    private int rewriteMethod(MethodNode method) {
        if (method.instructions == null || method.instructions.size() == 0) return 0;

        int changed = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof LdcInsnNode)) continue;
            LdcInsnNode ldc = (LdcInsnNode) insn;
            if (!(ldc.cst instanceof String)) continue;

            String original = (String) ldc.cst;
            String rewritten = remapString(original);
            if (rewritten != null && !rewritten.equals(original)) {
                ldc.cst = rewritten;
                changed++;
            }
        }
        return changed;
    }

    /**
     * Returns the rewritten value of {@code s} or {@code null} if no rule
     * matched. Each rule is mutually exclusive: a string is at most one of
     * {a class name, a descriptor, a member name} so we early-return on the
     * first hit.
     */
    private String remapString(String s) {
        if (s == null || s.isEmpty()) return null;

        // Rule 3 — type descriptor. Cheapest discriminator (single char check)
        // and avoids passing descriptors through the class-mapping table where
        // they'd never match anyway. Method descriptors start with '(',
        // field descriptors with one of the JVM type sigils 'L' / '['.
        char first = s.charAt(0);
        if (first == '(' || first == '[' || (first == 'L' && s.endsWith(";"))) {
            String d = remapDescriptor(s);
            if (d != null && !d.equals(s)) return d;
            // Don't fall through to class-mapping lookup for things that
            // structurally look like descriptors but didn't change. Avoids the
            // (pathological) case where a descriptor string happens to coincide
            // with an unrelated class mapping key.
            return null;
        }

        // Rule 1 — slash-form internal class name. Direct mapping lookup.
        // Covers `"net/minecraft/src/Block"` style strings used as ASM owner
        // arguments.
        String slashHit = mappings.classes.get(s);
        if (slashHit != null) return slashHit;

        // Rule 2 — dotted binary class name. Convert dot->slash, look up, then
        // re-emit using the SAME separator the source used (callers like
        // Class.forName expect dots; ASM owners expect slashes — preserving
        // the original sigil keeps both happy).
        if (s.indexOf('.') >= 0 && s.indexOf('/') < 0) {
            String slashed = s.replace('.', '/');
            String dottedHit = mappings.classes.get(slashed);
            if (dottedHit != null) return dottedHit.replace('/', '.');
        }

        return null;
    }

    // =========================================================================
    // Rule 4 — context-driven member-name rewrite via ASM node constructors.
    //
    // Detects the bytecode shape produced by user code like
    //
    //     new FieldInsnNode(GETFIELD, "net/minecraft/src/Block", "blockID", "I")
    //     new MethodInsnNode(INVOKESTATIC, "...", "shouldSideBeRendered", "(...)Z", false)
    //
    // For each call site, looks back at the consecutive LDC pushes that
    // supplied the (owner, name, desc) arguments. If the owner string is a
    // known mapped class, the name is remapped against the field / method
    // table for that owner, and the descriptor is rewritten through
    // {@link #remapDescriptor}. All three LDCs are mutated together so the
    // emitted instruction lines up with the obfuscated jar.
    //
    // Standalone LDCs that don't form a recognised constructor pattern are
    // left to {@link #rewriteMethod} (pass 2), which only applies the
    // unambiguous rules 1–3.
    // =========================================================================

    /** ASM internal names for the two node constructors we recognise. */
    private static final String FIELD_INSN_NODE  = "org/objectweb/asm/tree/FieldInsnNode";
    private static final String METHOD_INSN_NODE = "org/objectweb/asm/tree/MethodInsnNode";

    /** {@code (int, String, String, String)V} — both FieldInsnNode and the
     *  3-string MethodInsnNode share this descriptor. */
    private static final String CTOR_DESC_3STR =
            "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
    /** {@code (int, String, String, String, boolean)V} — 5-arg MethodInsnNode. */
    private static final String CTOR_DESC_3STR_BOOL =
            "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V";

    private int rewriteAsmNodeConstructors(MethodNode method) {
        if (method.instructions == null || method.instructions.size() == 0) return 0;
        int rewrites = 0;

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.INVOKESPECIAL) continue;
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) insn;
            if (!"<init>".equals(call.name)) continue;

            boolean isField  = FIELD_INSN_NODE.equals(call.owner)
                    && CTOR_DESC_3STR.equals(call.desc);
            boolean isMethod = METHOD_INSN_NODE.equals(call.owner)
                    && (CTOR_DESC_3STR.equals(call.desc) || CTOR_DESC_3STR_BOOL.equals(call.desc));
            if (!isField && !isMethod) continue;

            // Bytecode pattern (FieldInsnNode + 3-string MethodInsnNode):
            //   ... opcode-push -> LDC owner -> LDC name -> LDC desc -> INVOKESPECIAL
            // 5-arg MethodInsnNode adds an isInterface boolean push between
            // the desc LDC and the constructor call.
            AbstractInsnNode cursor = previousReal(insn);
            if (isMethod && CTOR_DESC_3STR_BOOL.equals(call.desc)) {
                cursor = previousReal(cursor); // skip the boolean push
            }
            LdcInsnNode descLdc  = asStringLdc(cursor);
            LdcInsnNode nameLdc  = asStringLdc(descLdc  != null ? previousReal(descLdc)  : null);
            LdcInsnNode ownerLdc = asStringLdc(nameLdc  != null ? previousReal(nameLdc)  : null);
            if (descLdc == null || nameLdc == null || ownerLdc == null) continue;

            String owner = (String) ownerLdc.cst;
            String name  = (String) nameLdc.cst;
            String desc  = (String) descLdc.cst;

            // Owner must be a known mapped class. If not, the user is targeting
            // either their own class or a class outside the mapping scope —
            // leave the entire triple alone.
            String mappedOwner = mappings.classes.get(owner);
            if (mappedOwner == null) continue;

            // Both rename maps' optGet take args in (owner, desc, name) order.
            // The 3-string Reference ctor IS (owner, name, desc), but optGet
            // passes its own args as (arg1, arg3, arg2) — so the *call* signature
            // ends up swapped from the ctor signature. Verified by decompiling
            // FieldRenameMap / MethodRenameMap in rdi 1.1.
            String mappedName = isField
                    ? mappings.fields.optGet(owner, desc, name)
                    : mappings.methods.optGet(owner, desc, name);
            String mappedDesc = remapDescriptor(desc);

            ownerLdc.cst = mappedOwner;
            rewrites++;
            if (mappedName != null && !mappedName.equals(name)) {
                nameLdc.cst = mappedName;
                rewrites++;
            }
            if (mappedDesc != null && !mappedDesc.equals(desc)) {
                descLdc.cst = mappedDesc;
                rewrites++;
            }
        }
        return rewrites;
    }

    /** Walks back skipping non-real nodes (labels, line numbers, frames). */
    private static AbstractInsnNode previousReal(AbstractInsnNode insn) {
        if (insn == null) return null;
        AbstractInsnNode prev = insn.getPrevious();
        while (prev != null && prev.getOpcode() < 0) {
            prev = prev.getPrevious();
        }
        return prev;
    }

    /** Returns {@code insn} cast as an LDC of a String constant, or null. */
    private static LdcInsnNode asStringLdc(AbstractInsnNode insn) {
        if (!(insn instanceof LdcInsnNode)) return null;
        LdcInsnNode ldc = (LdcInsnNode) insn;
        return ldc.cst instanceof String ? ldc : null;
    }

    /** Returns the Type carried by an LDC of a {@code Type} constant, or null.
     *  This is the bytecode shape of source-level {@code Foo.class}. */
    private static Type asTypeLdc(AbstractInsnNode insn) {
        if (!(insn instanceof LdcInsnNode)) return null;
        LdcInsnNode ldc = (LdcInsnNode) insn;
        return ldc.cst instanceof Type ? (Type) ldc.cst : null;
    }

    // =========================================================================
    // Rule 4 — context-driven member-name rewrite via reflection lookups.
    //
    // Detects three call shapes that all consume a (Class, String) pair on the
    // stack and look the field up by that name:
    //
    //   1. Direct:        SomeClass.class.getDeclaredField("name")
    //                                    .getField("name")
    //   2. Local var:     Class<?> c = SomeClass.class;
    //                     c.getDeclaredField("name");
    //   3. Helper method: Field f = grab(SomeClass.class, "name");
    //                     // where grab returns Field and takes (Class, String)
    //
    // The bytecode arg layout is identical for all three (class on stack first,
    // name on stack second), so one walk-back routine handles them. The class
    // reference is resolved either as a direct Type LDC or by tracing an
    // ALOAD back to its source ASTORE within the same method.
    //
    // {@code getDeclaredMethod} / {@code getMethod} are NOT handled — their
    // varargs Class[] arg makes the bytecode shape much harder to walk back
    // through, and our codebase doesn't use them. Easy follow-up if needed.
    // =========================================================================

    /** Return type that uniquely identifies a field-lookup helper signature. */
    private static final String FIELD_LOOKUP_RET = ")Ljava/lang/reflect/Field;";

    private int rewriteReflectionLookups(MethodNode method) {
        if (method.instructions == null || method.instructions.size() == 0) return 0;
        int rewrites = 0;

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) insn;
            if (!isFieldLookup(call)) continue;

            // Pattern: <push class> -> LDC name -> INVOKE*.
            LdcInsnNode nameLdc = asStringLdc(previousReal(call));
            if (nameLdc == null) continue;

            String owner = resolveClassRef(previousReal(nameLdc));
            if (owner == null) continue;

            // Filter to mapped classes only — anything else is either a user
            // class (which we don't want to remap) or out of scope.
            if (!mappings.classes.containsKey(owner)) continue;

            String name = (String) nameLdc.cst;
            String desc = findFieldDesc(owner, name);
            if (desc == null) continue;     // not a known field on that class

            // optGet takes (owner, desc, name) — see ASM-node-constructor
            // rewriter above for the explanation.
            String mapped = mappings.fields.optGet(owner, desc, name);
            if (mapped != null && !mapped.equals(name)) {
                nameLdc.cst = mapped;
                rewrites++;
            }
        }
        return rewrites;
    }

    /**
     * Direct {@code Class.getDeclaredField/getField} (INVOKEVIRTUAL) OR any
     * {@code INVOKE*} with the helper signature {@code (Class, String)Field}.
     * The helper case lets users factor out a {@code grab(c, "name")} wrapper
     * around {@code getDeclaredField} without losing the rewrite.
     */
    private static boolean isFieldLookup(MethodInsnNode call) {
        if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                && "java/lang/Class".equals(call.owner)
                && ("getDeclaredField".equals(call.name) || "getField".equals(call.name))
                && "(Ljava/lang/String;)Ljava/lang/reflect/Field;".equals(call.desc)) {
            return true;
        }
        return call.desc != null
                && call.desc.startsWith("(Ljava/lang/Class;Ljava/lang/String;)")
                && call.desc.endsWith(FIELD_LOOKUP_RET);
    }

    /**
     * Returns the internal name of the class pushed on the stack at this insn,
     * or {@code null} if it can't be determined statically. Handles:
     *
     * <ul>
     *   <li>Direct Type LDC (the most common shape — bytecode of {@code Foo.class}).</li>
     *   <li>{@code ALOAD slot} that was last assigned by {@code ASTORE slot} of
     *       a Type LDC — the {@code Class<?> c = Foo.class;} pattern. We walk
     *       back through the instruction list to find the latest matching
     *       ASTORE and check what fed it.</li>
     * </ul>
     *
     * Anything more complex (method-parameter Class, field-loaded Class,
     * forName lookup) returns null and the rewrite is skipped — false positive
     * cost is much higher than missing a rewrite.
     */
    private String resolveClassRef(AbstractInsnNode insn) {
        if (insn == null) return null;
        Type direct = asTypeLdc(insn);
        if (direct != null) return direct.getInternalName();

        if (insn instanceof VarInsnNode && insn.getOpcode() == Opcodes.ALOAD) {
            int slot = ((VarInsnNode) insn).var;
            for (AbstractInsnNode prev = previousReal(insn); prev != null; prev = previousReal(prev)) {
                if (prev instanceof VarInsnNode
                        && prev.getOpcode() == Opcodes.ASTORE
                        && ((VarInsnNode) prev).var == slot) {
                    Type stored = asTypeLdc(previousReal(prev));
                    return stored != null ? stored.getInternalName() : null;
                }
            }
        }
        return null;
    }

    /**
     * Returns the descriptor of the named field declared on {@code owner} (or
     * inherited from a superclass that's also in the storage). {@code null}
     * when the field isn't found — the caller treats that as "leave the LDC
     * alone" rather than guessing a wrong descriptor.
     */
    private String findFieldDesc(String ownerInternalName, String fieldName) {
        if (storage == null) return null;
        String current = ownerInternalName;
        // Walk up the superclass chain — Beta uses inheritance heavily for
        // Entity/EntityLiving/EntityPlayer, and reflection lookups against the
        // most-derived class find inherited fields too.
        while (current != null) {
            ClassNode cls = storage.getClass(current);
            if (cls == null) break;
            if (cls.fields != null) {
                for (FieldNode f : cls.fields) {
                    if (fieldName.equals(f.name)) return f.desc;
                }
            }
            current = cls.superName;
            // Cut off the walk at java.lang.Object or anything outside the
            // mapped Minecraft classes — those won't be in storage anyway, but
            // explicitly stopping avoids a redundant lookup.
            if (current == null || "java/lang/Object".equals(current)) break;
        }
        return null;
    }

    // =========================================================================
    // Rule 5 — context-driven member-name rewrite via ASM-tree-node name
    // equality checks.
    //
    // Detects:
    //
    //     methodNode.name.equals("startGame")
    //     fieldNode.name.equals("blockID")
    //     methodNode.name.equalsIgnoreCase("...")
    //
    // Bytecode shape:
    //
    //     ALOAD <local>                        ; the MethodNode/FieldNode
    //     GETFIELD <asm-tree-node>.name        ; load its name field
    //     LDC "<member-name>"                  ; the candidate name
    //     INVOKEVIRTUAL String.equals/equalsIgnoreCase
    //
    // Without owner context (we don't know which class the method/field
    // belongs to from this fragment alone), we look up the LDC string in
    // a precomputed "deobf name -> obf name" index. The index only contains
    // names that have a UNIQUE obf mapping across the whole storage. Names
    // with conflicting mappings (e.g., "init" or "render" appearing on
    // multiple classes with different obf renames) are skipped.
    //
    // Why this works in practice for nclient:
    //   - Transformers check method names that are highly specific (e.g.
    //     "startGame", "shouldSideBeRendered", "handleKeyPress"). Specific
    //     names are uniquely mapped.
    //   - Overridden virtual methods receive a single obf name across the
    //     whole inheritance chain (otherwise virtual dispatch would break),
    //     so "shouldSideBeRendered" on Block/BlockFluid/etc. all map to the
    //     same obf name.
    //
    // Without Rule 5, transformers run at build time against an obf jar,
    // see method names like "f"/"a"/etc., and never match the deobf string
    // literals their own code carries. Net effect: build-time transformation
    // is a silent no-op for everything that isn't a class-name or descriptor
    // check. With Rule 5, the literals get rewritten to obf names at reobf
    // time and the build-time transform fires correctly.
    // =========================================================================

    private static final String ASM_TREE_PKG = "org/objectweb/asm/tree/";

    private int rewriteAsmNodeNameChecks(MethodNode method) {
        if (method.instructions == null || method.instructions.size() == 0) return 0;
        int rewrites = 0;

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!isStringEqualsCall(insn)) continue;

            // Pattern: ... GETFIELD <node>.name -> LDC "name" -> INVOKEVIRTUAL String.equals
            LdcInsnNode nameLdc = asStringLdc(previousReal(insn));
            if (nameLdc == null) continue;
            AbstractInsnNode getField = previousReal(nameLdc);
            Boolean isMethodName = isAsmTreeNameGetField(getField);
            if (isMethodName == null) continue;

            String origName = (String) nameLdc.cst;

            // Try the higher-precision (name, desc) lookup first by scanning
            // forward for an immediately-following `m.desc.equals(...)` check.
            // Most transformers gate their work on BOTH name and desc, so this
            // pairs them up cleanly. The scan window is intentionally tight —
            // we want them close together inside the same conditional block.
            LdcInsnNode descLdc = findFollowingDescCheck(insn, isMethodName ? true : false);
            if (descLdc != null) {
                String key = origName + "|" + descLdc.cst;
                Map<String, String> combined = isMethodName ? nameDescToObfMethodMap : nameDescToObfFieldMap;
                String obf = combined.get(key);
                if (obf != null && !obf.equals(origName)) {
                    nameLdc.cst = obf;
                    rewrites++;
                    continue;
                }
            }

            // Fallback: standalone name lookup. Skipped on conflicts (different
            // obf names across classes for the same deobf name), which is the
            // safe behavior — better to leave the LDC alone than rewrite to
            // the wrong target.
            Map<String, String> index = isMethodName ? uniqueMethodNameMap : uniqueFieldNameMap;
            String mapped = index.get(origName);
            if (mapped != null && !mapped.equals(origName)) {
                nameLdc.cst = mapped;
                rewrites++;
            }
        }
        return rewrites;
    }

    private static boolean isStringEqualsCall(AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode)) return false;
        MethodInsnNode call = (MethodInsnNode) insn;
        if (call.getOpcode() != Opcodes.INVOKEVIRTUAL) return false;
        if (!"java/lang/String".equals(call.owner)) return false;
        if ("equals".equals(call.name) && "(Ljava/lang/Object;)Z".equals(call.desc)) return true;
        if ("equalsIgnoreCase".equals(call.name) && "(Ljava/lang/String;)Z".equals(call.desc)) return true;
        return false;
    }

    /**
     * Scan forward from {@code start} for a sibling {@code .desc.equals(LDC)}
     * check on the same kind of ASM tree node ({@code MethodNode} for method
     * checks, {@code FieldNode} for field checks). Returns the LDC of the
     * descriptor string, or null if not found within the scan window.
     *
     * <p>The scan stops at the next {@code IRETURN} / {@code RETURN} /
     * {@code ATHROW} (we don't cross method-exit boundaries) and limits
     * itself to a small forward window to avoid pairing unrelated checks.</p>
     */
    private LdcInsnNode findFollowingDescCheck(AbstractInsnNode start, boolean methodNameCheck) {
        int hops = 0;
        for (AbstractInsnNode cur = start.getNext(); cur != null && hops < 30; cur = cur.getNext()) {
            int op = cur.getOpcode();
            if (op == Opcodes.IRETURN || op == Opcodes.RETURN || op == Opcodes.ATHROW) return null;
            if (op < 0) continue; // labels / line numbers / frames
            hops++;

            if (!isStringEqualsCall(cur)) continue;
            LdcInsnNode candidate = asStringLdc(previousReal(cur));
            if (candidate == null) continue;
            AbstractInsnNode getField = previousReal(candidate);
            if (!isAsmTreeDescGetField(getField, methodNameCheck)) continue;

            return candidate;
        }
        return null;
    }

    /**
     * True if {@code insn} is a GETFIELD on an ASM tree node's {@code desc}
     * field. Mirrors {@link #isAsmTreeNameGetField} but for descriptor lookups.
     * The {@code methodContext} flag picks the right tree-node owner (we don't
     * accept a FieldNode.desc check inside a MethodNode.name-paired pattern).
     */
    private static boolean isAsmTreeDescGetField(AbstractInsnNode insn, boolean methodContext) {
        if (!(insn instanceof FieldInsnNode)) return false;
        FieldInsnNode get = (FieldInsnNode) insn;
        if (get.getOpcode() != Opcodes.GETFIELD) return false;
        if (!"desc".equals(get.name)) return false;
        if (!"Ljava/lang/String;".equals(get.desc)) return false;
        if (get.owner == null || !get.owner.startsWith(ASM_TREE_PKG)) return false;

        String simpleName = get.owner.substring(ASM_TREE_PKG.length());
        if (methodContext) {
            return "MethodNode".equals(simpleName) || "MethodInsnNode".equals(simpleName);
        }
        return "FieldNode".equals(simpleName) || "FieldInsnNode".equals(simpleName);
    }

    /**
     * Returns {@code Boolean.TRUE} if {@code insn} is GETFIELD on an
     * {@code org.objectweb.asm.tree.MethodNode#name} (or any other tree-node
     * class that exposes a method-name field), {@code Boolean.FALSE} for a
     * field-name analogue (FieldNode#name), {@code null} otherwise.
     *
     * <p>The distinction matters because we look the LDC up in different
     * mapping tables: method-name LDCs go through {@code mappings.methods},
     * field-name LDCs through {@code mappings.fields}. A wrong choice would
     * either fail to find a mapping or rewrite to an unrelated name.</p>
     */
    private static Boolean isAsmTreeNameGetField(AbstractInsnNode insn) {
        if (!(insn instanceof FieldInsnNode)) return null;
        FieldInsnNode get = (FieldInsnNode) insn;
        if (get.getOpcode() != Opcodes.GETFIELD) return null;
        if (!"name".equals(get.name)) return null;
        if (!"Ljava/lang/String;".equals(get.desc)) return null;
        if (get.owner == null || !get.owner.startsWith(ASM_TREE_PKG)) return null;

        String simpleName = get.owner.substring(ASM_TREE_PKG.length());
        if ("FieldNode".equals(simpleName) || "FieldInsnNode".equals(simpleName)) {
            return Boolean.FALSE;   // field-name lookup
        }
        // Default everything else under asm.tree to the method-name table.
        // MethodNode and MethodInsnNode are the common cases; subclasses of
        // those (rare) also work because their .name field semantics match.
        return Boolean.TRUE;
    }

    /**
     * Walk a method or field type descriptor and rewrite each {@code L<class>;}
     * token through the class mapping. Returns null on parse failure (the
     * caller treats that as "leave the original string alone").
     */
    private String remapDescriptor(String desc) {
        StringBuilder out = new StringBuilder(desc.length());
        int i = 0;
        int n = desc.length();
        while (i < n) {
            char c = desc.charAt(i);
            // Pass through structural chars and primitive type codes verbatim.
            // Any other char besides 'L' is either a delimiter ('(', ')'),
            // an array prefix ('['), or a primitive (B,S,I,J,F,D,C,Z,V).
            if (c != 'L') {
                out.append(c);
                i++;
                continue;
            }
            int semi = desc.indexOf(';', i + 1);
            if (semi < 0) return null;     // malformed
            String cls = desc.substring(i + 1, semi);
            String mapped = mappings.classes.get(cls);
            out.append('L').append(mapped != null ? mapped : cls).append(';');
            i = semi + 1;
        }
        return out.toString();
    }

    private boolean isTarget(String internalName) {
        if (internalName == null) return false;
        for (String prefix : packagePrefixes) {
            if (internalName.startsWith(prefix)) return true;
        }
        return false;
    }
}
