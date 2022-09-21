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

import java.util.Collections;
import java.util.List;

import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.Field;
import dev.kyleescobar.runetools.asm.attributes.code.Instruction;
import dev.kyleescobar.runetools.asm.attributes.code.InstructionType;
import dev.kyleescobar.runetools.asm.attributes.code.Instructions;
import dev.kyleescobar.runetools.asm.execution.*;
import dev.kyleescobar.runetools.asm.pool.Class;
import dev.kyleescobar.runetools.asm.pool.Method;
import dev.kyleescobar.runetools.asm.signature.Signature;
import dev.kyleescobar.runetools.deob.deobfuscators.mapping.MappingExecutorUtil;
import dev.kyleescobar.runetools.deob.deobfuscators.mapping.ParallelExecutorMapping;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.GetFieldInstruction;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.InvokeInstruction;
import dev.kyleescobar.runetools.asm.execution.Execution;
import dev.kyleescobar.runetools.asm.execution.Frame;
import dev.kyleescobar.runetools.asm.execution.InstructionContext;
import dev.kyleescobar.runetools.asm.execution.Stack;
import dev.kyleescobar.runetools.asm.execution.StackContext;
import dev.kyleescobar.runetools.asm.execution.Value;
import org.objectweb.asm.MethodVisitor;

public class InvokeStatic extends Instruction implements InvokeInstruction
{
	private Method method;
	private dev.kyleescobar.runetools.asm.Method myMethod;

	public InvokeStatic(Instructions instructions, InstructionType type)
	{
		super(instructions, type);
	}

	public InvokeStatic(Instructions instructions, Method method)
	{
		super(instructions, InstructionType.INVOKESTATIC);

		this.method = method;
	}

	public InvokeStatic(Instructions instructions, dev.kyleescobar.runetools.asm.Method method)
	{
		super(instructions, InstructionType.INVOKESTATIC);
		this.method = method.getPoolMethod();
		this.myMethod = method;
	}

	@Override
	public void accept(MethodVisitor visitor)
	{
		visitor.visitMethodInsn(this.getType().getCode(),
			method.getClazz().getName(),
			method.getName(),
			method.getType().toString(),
			false);
	}

	@Override
	public String toString()
	{
		return "invokestatic " + method + " in " + this.getInstructions().getCode().getMethod();// + " at pc 0x" + Integer.toHexString(this.getPc());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<dev.kyleescobar.runetools.asm.Method> getMethods()
	{
		return myMethod != null ? Collections.singletonList(myMethod) : Collections.EMPTY_LIST;
	}

	@Override
	public InstructionContext execute(Frame frame)
	{
		InstructionContext ins = new InstructionContext(this, frame);
		Stack stack = frame.getStack();

		int count = method.getType().size();

		for (int i = 0; i < count; ++i)
		{
			StackContext arg = stack.pop();
			ins.pop(arg);
		}

		if (!method.getType().isVoid())
		{
			StackContext ctx = new StackContext(ins,
				method.getType().getReturnValue(),
				Value.UNKNOWN
			);
			stack.push(ctx);

			ins.push(ctx);
		}

		if (myMethod != null)
		{
			ins.invoke(myMethod);

			assert myMethod.getCode() != null;

			Execution execution = frame.getExecution();

			if (execution.staticStep)
			{
				Frame staticFrame = StaticStep.stepInto(frame, ins);

				if (staticFrame != null)
				{
					// this invokestatic instruction hasn't been added to this frame yet.. so it
					// is not yet in the return frame
					staticFrame.returnTo.addInstructionContext(ins);
					staticFrame.returnTo.nextInstruction();

					// returnTo has already be duped from frame which is why executing remains
					// true and it is able to resume later
					frame.stop();
				}
			}
			else
			{
				// add possible method call to execution
				execution.invoke(ins, myMethod);
			}

			frame.getExecution().order(frame, myMethod);
		}

		return ins;
	}

	@Override
	public void removeParameter(int idx)
	{
		Class clazz = method.getClazz();

		// create new signature
		Signature sig = new Signature(method.getType());
		sig.remove(idx);

		// create new method pool object
		method = new Method(clazz, method.getName(), sig);
	}

	@Override
	public Method getMethod()
	{
		return method;
	}

	private dev.kyleescobar.runetools.asm.Method lookupMethod()
	{
		ClassGroup group = this.getInstructions().getCode().getMethod().getClassFile().getGroup();

		ClassFile otherClass = group.findClass(method.getClazz().getName());
		if (otherClass == null)
		{
			return null; // not our class
		}
		dev.kyleescobar.runetools.asm.Method other = otherClass.findMethodDeepStatic(method.getName(), method.getType());
		if (other == null)
		{
			return null; // when regenerating the pool after renaming the method this can be null.
		}
		return other;
	}

	@Override
	public void lookup()
	{
		myMethod = lookupMethod();
	}

	@Override
	public void regeneratePool()
	{
		if (myMethod != null)
		{
			if (myMethod != lookupMethod())
			{
				method = myMethod.getPoolMethod();
			}
		}
	}

	@Override
	public void map(ParallelExecutorMapping mapping, InstructionContext ctx, InstructionContext other)
	{
		List<dev.kyleescobar.runetools.asm.Method> myMethods = this.getMethods(),
			otherMethods = ((InvokeStatic) other.getInstruction()).getMethods();

		assert myMethods.size() == otherMethods.size();

		for (int i = 0; i < myMethods.size(); ++i)
		{
			mapping.map(this, myMethods.get(i), otherMethods.get(i));
		}

		for (int i = 0; i < ctx.getPops().size(); ++i)
		{
			StackContext s1 = ctx.getPops().get(i),
				s2 = other.getPops().get(i);

			InstructionContext base1 = MappingExecutorUtil.resolve(s1.getPushed(), s1);
			InstructionContext base2 = MappingExecutorUtil.resolve(s2.getPushed(), s2);

			if (base1.getInstruction() instanceof GetFieldInstruction && base2.getInstruction() instanceof GetFieldInstruction)
			{
				GetFieldInstruction gf1 = (GetFieldInstruction) base1.getInstruction(),
					gf2 = (GetFieldInstruction) base2.getInstruction();

				Field f1 = gf1.getMyField(),
					f2 = gf2.getMyField();

				if (f1 != null && f2 != null)
				{
					mapping.map(this, f1, f2);
				}
			}
		}
	}

	@Override
	public boolean isSame(InstructionContext thisIc, InstructionContext otherIc)
	{
		if (thisIc.getInstruction().getClass() != otherIc.getInstruction().getClass())
		{
			return false;
		}

		InvokeStatic thisIi = (InvokeStatic) thisIc.getInstruction(),
			otherIi = (InvokeStatic) otherIc.getInstruction();

		if (!MappingExecutorUtil.isMaybeEqual(thisIi.method.getType(), otherIi.method.getType()))
		{
			return false;
		}

		List<dev.kyleescobar.runetools.asm.Method> thisMethods = thisIi.getMethods(),
			otherMethods = otherIi.getMethods();

		if (thisMethods.size() != otherMethods.size())
		{
			return false;
		}

		for (int i = 0; i < thisMethods.size(); ++i)
		{
			dev.kyleescobar.runetools.asm.Method m1 = thisMethods.get(i);
			dev.kyleescobar.runetools.asm.Method m2 = otherMethods.get(i);

			/* The class names are random */
			if (!MappingExecutorUtil.isMaybeEqual(m1.getDescriptor(), m2.getDescriptor()))
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean canMap(InstructionContext thisIc)
	{
		return MappingExecutorUtil.isMappable(this);
	}

	@Override
	public void setMethod(Method method)
	{
		this.method = method;
	}
}
