package org.pathwaycommons.pathwaycards.extractor;

import org.biopax.paxtools.pattern.Match;
import org.biopax.paxtools.pattern.constraint.ConstraintAdapter;

/**
 *
 */
public class HasSmallerID extends ConstraintAdapter
{
	protected HasSmallerID()
	{
		super(2);
	}

	@Override
	public boolean satisfies(Match match, int... ind)
	{
		return match.get(ind[0]).getUri().compareTo(
			match.get(ind[1]).getUri()) < 0;
	}
}
