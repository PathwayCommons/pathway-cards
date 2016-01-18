package org.pathwaycommons.pathwaycards.convertor;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by BaburO on 1/18/2016.
 */
public class HGNC
{
	static private Map<String, String> upToSymbol;

	static String getSymbol(String uniprot)
	{
		return upToSymbol.get(uniprot);
	}

	static
	{
		upToSymbol = new HashMap<>();
		Scanner sc = new Scanner(HGNC.class.getResourceAsStream("hgnc.txt"));
		sc.nextLine();
		while (sc.hasNextLine())
		{
			String[] token = sc.nextLine().split("\t");

			if (token.length > 2 && !token[2].isEmpty())
			{
				upToSymbol.put(token[2], token[1]);
			}
		}
	}
}
