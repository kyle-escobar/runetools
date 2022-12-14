package dev.kyleescobar.runetools.deob.updater.mappingdumper;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

import dev.kyleescobar.runetools.deob.DeobAnnotations;
import dev.kyleescobar.runetools.asm.Field;
import dev.kyleescobar.runetools.asm.pool.Method;
import dev.kyleescobar.runetools.deob.updater.MappingDumper;

public class MappedField
{
	@SerializedName("field")
	public String exportedName;
	public String owner;
	@SerializedName("name")
	public String obfuscatedName;
	public int access;
	public String descriptor;
	public Number decoder;
	// method name, amt of times
	public Map<Method, Integer> puts = new HashMap<>();
	public Map<Method, Integer> gets = new HashMap<>();

	public MappedField visitField(final Field f, final MappingDump dump)
	{
		MappingDumper.putMap(f.getPoolField(), this);

		exportedName = DeobAnnotations.getExportedName(f);

		owner = MappingDumper.getMap(f.getClassFile()).obfuscatedName;

		obfuscatedName = DeobAnnotations.getObfuscatedName(f);
		if (obfuscatedName == null)
		{
			obfuscatedName = f.getName();
		}

		access = f.getAccessFlags();

		descriptor = f.getObfuscatedType().toString();

		decoder = DeobAnnotations.getObfuscatedGetter(f);

		return this;
	}
}
