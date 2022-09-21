/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.kyleescobar.runetools.asm.attributes.code.instructions;

import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.Type;
import dev.kyleescobar.runetools.asm.attributes.code.Instruction;
import dev.kyleescobar.runetools.asm.attributes.code.InstructionType;
import dev.kyleescobar.runetools.asm.attributes.code.Instructions;
import dev.kyleescobar.runetools.asm.pool.Class;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.TypeInstruction;
import dev.kyleescobar.runetools.asm.execution.Frame;
import dev.kyleescobar.runetools.asm.execution.InstructionContext;
import dev.kyleescobar.runetools.asm.execution.Stack;
import dev.kyleescobar.runetools.asm.execution.StackContext;
import dev.kyleescobar.runetools.asm.execution.Value;
import org.objectweb.asm.MethodVisitor;

public class New extends Instruction implements TypeInstruction
{
	private Class clazz;
	private ClassFile myClass;

	public New(Instructions instructions, InstructionType type)
	{
		super(instructions, type);
	}

	public New(Instructions instructions, Class clazz)
	{
		super(instructions, InstructionType.NEW);
		this.clazz = clazz;
	}

	public New(Instructions instructions, ClassFile classFile)
	{
		super(instructions, InstructionType.NEW);
		this.clazz = classFile.getPoolClass();
		this.myClass = classFile;
	}

	@Override
	public void accept(MethodVisitor visitor)
	{
		visitor.visitTypeInsn(this.getType().getCode(), this.getType_().getInternalName());
	}

	@Override
	public InstructionContext execute(Frame frame)
	{
		InstructionContext ins = new InstructionContext(this, frame);
		Stack stack = frame.getStack();
		
		StackContext ctx = new StackContext(ins, Type.getType(clazz), Value.UNKNOWN);
		stack.push(ctx);
		
		ins.push(ctx);
		
		return ins;
	}
	
	public Class getNewClass()
	{
		return clazz;
	}

	public void setNewClass(Class clazz)
	{
		this.clazz = clazz;
		lookup();
	}
	
	@Override
	public void lookup()
	{
		ClassGroup group = this.getInstructions().getCode().getMethod().getClassFile().getGroup();
		myClass = group.findClass(clazz.getName());
	}
	
	@Override
	public void regeneratePool()
	{
		if (myClass != null)
			clazz = myClass.getPoolClass();
	}

	@Override
	public Type getType_()
	{
		return Type.getType(clazz);
	}

	@Override
	public void setType(Type type)
	{
		clazz = new Class(type.getInternalName());
	}
}
