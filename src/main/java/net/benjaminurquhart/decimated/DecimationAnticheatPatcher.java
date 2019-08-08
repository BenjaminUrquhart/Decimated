package net.benjaminurquhart.decimated;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.google.common.io.Files;

import net.bytebuddy.agent.ByteBuddyAgent;

/*
 * Source:
 * https://gist.github.com/natanbc/c41d7eaca5fbc4e0355bb187bf60bd96
 * Thanks Natan!
 */
public class DecimationAnticheatPatcher {
	
	public static final int ASM_VERSION = Opcodes.ASM5;
	
    public static void patch() throws ClassNotFoundException, UnmodifiableClassException {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                switch(className) {
                /*
                    case "net/decimation/mod/client/managers/DecimationClientAnticheat": {
                        classfileBuffer = patch(classfileBuffer, "isCheating", "()Z", mv -> {
                            mv.visitInsn(Opcodes.ICONST_0);
                            mv.visitInsn(Opcodes.IRETURN);
                            mv.visitMaxs(1, 1);
                            mv.visitEnd();
                        });
                        break;
                    }*/
                    case "net/decimation/mod/utilities/net/messages_minecraft/Message_Cheating": {
                        classfileBuffer = patch(classfileBuffer, "<init>", "(ZJ)V", mv -> {
                        	mv.visitVarInsn(Opcodes.ALOAD, 0);
                        	mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitInsn(Opcodes.ICONST_0);
                            mv.visitFieldInsn(Opcodes.PUTFIELD,
                                    "net/decimation/mod/utilities/net/messages_minecraft/Message_Cheating",
                                    "isCheating", "Z");
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitVarInsn(Opcodes.LLOAD, 2);
                            mv.visitFieldInsn(Opcodes.PUTFIELD,
                                    "net/decimation/mod/utilities/net/messages_minecraft/Message_Cheating",
                                    "directorySize", "J");
                            mv.visitInsn(Opcodes.RETURN);
                            mv.visitMaxs(2, 2);
                            mv.visitEnd();
                        });
                        break;
                    }
                }
                return classfileBuffer;
            }
        }, true);
        instrumentation.retransformClasses(
                //Class.forName("net.decimation.mod.client.managers.DecimationClientAnticheat"),
                Class.forName("net.decimation.mod.utilities.net.messages_minecraft.Message_Cheating")
        );
    }
    
    private static byte[] patch(byte[] code, final String methodName, final String methodDesc, final Consumer<MethodVisitor> patcher) {
        ClassReader cr = new ClassReader(code);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(ASM_VERSION, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            	MethodVisitor v = super.visitMethod(access, name, desc, signature, exceptions);
                if(name.equals(methodName) && desc.equals(methodDesc)) {
                    return new MethodVisitor(ASM_VERSION, v) {
                        @Override
                        public void visitCode() {
                            patcher.accept(this);
                        }
                    };
                }
                return v;
            }
        }, ClassReader.EXPAND_FRAMES);
        try {
        	Files.write(cw.toByteArray(), new File("bytes.class"));
        }
        catch(Exception e) {
        	e.printStackTrace();
        }
        return cw.toByteArray();
    }
}