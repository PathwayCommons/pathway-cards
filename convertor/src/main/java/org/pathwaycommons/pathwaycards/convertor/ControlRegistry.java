package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by babur on 1/21/2016.
 */
public class ControlRegistry
{
	Map<Code, Control> registry;
	BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
	Model model;

	public ControlRegistry()
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
	public Control getControl(Set<PhysicalEntity> controllers, Conversion cnv)
	{
		Code code = new Code(controllers, cnv);

		if (registry.containsKey(code)) return registry.get(code);
		else
		{
			Control ctr = factory.create(cnv instanceof BiochemicalReaction ? Catalysis.class : Control.class,
				"Conversion/" + NextNumber.get());
			model.add(ctr);
			for (PhysicalEntity c : controllers)
			{
				ctr.addController(c);
			}
			ctr.addControlled(cnv);
			ctr.setControlType(ControlType.ACTIVATION);

			registry.put(code, ctr);
			return ctr;
		}
	}
	class Code
	{
		Set<PhysicalEntity> controllers;
		Conversion cnv;

		public Code(Set<PhysicalEntity> controllers, Conversion cnv)
		{
			this.controllers = controllers;
			this.cnv = cnv;
		}

		public int hashCode()
		{
			int h = 0;
			for (PhysicalEntity c : controllers)
			{
				h += c.hashCode();
			}
			h += cnv.hashCode();
			return h;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Code)
			{
				Code c = (Code) obj;
				return controllers.size() == c.controllers.size() && controllers.containsAll(c.controllers) &&
					cnv == c.cnv;
			}
			else return this == obj;
		}
	}
}
