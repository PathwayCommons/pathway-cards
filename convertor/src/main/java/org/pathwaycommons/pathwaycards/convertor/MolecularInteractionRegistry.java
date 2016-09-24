package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.MolecularInteraction;
import org.biopax.paxtools.model.level3.PhysicalEntity;

import java.util.*;

/**
 * Created by babur on 1/21/2016.
 */
public class MolecularInteractionRegistry
{
	Map<Code, MolecularInteraction> registry;
	BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
	Model model;

	public MolecularInteractionRegistry()
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
	public MolecularInteraction getMolecularInteraction(PhysicalEntity... pes)
	{
		Code code = new Code(pes);

		if (registry.containsKey(code)) return registry.get(code);
		else
		{
			MolecularInteraction mi = factory.create(
				MolecularInteraction.class, "MolecularInteraction/" + NextNumber.get());

			model.add(mi);

			for (PhysicalEntity pe : pes)
			{
				mi.addParticipant(pe);
			}

			registry.put(code, mi);
			return mi;
		}
	}
	class Code
	{
		Set<PhysicalEntity> set;

		public Code(PhysicalEntity... pes)
		{
			set = new HashSet<>(Arrays.asList(pes));
		}

		public int hashCode()
		{
			return set.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Code)
			{
				Code c = (Code) obj;
				return set.equals(c.set);
			}
			else return this == obj;
		}
	}
}
