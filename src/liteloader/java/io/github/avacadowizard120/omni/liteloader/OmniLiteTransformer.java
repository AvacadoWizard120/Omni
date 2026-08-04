package io.github.avacadowizard120.omni.liteloader;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class OmniLiteTransformer implements IClassTransformer {
    private static final String HOOKS = "io/github/avacadowizard120/omni/liteloader/OmniLiteHooks";
    private static final OmniLiteMappings.Mapping MAPPING = OmniLiteMappings.CURRENT;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        boolean patchPlayer = matchesClass(
                name,
                transformedName,
                "net.minecraft.client.entity.EntityPlayerSP",
                MAPPING.playerClass
        );
        boolean chatLivesOnPlayer = MAPPING.chatClass == null || MAPPING.chatClass.equals(MAPPING.playerClass);
        boolean patchChat = (patchPlayer && chatLivesOnPlayer) || matchesClass(
                name,
                transformedName,
                "net.minecraft.client.entity.EntityClientPlayerMP",
                MAPPING.chatClass
        );
        if (patchPlayer || patchChat) {
            return transformClass(basicClass, patchPlayer, patchChat, false);
        }
        if (matchesClass(name, transformedName, "net.minecraft.entity.EntityLivingBase", MAPPING.livingClass)) {
            return transformClass(basicClass, false, false, true);
        }
        return basicClass;
    }

    private static byte[] transformClass(
            byte[] basicClass,
            boolean patchPlayer,
            boolean patchChat,
            boolean patchLiving
    ) {
        ClassReader reader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        boolean changed = false;
        int directionalPatches = 0;
        int commandPatches = 0;
        int jumpPatches = 0;
        int movementPatches = 0;
        for (Object methodObject : classNode.methods) {
            MethodNode method = (MethodNode) methodObject;
            if (patchPlayer
                    && "()V".equals(method.desc)
                    && matchesName(method.name, "onLivingUpdate", "func_70636_d", MAPPING.livingUpdateMethod)) {
                int patched = patchDirectionalImpulseChecks(method);
                directionalPatches += patched;
                changed |= patched > 0;
            }
            if (patchChat
                    && "(Ljava/lang/String;)V".equals(method.desc)
                    && matchesName(method.name, "sendChatMessage", "func_71165_d", MAPPING.chatMethod)) {
                changed |= patchCommand(method);
                commandPatches++;
            }
            if (patchLiving
                    && "()V".equals(method.desc)
                    && matchesName(method.name, "jump", "func_70664_aZ", MAPPING.jumpMethod)) {
                if (patchSprintJump(method)) {
                    jumpPatches++;
                    changed = true;
                }
            }
            if (patchLiving && ("(FF)V".equals(method.desc) || "(FFF)V".equals(method.desc))) {
                int patched = patchMovementSpeed(method);
                movementPatches += patched;
                changed |= patched > 0;
            }
        }

        if (!changed) {
            return basicClass;
        }

        System.out.println("[Omni] Patched " + classNode.name
                + " directional=" + directionalPatches
                + " command=" + commandPatches
                + " jump=" + jumpPatches
                + " movement=" + movementPatches);

        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static int patchDirectionalImpulseChecks(MethodNode method) {
        int changed = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
            AbstractInsnNode nextSearch = insn.getNext();
            if (insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.GETFIELD) {
                FieldInsnNode forwardField = (FieldInsnNode) insn;
                AbstractInsnNode threshold = nextMeaningful(forwardField);
                AbstractInsnNode compare = nextMeaningful(threshold);
                AbstractInsnNode jump = nextMeaningful(compare);
                AbstractInsnNode inputGetInsn = previousMeaningful(forwardField);
                AbstractInsnNode loadThis = previousMeaningful(inputGetInsn);

                if (isForwardField(forwardField)
                        && isThreshold(threshold, 0.8F)
                        && isFloatCompare(compare)
                        && isJump(jump, Opcodes.IFLT)
                        && inputGetInsn instanceof FieldInsnNode
                        && inputGetInsn.getOpcode() == Opcodes.GETFIELD
                        && isInputField((FieldInsnNode) inputGetInsn)
                        && loadThis instanceof VarInsnNode
                        && loadThis.getOpcode() == Opcodes.ALOAD
                        && ((VarInsnNode) loadThis).var == 0) {
                    FieldInsnNode inputGet = (FieldInsnNode) inputGetInsn;
                    JumpInsnNode oldJump = (JumpInsnNode) jump;
                    InsnList replacement = new InsnList();
                    replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    replacement.add(new FieldInsnNode(Opcodes.GETFIELD, inputGet.owner, inputGet.name, inputGet.desc));
                    replacement.add(new LdcInsnNode(forwardField.name));
                    replacement.add(new LdcInsnNode(Float.valueOf(0.8F)));
                    replacement.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            HOOKS,
                            "hasDirectionalImpulse",
                            "(Ljava/lang/Object;Ljava/lang/String;F)Z"
                    ));
                    replacement.add(new JumpInsnNode(Opcodes.IFEQ, oldJump.label));

                    nextSearch = jump.getNext();
                    replaceRange(method.instructions, loadThis, jump, replacement);
                    changed++;
                }
            }
            insn = nextSearch;
        }
        return changed;
    }

    private static int patchMovementSpeed(MethodNode method) {
        int changed = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
                continue;
            }

            MethodInsnNode call = (MethodInsnNode) insn;
            int forwardVar;
            if ("(FF)V".equals(method.desc) && "(FFF)V".equals(call.desc)) {
                forwardVar = 2;
            } else if ("(FFF)V".equals(method.desc) && "(FFFF)V".equals(call.desc)) {
                forwardVar = 3;
            } else {
                continue;
            }

            InsnList multiplier = new InsnList();
            multiplier.add(new VarInsnNode(Opcodes.ALOAD, 0));
            multiplier.add(new VarInsnNode(Opcodes.FLOAD, 1));
            multiplier.add(new VarInsnNode(Opcodes.FLOAD, forwardVar));
            multiplier.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HOOKS,
                    "directionalSpeedMultiplier",
                    "(Ljava/lang/Object;FF)F"
            ));
            multiplier.add(new org.objectweb.asm.tree.InsnNode(Opcodes.FMUL));
            method.instructions.insertBefore(call, multiplier);
            changed++;
        }
        return changed;
    }

    private static boolean patchCommand(MethodNode method) {
        LabelNode continueChat = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
        guard.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOKS,
                "handleCommand",
                "(Ljava/lang/String;)Z"
        ));
        guard.add(new JumpInsnNode(Opcodes.IFEQ, continueChat));
        guard.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        guard.add(continueChat);
        guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(guard);
        return true;
    }

    private static boolean patchSprintJump(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
                continue;
            }

            MethodInsnNode call = (MethodInsnNode) insn;
            AbstractInsnNode jump = nextMeaningful(call);
            if (!isSprintCheckCandidate(call) || !isJump(jump, Opcodes.IFEQ)) {
                continue;
            }

            LabelNode target = ((JumpInsnNode) jump).label;
            AbstractInsnNode bodyStart = nextMeaningful(jump);
            AbstractInsnNode bodyEnd = previousMeaningful(target);
            if (bodyStart == null || bodyEnd == null || !containsMotionPatchShape(bodyStart, bodyEnd)) {
                continue;
            }

            InsnList replacement = new InsnList();
            replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
            replacement.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HOOKS,
                    "applyDirectionalSprintJump",
                    "(Ljava/lang/Object;)V"
            ));
            replaceRange(method.instructions, bodyStart, bodyEnd, replacement);
            return true;
        }
        return false;
    }

    private static boolean containsMotionPatchShape(AbstractInsnNode start, AbstractInsnNode end) {
        boolean sawMotionX = false;
        boolean sawMotionZ = false;
        boolean sawYaw = false;
        int doubleFieldWrites = 0;
        int floatFieldReads = 0;
        for (AbstractInsnNode cursor = start; cursor != null; cursor = cursor.getNext()) {
            if (cursor instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) cursor;
                sawMotionX |= matchesName(field.name, "motionX", "field_70159_w", MAPPING.motionXField);
                sawMotionZ |= matchesName(field.name, "motionZ", "field_70179_y", MAPPING.motionZField);
                sawYaw |= matchesName(field.name, "rotationYaw", "field_70177_z", MAPPING.yawField);
                if (field.getOpcode() == Opcodes.PUTFIELD && "D".equals(field.desc)) {
                    doubleFieldWrites++;
                }
                if (field.getOpcode() == Opcodes.GETFIELD && "F".equals(field.desc)) {
                    floatFieldReads++;
                }
            }
            if (cursor == end) {
                break;
            }
        }
        return (sawMotionX && sawMotionZ && sawYaw) || (doubleFieldWrites >= 2 && floatFieldReads >= 1);
    }

    private static boolean isSprintCheckCandidate(MethodInsnNode call) {
        return "()Z".equals(call.desc)
                && (matchesName(call.name, "isSprinting", "func_70051_ag", MAPPING.sprintingMethod)
                || call.getOpcode() == Opcodes.INVOKEVIRTUAL);
    }

    private static void replaceRange(InsnList instructions, AbstractInsnNode start, AbstractInsnNode end, InsnList replacement) {
        instructions.insertBefore(start, replacement);
        for (AbstractInsnNode cursor = start; cursor != null; ) {
            AbstractInsnNode next = cursor.getNext();
            boolean done = cursor == end;
            instructions.remove(cursor);
            if (done) {
                break;
            }
            cursor = next;
        }
    }

    private static AbstractInsnNode nextMeaningful(AbstractInsnNode insn) {
        if (insn == null) {
            return null;
        }
        AbstractInsnNode cursor = insn.getNext();
        while (isNoise(cursor)) {
            cursor = cursor.getNext();
        }
        return cursor;
    }

    private static AbstractInsnNode previousMeaningful(AbstractInsnNode insn) {
        if (insn == null) {
            return null;
        }
        AbstractInsnNode cursor = insn.getPrevious();
        while (isNoise(cursor)) {
            cursor = cursor.getPrevious();
        }
        return cursor;
    }

    private static boolean isNoise(AbstractInsnNode insn) {
        return insn instanceof LabelNode || insn instanceof LineNumberNode || insn instanceof FrameNode;
    }

    private static boolean isForwardField(FieldInsnNode field) {
        return "F".equals(field.desc);
    }

    private static boolean isInputField(FieldInsnNode field) {
        return matchesName(field.name, "movementInput", "field_71158_b", MAPPING.inputField)
                || (field.desc != null && field.desc.startsWith("L"));
    }

    private static boolean isThreshold(AbstractInsnNode insn, float value) {
        return (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Float
                && ((Float) ((LdcInsnNode) insn).cst).floatValue() == value)
                || (insn instanceof VarInsnNode && insn.getOpcode() == Opcodes.FLOAD);
    }

    private static boolean isFloatCompare(AbstractInsnNode insn) {
        return insn != null && (insn.getOpcode() == Opcodes.FCMPL || insn.getOpcode() == Opcodes.FCMPG);
    }

    private static boolean isJump(AbstractInsnNode insn, int opcode) {
        return insn instanceof JumpInsnNode && insn.getOpcode() == opcode;
    }

    private static boolean matchesClass(String name, String transformedName, String named, String obfuscated) {
        return matchesClassName(name, named, obfuscated) || matchesClassName(transformedName, named, obfuscated);
    }

    private static boolean matchesClassName(String actual, String named, String obfuscated) {
        return actual != null && (actual.equals(named)
                || actual.replace('/', '.').equals(named)
                || (obfuscated != null && actual.equals(obfuscated)));
    }

    private static boolean matchesName(String actual, String named, String stable, String obfuscated) {
        return actual != null && (actual.equals(named)
                || actual.equals(stable)
                || (obfuscated != null && actual.equals(obfuscated)));
    }
}
