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

package dev.kyleescobar.runetools.deob.updater;

import dev.kyleescobar.runetools.asm.Annotation;
import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.Field;
import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.deob.deobfuscators.Renamer;
import dev.kyleescobar.runetools.asm.Method;
import dev.kyleescobar.runetools.asm.attributes.Annotated;
import dev.kyleescobar.runetools.deob.util.NameMappings;

public class AnnotationRenamer
{
	private ClassGroup group;

	public AnnotationRenamer(ClassGroup group)
	{
		this.group = group;
	}

	public void run()
	{
		NameMappings mappings = buildMappings();

		Renamer renamer = new Renamer(mappings);
		renamer.run(group);
	}

	private NameMappings buildMappings()
	{
		NameMappings mappings = new NameMappings();

		for (ClassFile cf : group.getClasses())
		{
			String name = getImplements(cf);
			if (name != null)
				mappings.map(cf.getPoolClass(), name);

			for (Field f : cf.getFields())
			{
				name = DeobAnnotations.getExportedName(f);
				if (name != null)
					mappings.map(f.getPoolField(), name);
			}

			for (Method m : cf.getMethods())
			{
				name = DeobAnnotations.getExportedName(m);
				if (name != null)
					mappings.map(m.getPoolMethod(), name);
			}
		}

		return mappings;
	}

	private String getImplements(Annotated cf)
	{
		Annotation an = cf.findAnnotation(DeobAnnotations.IMPLEMENTS);
		return an == null ? null : an.getValueString();
	}
}
