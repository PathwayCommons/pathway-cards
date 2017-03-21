package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.PhysicalEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is for registering the interactions and the outputs of the created events, to use them later.
 * @author Ozgun Babur
 */
public class EventRegistry
{
	Map<String, Interaction> eventToInter;
	Map<String, PhysicalEntity> eventToProduct;

	public EventRegistry()
	{
		eventToInter = new HashMap<>();
		eventToProduct = new HashMap<>();
	}

	public void register(String predicateID, Interaction inter, PhysicalEntity product)
	{
		eventToInter.put(predicateID, inter);
		eventToProduct.put(predicateID, product);
	}

	public boolean isRegistered(String predicateID)
	{
		return eventToInter.containsKey(predicateID);
	}

	public Interaction getTheInteraction(String predicateID)
	{
		return eventToInter.get(predicateID);
	}

	public PhysicalEntity getTheProduct(String predicateID)
	{
		return eventToProduct.get(predicateID);
	}
}
