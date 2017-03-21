package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by BaburO on 1/18/2016.
 */
public class ComplexRepository
{
	BioPAXFactory factory;
	Model model;

	Map<Code, Complex> repo;

	public ComplexRepository()
	{
		factory = BioPAXLevel.L3.getDefaultFactory();
		repo = new HashMap<>();
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	public Complex getComplex(Set<PhysicalEntity> members)
	{
		return getComplex(members, null);
	}

	public Complex getComplex(Set<PhysicalEntity> members, String name)
	{
		Code c = new Code(members);
		if (repo.containsKey(c)) return repo.get(c);

		if (name == null)
		{
			name = "";
			for (PhysicalEntity member : members)
			{
				name += member.getDisplayName() + " : ";
			}
			if (!name.isEmpty()) name = name.substring(0, name.length() - 3);
		}

		Complex complex = factory.create(Complex.class, "Complex/" + NextNumber.get());
		complex.setDisplayName(name);
		model.add(complex);
		members.forEach(complex::addComponent);
		repo.put(c, complex);
		return complex;
	}

	private class Code
	{
		Set<PhysicalEntity> members;

		public Code(Set<PhysicalEntity> members)
		{
			this.members = members;
		}

		@Override
		public int hashCode()
		{
			int h = 0;
			for (PhysicalEntity member : members)
			{
				h += member.hashCode();
			}
			return h;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Code)
			{
				Code c = (Code) obj;

				return members.equals(c.members);
			}

			return false;
		}
	}
}
