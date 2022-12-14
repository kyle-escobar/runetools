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

import java.util.List;

import dev.kyleescobar.runetools.deob.Deob;
import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.deob.Deobfuscator;
import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.Field;
import dev.kyleescobar.runetools.asm.Method;
import dev.kyleescobar.runetools.asm.signature.util.VirtualMethods;
import dev.kyleescobar.runetools.deob.util.NameMappings;

public class RenameUnique implements Deobfuscator
{
	private void generateClassNames(NameMappings map, ClassGroup group)
	{
		int i = 0;
		for (ClassFile cf : group.getClasses())
			if (cf.getName().length() <= Deob.OBFUSCATED_NAME_MAX_LEN)
				map.map(cf.getPoolClass(), "class" + i++);
		map.setClasses(i);
	}

	private void generateFieldNames(NameMappings map, ClassGroup group)
	{
		int i = 0;
		for (ClassFile cf : group.getClasses())
			for (Field field : cf.getFields())
				if (Deob.isObfuscated(field.getName()) && !field.getName().equals(DeobAnnotations.getExportedName(field)))
					map.map(field.getPoolField(), "field" + i++);
		map.setFields(i);
	}

	private void generateMethodNames(NameMappings map, ClassGroup group)
	{
		int i = 0;
		for (ClassFile cf : group.getClasses())
			for (Method method : cf.getMethods())
			{
				if (!Deob.isObfuscated(method.getName()) || method.getName().equals(DeobAnnotations.getExportedName(method)))
					continue;

				List<Method> virtualMethods = VirtualMethods.getVirtualMethods(method);
				assert !virtualMethods.isEmpty();

				String name;
				if (virtualMethods.size() == 1)
					name = "method" + i++;
				else
					name = "vmethod" + i++;

				for (Method m : virtualMethods)
					map.map(m.getPoolMethod(), name);
			}
		map.setMethods(i);
	}

	@Override
	public void run(ClassGroup group)
	{
		group.buildClassGraph();
		group.lookup();

		NameMappings mappings = new NameMappings();

		this.generateClassNames(mappings, group);
		this.generateFieldNames(mappings, group);
		this.generateMethodNames(mappings, group);

		Renamer renamer = new Renamer(mappings);
		renamer.run(group);
	}
}
