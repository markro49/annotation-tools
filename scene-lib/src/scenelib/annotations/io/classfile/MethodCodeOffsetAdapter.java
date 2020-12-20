package scenelib.annotations.io.classfile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MethodCodeOffsetAdapter extends MethodVisitor {
  private final ClassReader classReader;
  private final int methodStart;
  private int offset = 0;
  private int codeStart = 0;
  private int attrCount = 0;

  public MethodCodeOffsetAdapter(ClassReader classReader,
      MethodVisitor methodVisitor, int start) {
    super(Opcodes.ASM7, methodVisitor);
    char[] buf = new char[classReader.header];
    this.methodStart = start;
    this.classReader = classReader;
    // const pool size is (not lowest) upper bound of string length
    codeStart = start;
    attrCount = classReader.readUnsignedShort(codeStart + 6);

    // find code attribute
    codeStart += 8;
    while (attrCount > 0) {
      String attrName = classReader.readUTF8(codeStart, buf);
      if ("Code".equals(attrName)) { break; }
      codeStart += 6 + classReader.readInt(codeStart + 2);
      --attrCount;
    }
  }

  private int readInt(int i) {
    return classReader.readInt(codeStart + i);
  }

  public int getMethodCodeStart() { return methodStart; }

  public int getMethodCodeOffset() { return offset; }

  public int getClassCodeOffset() { return codeStart + offset; }

  @Override
  public void visitFieldInsn(int opcode,
      String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    offset += 3;
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    offset += 3;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    ++offset;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    offset += opcode == Opcodes.SIPUSH ? 3 : 2;
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor,
      Handle bsm, Object... bsmArgs) {
    super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
    offset += 5;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    offset += 3;
  }

  @Override
  public void visitLdcInsn(Object cst) {
    super.visitLdcInsn(cst);
    offset += 2;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys,
      Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    offset += 8 - ((offset - codeStart) & 3);
    offset += 4 + 8 * readInt(offset);
  }

  @Deprecated
  @Override
  public void visitMethodInsn(int opcode,
      String owner, String name, String descriptor) {
    super.visitMethodInsn(opcode, owner, name, descriptor);
    offset += opcode == Opcodes.INVOKEINTERFACE ? 5 : 3;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    offset += opcode == Opcodes.INVOKEINTERFACE ? 5 : 3;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int dims) {
    super.visitMultiANewArrayInsn(descriptor, dims);
    offset += 4;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max,
      Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    offset += 8 - ((offset - codeStart) & 3);
    offset += 4 * (readInt(offset + 4) - readInt(offset) + 3);
  }

  @Override
  public void visitTypeInsn(int opcode, String descriptor) {
    super.visitTypeInsn(opcode, descriptor);
    offset += 3;
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    offset += var < 4 ? 1 : 2;
  }

  @Override
  public void visitEnd() {
    offset = -1;  // invalidated
  }
}
