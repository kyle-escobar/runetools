package dev.kyleescobar.runetools.deob.updater.mappingdumper;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.stream.Collectors;

import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.asm.ClassFile;
import dev.kyleescobar.runetools.deob.updater.MappingDumper;

public class MappedClass
{
	@SerializedName("class")
	public String implementingName;
	@SerializedName("name")
	public String obfuscatedName;
	@SerializedName("super")
	public String superClass;
	public int access;
	public List<String> interfaces;
	public List<MappedField> fields;
	public List<MappedMethod> methods;
	public List<MappedMethod> constructors;

	public MappedClass visitClass(final ClassFile c, final MappingDump dump)
	{
		MappingDumper.putMap(c, this);

		implementingName = DeobAnnotations.getImplements(c);

		obfuscatedName = DeobAnnotations.getObfuscatedName(c);
		if (obfuscatedName == null)
		{
			obfuscatedName = c.getName();
		}

		ClassFile parent = c.getParent();
		if (parent != null)
		{
			superClass = DeobAnnotations.getObfuscatedName(parent);
		}

		access = c.getAccess();

		interfaces = c.getInterfaces()
			.getMyInterfaces()
			.stream()
			.map(DeobAnnotations::getObfuscatedName)
			.collect(Collectors.toList());

		fields = c.getFields()
			.stream()
			.map(f -> new MappedField().visitField(f, dump))
			.collect(Collectors.toList());

		methods = c.getMethods()
			.stream()
			.map(m -> new MappedMethod().visitMethod(m, dump))
			.collect(Collectors.toList());

		constructors = methods
			.stream()
			.filter(m -> m.obfuscatedName.endsWith("init>"))
			.collect(Collectors.toList());

		return this;
	}
}
