package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.PhysicalEntity;

import java.util.*;

/**
 * Created by babur on 1/21/2016.
 */
public class ConversionRegistry
{
	Map<Code, Conversion> registry;
	BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
	Model model;

	public ConversionRegistry()
	{
		registry = new HashMap<>();
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	/**
	 * Checks if it is ok to generate a conversion for this interaction.
	 * @return true if can generate conversion, false if a similar one already registered before
	 */
	public Conversion getConversion(PhysicalEntity in, PhysicalEntity out, Class<? extends Conversion> clazz)
	{
		Code code = new Code(in, out);

		if (registry.containsKey(code)) return registry.get(code);
		else
		{
			Conversion cnv = factory.create(clazz, "Conversion/" + NextNumber.get());
			model.add(cnv);
			cnv.addLeft(in);
			cnv.addRight(out);
			cnv.setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);

			registry.put(code, cnv);
			return cnv;
		}
	}
	class Code
	{
		PhysicalEntity in;
		PhysicalEntity out;

		public Code(PhysicalEntity in, PhysicalEntity out)
		{
			this.in = in;
			this.out = out;
		}

		public int hashCode()
		{
			return in.hashCode() + out.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Code)
			{
				Code c = (Code) obj;
				return in == c.in && out == c.out;
			}
			else return this == obj;
		}
	}
}
