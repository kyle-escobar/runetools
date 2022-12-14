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

import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.asm.ClassGroup;
import dev.kyleescobar.runetools.asm.Field;
import dev.kyleescobar.runetools.asm.Method;
import dev.kyleescobar.runetools.asm.Annotation;
import dev.kyleescobar.runetools.asm.attributes.Annotated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationMapper
{
	private static final Logger logger = LoggerFactory.getLogger(AnnotationMapper.class);

	private final ClassGroup source, target;
	private final ParallelExecutorMapping mapping;

	public AnnotationMapper(ClassGroup source, ClassGroup target, ParallelExecutorMapping mapping)
	{
		this.source = source;
		this.target = target;
		this.mapping = mapping;
	}

	public void run()
	{
		int count = 0;

		for (ClassFile c : source.getClasses())
		{
			ClassFile other = (ClassFile) mapping.get(c);

			count += run(c, other);
		}

		logger.info("Copied {} annotations", count);
	}

	private int run(ClassFile from, ClassFile to)
	{
		int count = 0;

		if (hasCopyableAnnotation(from))
		{
			if (to != null)
			{
				count += copyAnnotations(from, to);
			}
			else
			{
				logger.warn("Class {} has copyable annotations but there is no mapped class", from);
			}
		}

		for (Field f : from.getFields())
		{
			if (!hasCopyableAnnotation(f))
				continue;

			Field other = (Field) mapping.get(f);
			if (other == null)
			{
				logger.warn("Unable to map annotated field {} named {}", f, DeobAnnotations.getExportedName(f));
				continue;
			}

			count += copyAnnotations(f, other);
		}

		for (Method m : from.getMethods())
		{
			if (!hasCopyableAnnotation(m))
				continue;

			Method other = (Method) mapping.get(m);
			if (other == null)
			{
				logger.warn("Unable to map annotated method {} named {}", m, DeobAnnotations.getExportedName(m));
				continue;
			}

			count += copyAnnotations(m, other);
		}

		return count;
	}

	private int copyAnnotations(Annotated from, Annotated to)
	{
		int count = 0;

		if (from.getAnnotations() == null)
			return count;

		for (Annotation a : from.getAnnotations().values())
		{
			if (isCopyable(a))
			{
				to.addAnnotation(a);
				++count;
			}
		}

		return count;
	}

	private boolean hasCopyableAnnotation(Annotated a)
	{
		return a.findAnnotation(DeobAnnotations.EXPORT) != null || a.findAnnotation(DeobAnnotations.IMPLEMENTS) != null;
	}

	private boolean isCopyable(Annotation a)
	{
		return a.getType().equals(DeobAnnotations.EXPORT)
			|| a.getType().equals(DeobAnnotations.IMPLEMENTS);
	}
}
