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

package dev.kyleescobar.runetools.deob.deobfuscators;

import java.util.ArrayList;
import java.util.List;

import dev.kyleescobar.runetools.deob.Deobfuscator;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.attributes.code.Instruction;
import dev.kyleescobar.runetools.asm.attributes.code.Instructions;
import dev.kyleescobar.runetools.asm.attributes.code.instructions.AConstNull;
import dev.kyleescobar.runetools.asm.attributes.code.instructions.CheckCast;
import dev.kyleescobar.runetools.asm.execution.Execution;
import dev.kyleescobar.runetools.asm.execution.InstructionContext;
import dev.kyleescobar.runetools.asm.execution.MethodContext;
import dev.kyleescobar.runetools.asm.execution.StackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CastNull implements Deobfuscator
{
	private static final Logger logger = LoggerFactory.getLogger(CastNull.class);
	
	private int removed;
	
	private final List<Instruction> interesting = new ArrayList<>();
	private final List<Instruction> notInteresting = new ArrayList<>();
	
	private void visit(InstructionContext ictx)
	{
		if (!(ictx.getInstruction() instanceof CheckCast))
			return;
		
		if (notInteresting.contains(ictx.getInstruction()) || interesting.contains(ictx.getInstruction()))
			return;
		
		StackContext sctx = ictx.getPops().get(0);
		if (sctx.getPushed().getInstruction() instanceof AConstNull)
		{
			interesting.add(ictx.getInstruction());
		}
		else
		{
			interesting.remove(ictx.getInstruction());
			notInteresting.add(ictx.getInstruction());
		}
	}
	
	private void visit(MethodContext ctx)
	{
		Instructions ins = ctx.getMethod().getCode().getInstructions();
		interesting.forEach(ins::remove);
		removed += interesting.size();
		interesting.clear();
		notInteresting.clear();
	}
	
	@Override
	public void run(ClassGroup group)
	{
		Execution execution = new Execution(group);
		execution.addExecutionVisitor(i -> visit(i));
		execution.addMethodContextVisitor(i -> visit(i));
		execution.populateInitialMethods();
		execution.run();
		
		logger.info("Removed {} casts on null", removed);
	}

}
