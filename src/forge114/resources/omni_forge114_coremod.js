function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');

    var HOOKS = 'io/github/avacadowizard120/omni/OmniForge114Hooks';

    function methodMatches(method, names, desc) {
        if (method.desc !== desc) {
            return false;
        }
        for (var i = 0; i < names.length; i++) {
            if (method.name === names[i] || method.name === ASMAPI.mapMethod(names[i])) {
                return true;
            }
        }
        return false;
    }

    function firstInsn(method) {
        var insn = method.instructions.getFirst();
        while (insn !== null && insn.getOpcode() < 0) {
            insn = insn.getNext();
        }
        return insn === null ? method.instructions.getFirst() : insn;
    }

    function previousMeaningful(insn) {
        var cursor = insn.getPrevious();
        while (cursor !== null && cursor.getOpcode() < 0) {
            cursor = cursor.getPrevious();
        }
        return cursor;
    }

    function replaceBooleanMethod(method, hookName) {
        var code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            HOOKS,
            hookName,
            '(Ljava/lang/Object;)Z',
            false
        ));
        code.add(new InsnNode(Opcodes.IRETURN));
        method.instructions.clear();
        method.instructions.add(code);
        method.tryCatchBlocks.clear();
        method.maxStack = 1;
    }

    function insertVoidHead(method, hookName) {
        var code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            HOOKS,
            hookName,
            '(Ljava/lang/Object;)V',
            false
        ));
        method.instructions.insertBefore(firstInsn(method), code);
    }

    function insertBeforeReturns(method, hookName) {
        var changed = 0;
        for (var insn = method.instructions.getFirst(); insn !== null; ) {
            var next = insn.getNext();
            if (insn.getOpcode() === Opcodes.RETURN) {
                var code = new InsnList();
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                code.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    HOOKS,
                    hookName,
                    '(Ljava/lang/Object;)V',
                    false
                ));
                method.instructions.insertBefore(insn, code);
                changed++;
            }
            insn = next;
        }
        return changed;
    }

    function patchMoveRelativeSpeeds(method) {
        var changed = 0;
        for (var insn = method.instructions.getFirst(); insn !== null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode) || insn.getOpcode() !== Opcodes.INVOKEVIRTUAL) {
                continue;
            }
            if (insn.desc !== '(FLnet/minecraft/util/math/Vec3d;)V') {
                continue;
            }

            var vectorLoad = previousMeaningful(insn);
            if (!(vectorLoad instanceof VarInsnNode) || vectorLoad.getOpcode() !== Opcodes.ALOAD) {
                continue;
            }

            var code = new InsnList();
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOKS,
                'directionalSpeedMultiplier',
                '(Ljava/lang/Object;)F',
                false
            ));
            code.add(new InsnNode(Opcodes.FMUL));
            method.instructions.insertBefore(vectorLoad, code);
            changed++;
        }
        return changed;
    }

    function transformMovementInput(classNode) {
        var patched = 0;
        for (var i = 0; i < classNode.methods.size(); i++) {
            var method = classNode.methods.get(i);
            if (methodMatches(method, ['hasForwardImpulse', 'func_223135_b'], '()Z')) {
                replaceBooleanMethod(method, 'hasForwardImpulse');
                patched++;
            }
        }
        ASMAPI.log('INFO', 'Omni patched MovementInput methods: {}', patched);
        return classNode;
    }

    function transformClientPlayer(classNode) {
        var patched = 0;
        for (var i = 0; i < classNode.methods.size(); i++) {
            var method = classNode.methods.get(i);
            if (methodMatches(method, ['hasEnoughImpulseToStartSprinting', 'func_223110_ee'], '()Z')) {
                replaceBooleanMethod(method, 'hasEnoughImpulseToStartSprinting');
                patched++;
            }
        }
        ASMAPI.log('INFO', 'Omni patched ClientPlayerEntity methods: {}', patched);
        return classNode;
    }

    function transformLivingEntity(classNode) {
        var movement = 0;
        var jump = 0;
        for (var i = 0; i < classNode.methods.size(); i++) {
            var method = classNode.methods.get(i);
            if (methodMatches(method, ['travel', 'func_213352_e'], '(Lnet/minecraft/util/math/Vec3d;)V')) {
                movement += patchMoveRelativeSpeeds(method);
            }
            if (methodMatches(method, ['jumpFromGround', 'func_70664_aZ'], '()V')) {
                insertVoidHead(method, 'capturePreJumpVelocity');
                jump += insertBeforeReturns(method, 'redirectSprintJumpBoost');
            }
        }
        ASMAPI.log('INFO', 'Omni patched LivingEntity moveRelative speeds: {}, jump returns: {}', movement, jump);
        return classNode;
    }

    return {
        'movement_input': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.util.MovementInput'
            },
            'transformer': transformMovementInput
        },
        'client_player': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.client.entity.player.ClientPlayerEntity'
            },
            'transformer': transformClientPlayer
        },
        'living_entity': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.entity.LivingEntity'
            },
            'transformer': transformLivingEntity
        }
    };
}
