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
import dev.kyleescobar.runetools.asm.Method;
import dev.kyleescobar.runetools.asm.attributes.code.Instruction;
import dev.kyleescobar.runetools.asm.attributes.code.InstructionType;
import dev.kyleescobar.runetools.asm.attributes.code.Instructions;
import dev.kyleescobar.runetools.asm.execution.Frame;
import dev.kyleescobar.runetools.asm.execution.InstructionContext;
import dev.kyleescobar.runetools.asm.execution.Stack;
import dev.kyleescobar.runetools.asm.execution.StackContext;
import dev.kyleescobar.runetools.asm.pool.Class;
import dev.kyleescobar.runetools.asm.pool.Field;
import dev.kyleescobar.runetools.deob.deobfuscators.mapping.MappingExecutorUtil;
import dev.kyleescobar.runetools.deob.deobfuscators.mapping.ParallelExecutorMapping;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.GetFieldInstruction;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.PushConstantInstruction;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.SetFieldInstruction;
import org.objectweb.asm.MethodVisitor;

public class PutField extends Instruction implements SetFieldInstruction
{
	private Field field;
	private dev.kyleescobar.runetools.asm.Field myField;

	public PutField(Instructions instructions, InstructionType type)
	{
		super(instructions, type);
	}

	public PutField(Instructions instructions, Field field)
	{
		super(instructions, InstructionType.PUTFIELD);
		this.field = field;
	}

	public PutField(Instructions instructions, dev.kyleescobar.runetools.asm.Field field)
	{
		super(instructions, InstructionType.PUTFIELD);
		this.field = field.getPoolField();
		this.myField = field;
	}

	@Override
	public String toString()
	{
		Method m = this.getInstructions().getCode().getMethod();
		return "putfield " + field + " in " + m;
	}

	@Override
	public void accept(MethodVisitor visitor)
	{
		visitor.visitFieldInsn(this.getType().getCode(),
			field.getClazz().getName(),
			field.getName(),
			field.getType().toString());
	}

	@Override
	public InstructionContext execute(Frame frame)
	{
		InstructionContext ins = new InstructionContext(this, frame);
		Stack stack = frame.getStack();

		StackContext value = stack.pop();
		StackContext object = stack.pop();
		ins.pop(value, object);

		if (myField != null)
		{
			frame.getExecution().order(frame, myField);
		}

		return ins;
	}

	@Override
	public Field getField()
	{
		return field;
	}

	@Override
	public dev.kyleescobar.runetools.asm.Field getMyField()
	{
		Class clazz = field.getClazz();

		ClassGroup group = this.getInstructions().getCode().getMethod().getClassFile().getGroup();
		ClassFile cf = group.findClass(clazz.getName());
		if (cf == null)
		{
			return null;
		}

		dev.kyleescobar.runetools.asm.Field f2 = cf.findFieldDeep(field.getName(), field.getType());
		return f2;
	}

	@Override
	public void lookup()
	{
		myField = getMyField();
	}

	@Override
	public void regeneratePool()
	{
		if (myField != null)
		{
			if (getMyField() != myField)
			{
				field = myField.getPoolField();
			}
		}
	}

	@Override
	public void map(ParallelExecutorMapping mapping, InstructionContext ctx, InstructionContext other)
	{
		dev.kyleescobar.runetools.asm.Field myField = this.getMyField();
		dev.kyleescobar.runetools.asm.Field otherField = ((PutField) other.getInstruction()).getMyField();

		assert MappingExecutorUtil.isMaybeEqual(myField.getType(), otherField.getType());

		mapping.map(this, myField, otherField);

		// map assignment
		StackContext object1 = ctx.getPops().get(1),
			object2 = other.getPops().get(1);

		InstructionContext base1 = MappingExecutorUtil.resolve(object1.getPushed(), object1);
		InstructionContext base2 = MappingExecutorUtil.resolve(object2.getPushed(), object2);

		mapGetFieldInstructrions(mapping, base1, base2);

		// map value
		object1 = ctx.getPops().get(0);
		object2 = other.getPops().get(0);

		base1 = MappingExecutorUtil.resolve(object1.getPushed(), object1);
		base2 = MappingExecutorUtil.resolve(object2.getPushed(), object2);

		mapGetFieldInstructrions(mapping, base1, base2);
	}

	private void mapGetFieldInstructrions(ParallelExecutorMapping mapping, InstructionContext base1, InstructionContext base2)
	{
		if (base1.getInstruction() instanceof GetFieldInstruction && base2.getInstruction() instanceof GetFieldInstruction)
		{
			GetFieldInstruction gf1 = (GetFieldInstruction) base1.getInstruction(),
				gf2 = (GetFieldInstruction) base2.getInstruction();

			dev.kyleescobar.runetools.asm.Field f1 = gf1.getMyField();
			dev.kyleescobar.runetools.asm.Field f2 = gf2.getMyField();

			if (f1 != null && f2 != null)
			{
				mapping.map(this, f1, f2);
			}
		}
	}

	private boolean isMaybeEqual(InstructionContext base1, InstructionContext base2)
	{
		if (base1.getInstruction() instanceof GetFieldInstruction && base2.getInstruction() instanceof GetFieldInstruction)
		{
			GetFieldInstruction gf1 = (GetFieldInstruction) base1.getInstruction(),
				gf2 = (GetFieldInstruction) base2.getInstruction();

			dev.kyleescobar.runetools.asm.Field f1 = gf1.getMyField();
			dev.kyleescobar.runetools.asm.Field f2 = gf2.getMyField();

			return MappingExecutorUtil.isMaybeEqual(f1, f2);
		}

		return true;
	}

	@Override
	public boolean isSame(InstructionContext thisIc, InstructionContext otherIc)
	{
		if (thisIc.getInstruction().getClass() != otherIc.getInstruction().getClass())
		{
			return false;
		}

		PutField thisPf = (PutField) thisIc.getInstruction(),
			otherPf = (PutField) otherIc.getInstruction();

		dev.kyleescobar.runetools.asm.Field f1 = thisPf.getMyField();
		dev.kyleescobar.runetools.asm.Field f2 = otherPf.getMyField();

		if ((f1 != null) != (f2 != null))
		{
			return false;
		}

		if (!MappingExecutorUtil.isMaybeEqual(f1.getClassFile(), f2.getClassFile())
			|| !MappingExecutorUtil.isMaybeEqual(f1.getType(), f2.getType()))
		{
			return false;
		}

		// check assignment
		StackContext object1 = thisIc.getPops().get(1),
			object2 = otherIc.getPops().get(1);

		InstructionContext base1 = MappingExecutorUtil.resolve(object1.getPushed(), object1);
		InstructionContext base2 = MappingExecutorUtil.resolve(object2.getPushed(), object2);

		if (!isMaybeEqual(base1, base2))
		{
			return false;
		}

		// check value
		object1 = thisIc.getPops().get(0);
		object2 = otherIc.getPops().get(0);

		base1 = MappingExecutorUtil.resolve(object1.getPushed(), object1);
		base2 = MappingExecutorUtil.resolve(object2.getPushed(), object2);

		if (!isMaybeEqual(base1, base2))
		{
			return false;
		}

		return true;
	}

	@Override
	public boolean canMap(InstructionContext thisIc)
	{
		StackContext value = thisIc.getPops().get(0);
		Instruction i = value.getPushed().getInstruction();

		// sometimes ConstantValue field attributes and inlined into the constructor,
		// which are all constants, so we ignore those mappings here
		if (thisIc.getFrame().getMethod().getName().equals("<init>"))
		{
			if (i instanceof PushConstantInstruction || i instanceof AConstNull)
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void setField(Field field)
	{
		this.field = field;
	}
}
