/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.deob.Deobfuscator;
import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.Field;
import dev.kyleescobar.runetools.asm.Method;
import dev.kyleescobar.runetools.asm.attributes.Annotated;
import dev.kyleescobar.runetools.asm.execution.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sort fields and methods based first on the name order of the classes, and
 * then based on the order the executor encounters them.
 *
 * @author Adam
 */
public class Order implements Deobfuscator
{
	private static final Logger logger = LoggerFactory.getLogger(Order.class);

	private Execution execution;

	private final Map<String, Integer> nameIndices = new HashMap<>();

	@Override
	public void run(ClassGroup group)
	{
		execution = new Execution(group);
		execution.staticStep = true;
		execution.populateInitialMethods();
		execution.run();

		for (int i = 0; i < group.getClasses().size(); i++)
		{
			ClassFile cf = group.getClasses().get(i);
			String className = DeobAnnotations.getObfuscatedName(cf);
			nameIndices.put(className, i);
		}

		int sortedMethods = 0, sortedFields = 0;

		for (ClassFile cf : group.getClasses())
		{
			List<Method> m = cf.getMethods();
			m.sort(this::compare);

			sortedMethods += m.size();

			// field order of enums is mostly handled in EnumDeobfuscator
			if (!cf.isEnum())
			{
				List<Field> f = cf.getFields();
				f.sort(this::compare);

				sortedFields += f.size();
			}
		}

		logger.info("Sorted {} methods and {} fields", sortedMethods, sortedFields);
	}

	// static fields, member fields, clinit, init, methods, static methods
	private int compare(Annotated a, Annotated b)
	{
		int i1 = getType(a), i2 = getType(b);

		if (i1 != i2)
		{
			return Integer.compare(i1, i2);
		}

		int nameIdx1 = getNameIdx(a);
		int nameIdx2 = getNameIdx(b);

		if (nameIdx1 != nameIdx2)
		{
			return Integer.compare(nameIdx1, nameIdx2);
		}

		return compareOrder(a, b);
	}

	private int getNameIdx(Annotated annotations)
	{
		String name = DeobAnnotations.getObfuscatedName(annotations);

		Integer nameIdx = nameIndices.get(name);

		return nameIdx != null ? nameIdx : -1;
	}

	private int getType(Annotated a)
	{
		if (a instanceof Method)
			return getType((Method) a);
		else if (a instanceof Field)
			return getType((Field) a);
		throw new RuntimeException("kys");
	}

	private int getType(Method m)
	{
		if (m.getName().equals("<clinit>"))
		{
			return 1;
		}
		if (m.getName().equals("<init>"))
		{
			return 2;
		}
		if (!m.isStatic())
		{
			return 3;
		}
		return 4;
	}

	private int getType(Field f)
	{
		if (f.isStatic())
		{
			return 1;
		}
		return 2;
	}

	private int compareOrder(Object o1, Object o2)
	{
		Integer i1, i2;

		i1 = execution.getOrder(o1);
		i2 = execution.getOrder(o2);

		if (i1 == null)
		{
			i1 = Integer.MAX_VALUE;
		}
		if (i2 == null)
		{
			i2 = Integer.MAX_VALUE;
		}

		if (!i1.equals(i2))
		{
			return Integer.compare(i1, i2);
		}

		// Fall back to number of accesses
		i1 = execution.getAccesses(o1);
		i2 = execution.getAccesses(o2);

		if (i1 == null)
		{
			i1 = Integer.MAX_VALUE;
		}
		if (i2 == null)
		{
			i2 = Integer.MAX_VALUE;
		}

		return Integer.compare(i1, i2);
	}

}
