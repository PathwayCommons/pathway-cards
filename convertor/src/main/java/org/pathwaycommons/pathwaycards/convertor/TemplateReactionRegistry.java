package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.TemplateReaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by babur on 1/21/2016.
 */
public class TemplateReactionRegistry
{
	Map<PhysicalEntity, TemplateReaction> registry;
	BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
	Model model;

	public TemplateReactionRegistry()
	{
		registry = new HashMap<>();
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	/**
	 * Gets or creates the TemplateReaction for producing the given physical entity.
	 */
	public TemplateReaction getTempReac(PhysicalEntity out)
	{
		if (registry.containsKey(out)) return registry.get(out);
		else
		{
			TemplateReaction tr = factory.create(TemplateReaction.class, "TemplateReaction/" + NextNumber.get());
			model.add(tr);
			tr.addProduct(out);

			registry.put(out, tr);
			return tr;
		}
	}
}
