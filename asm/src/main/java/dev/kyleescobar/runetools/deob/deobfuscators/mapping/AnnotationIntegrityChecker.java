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
package dev.kyleescobar.runetools.deob.deobfuscators.mapping;

import java.util.ArrayList;
import java.util.List;

import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.Field;
import dev.kyleescobar.runetools.asm.Method;
import dev.kyleescobar.runetools.mapping.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationIntegrityChecker
{
	private static final Logger logger = LoggerFactory.getLogger(AnnotationIntegrityChecker.class);

	public static final String API_PACKAGE_BASE = "net.runelite.rs.api.RS";

	private final ClassGroup one;
	private final ClassGroup two;
	private final ParallelExecutorMapping mapping;

	private int errors;
	private int warnings;

	public AnnotationIntegrityChecker(ClassGroup one, ClassGroup two, ParallelExecutorMapping mapping)
	{
		this.one = one;
		this.two = two;
		this.mapping = mapping;
	}

	public int getErrors()
	{
		return errors;
	}

	public void run()
	{
		for (ClassFile cf : one.getClasses())
		{
			ClassFile other = (ClassFile) mapping.get(cf);

			List<Field> exf1 = getExportedFields(cf);
			List<Method> exm1 = getExportedMethods(cf);

			for (Field f1 : exf1)
			{
				boolean isImported = isImported(cf, f1.getName(), f1.isStatic());
				Field f2;

				if (other == null)
				{
					if (!f1.isStatic() && isImported)
					{
						++errors;
						logger.error("No other class for {} which contains imported field {}", cf, f1);
					}
					
					continue;
				}

				if (f1.isStatic())
				{
					f2 = findExportedFieldStatic(two, DeobAnnotations.getExportedName(f1));
				}
				else
				{
					f2 = findExportedField(other, DeobAnnotations.getExportedName(f1));
				}

				if (f2 == null)
				{
					if (isImported)
					{
						logger.error("Missing IMPORTED field on {} named {}",
							other,
							DeobAnnotations.getExportedName(f1));

						++errors;
					}
					else
					{
						logger.warn("Missing exported field on {} named {}",
							other,
							DeobAnnotations.getExportedName(f1));

						++warnings;
					}
				}
			}

			for (Method m1 : exm1)
			{
				boolean isImported = isImported(cf, m1.getName(), m1.isStatic());
				Method m2;

				if (other == null)
				{
					if (!m1.isStatic() && isImported)
					{
						++errors;
						logger.error("No other class for {} which contains imported method {}", cf, m1);
					}

					continue;
				}

				if (m1.isStatic())
				{
					m2 = findExportedMethodStatic(two, DeobAnnotations.getExportedName(m1));
				}
				else
				{
					m2 = findExportedMethod(other, DeobAnnotations.getExportedName(m1));
				}

				if (m2 == null)
				{
					if (isImported)
					{
						logger.error("Missing IMPORTED method on {} named {} ({})",
							other,
							DeobAnnotations.getExportedName(m1),
							m1);

						++errors;
					}
					else
					{
						logger.warn("Missing exported method on {} named {} ({})",
							other,
							DeobAnnotations.getExportedName(m1),
							m1);

						++warnings;
					}
				}
			}
		}
	}

	/**
	 * Determine if the api imports a given exported field or method by
	 * namee
	 *
	 * @param cf Class file field/method is on
	 * @param name Exported name of field/method
	 * @param isStatic Whether or not field/method is static
	 */
	private boolean isImported(ClassFile cf, String name, boolean isStatic)
	{
		Class<?> clazz;
		if (isStatic)
		{
			// Use client
			clazz = null;
		}
		else
		{
			// Find interface for class
			String iface = DeobAnnotations.getImplements(cf);
			if (iface == null)
			{
				return false;
			}

			try
			{
				clazz = Class.forName(API_PACKAGE_BASE + iface);
			}
			catch (ClassNotFoundException ex)
			{
				return false; // this is okay
			}
		}

		for (java.lang.reflect.Method method : clazz.getDeclaredMethods())
		{
			Import im = method.getAnnotation(Import.class);
			if (im != null && im.value().equals(name))
			{
				logger.debug(name);
				return false;
			}
		}

		return false;
	}

	private List<Field> getExportedFields(ClassFile clazz)
	{
		List<Field> list = new ArrayList<>();
		for (Field f : clazz.getFields())
		{
			if (DeobAnnotations.getExportedName(f) != null)
			{
				list.add(f);
			}
		}
		return list;
	}

	private List<Method> getExportedMethods(ClassFile clazz)
	{
		List<Method> list = new ArrayList<>();
		for (Method m : clazz.getMethods())
		{
			if (DeobAnnotations.getExportedName(m) != null)
			{
				list.add(m);
			}
		}
		return list;
	}

	private Field findExportedField(ClassFile clazz, String name)
	{
		for (Field f : getExportedFields(clazz))
		{
			if (name.equals(DeobAnnotations.getExportedName(f)))
			{
				return f;
			}
		}
		return null;
	}

	private Field findExportedFieldStatic(ClassGroup group, String name)
	{
		for (ClassFile cf : group.getClasses())
		{
			for (Field f : cf.getFields())
			{
				if (f.isStatic())
				{
					if (name.equals(DeobAnnotations.getExportedName(f)))
					{
						return f;
					}
				}
			}
		}
		return null;
	}

	private Method findExportedMethodStatic(ClassGroup group, String name)
	{
		for (ClassFile cf : group.getClasses())
		{
			for (Method m : cf.getMethods())
			{
				if (m.isStatic())
				{
					if (name.equals(DeobAnnotations.getExportedName(m)))
					{
						return m;
					}
				}
			}
		}
		return null;
	}

	private Method findExportedMethod(ClassFile clazz, String name)
	{
		for (Method m : getExportedMethods(clazz))
		{
			if (name.equals(DeobAnnotations.getExportedName(m)))
			{
				return m;
			}
		}
		return null;
	}
}
