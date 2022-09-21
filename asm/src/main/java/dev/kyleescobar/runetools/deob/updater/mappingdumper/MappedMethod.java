package dev.kyleescobar.runetools.deob.updater.mappingdumper;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.kyleescobar.runetools.asm.pool.Method;
import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.asm.attributes.Code;
import dev.kyleescobar.runetools.asm.attributes.code.Instruction;
import dev.kyleescobar.runetools.asm.attributes.code.Instructions;
import dev.kyleescobar.runetools.asm.attributes.code.Parameter;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.GetFieldInstruction;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.InvokeInstruction;
import dev.kyleescobar.runetools.asm.attributes.code.instruction.types.SetFieldInstruction;
import dev.kyleescobar.runetools.asm.pool.Class;
import dev.kyleescobar.runetools.asm.pool.Field;
import dev.kyleescobar.runetools.deob.updater.MappingDumper;

public class MappedMethod
{
	@SerializedName("method")
	public String exportedName;
	public String owner;
	@SerializedName("name")
	public String obfuscatedName;
	public int access;
	public List<String> parameters;
	public String descriptor;
	public String garbageValue;
	public List<Integer> lineNumbers;
	public Map<Field, Integer> fieldGets = new HashMap<>();
	public Map<Field, Integer> fieldPuts = new HashMap<>();
	public Map<Method, Integer> dependencies = new HashMap<>();

	public MappedMethod visitMethod(final dev.kyleescobar.runetools.asm.Method m, final MappingDump dump)
	{
		MappingDumper.putMap(m.getPoolMethod(), this);
		exportedName = DeobAnnotations.getExportedName(m);

		owner = MappingDumper.getMap(m.getClassFile()).obfuscatedName;

		obfuscatedName = DeobAnnotations.getObfuscatedName(m);
		if (obfuscatedName == null)
		{
			obfuscatedName = m.getName();
		}

		access = m.getAccessFlags();

		parameters = m.getParameters()
			.stream()
			.map(Parameter::getName)
			.collect(Collectors.toList());

		descriptor = m.getObfuscatedSignature().toString();

		garbageValue = DeobAnnotations.getDecoder(m);

		Code c = m.getCode();
		if (c != null)
		{
			visitCode(c);
		}

		return this;
	}

	private void visitCode(Code c)
	{
		lineNumbers = c.getLineNumbers();

		Instructions ins = c.getInstructions();
		for (Instruction i : ins.getInstructions())
		{
			if (i instanceof GetFieldInstruction)
			{
				Field k = ((GetFieldInstruction) i).getField();
				int v = fieldGets.getOrDefault(k, 0) + 1;
				fieldGets.put(k, v);
			}
			else if (i instanceof SetFieldInstruction)
			{
				Field k = ((SetFieldInstruction) i).getField();
				int v = fieldPuts.getOrDefault(k, 0) + 1;
				fieldPuts.put(k, v);
			}
			else if (i instanceof InvokeInstruction)
			{
				List<dev.kyleescobar.runetools.asm.Method> met = ((InvokeInstruction) i).getMethods();
				Method k;
				if (met.size() > 0)
				{
					dev.kyleescobar.runetools.asm.Method mme = met.get(0);
					k = new Method(
						new Class(Objects.requireNonNull(DeobAnnotations.getObfuscatedName(mme.getClassFile()))),
						DeobAnnotations.getObfuscatedName(mme),
						mme.getObfuscatedSignature() != null ? mme.getObfuscatedSignature() : mme.getDescriptor()
					);
				}
				else
				{
					k = ((InvokeInstruction) i).getMethod();
				}

				int v = dependencies.getOrDefault(k, 0) + 1;
				dependencies.put(k, v);
			}
		}
	}
}
