package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXElement;
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
	public Control getControl(Set<PhysicalEntity> controllers, Interaction inter, Boolean positive)
	{
		Code code = new Code(controllers, inter, positive);

		if (registry.containsKey(code)) return registry.get(code);
		else
		{
			Control ctr;

			if (inter instanceof BiochemicalReaction && positive)
			{
				ctr = factory.create(Catalysis.class, "Catalysis/" + NextNumber.get());
			}
			else if (inter instanceof TemplateReaction)
			{
				ctr = factory.create(TemplateReactionRegulation.class, "TemplateReactionRegulation/" + NextNumber.get());
			}
			else
			{
				ctr = factory.create(Control.class, "Control/" + NextNumber.get());
			}

			model.add(ctr);
			for (PhysicalEntity c : controllers)
			{
				ctr.addController(c);
			}
			ctr.addControlled(inter);
			ctr.setControlType(positive ? ControlType.ACTIVATION : ControlType.INHIBITION);

			registry.put(code, ctr);
			return ctr;
		}
	}
	class Code
	{
		Set<PhysicalEntity> controllers;
		Interaction inter;
		Boolean positive;

		public Code(Set<PhysicalEntity> controllers, Interaction inter, Boolean positive)
		{
			this.controllers = controllers;
			this.inter = inter;
			this.positive = positive;
		}

		public int hashCode()
		{
			int h = 0;
			for (PhysicalEntity c : controllers)
			{
				h += c.hashCode();
			}
			h += inter.hashCode();
			h += positive.hashCode();
			return h;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Code)
			{
				Code c = (Code) obj;
				return controllers.size() == c.controllers.size() && controllers.containsAll(c.controllers) &&
					inter == c.inter && positive.equals(c.positive);
			}
			else return this == obj;
		}
	}
}
