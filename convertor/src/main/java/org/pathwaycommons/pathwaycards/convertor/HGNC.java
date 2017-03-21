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
	static private Map<String, String> symbolToUP;
	static private Map<String, String> idToUP;
	static private Map<String, String> idToSymbol;

	static String getSymbolOfUP(String uniprot)
	{
		return upToSymbol.get(uniprot);
	}

	static String getUniProtOfSymbol(String symbol)
	{
		return symbolToUP.get(symbol);
	}

	static String getUniProtOfID(String id)
	{
		return idToUP.get(id);
	}

	static String getSymbolOfID(String id)
	{
		return idToSymbol.get(id);
	}

	static
	{
		upToSymbol = new HashMap<>();
		symbolToUP = new HashMap<>();
		idToUP = new HashMap<>();
		idToSymbol = new HashMap<>();
		Scanner sc = new Scanner(HGNC.class.getResourceAsStream("hgnc.txt"));
		sc.nextLine();
		while (sc.hasNextLine())
		{
			String[] token = sc.nextLine().split("\t");

			if (token.length > 2 && !token[2].isEmpty())
			{
				upToSymbol.put(token[2], token[1]);

				if (symbolToUP.containsKey(token[1]))
				{
					System.out.println();
					System.out.println("token[1] = " + token[1]);
					System.out.println("symbolToUP = " + symbolToUP.get(token[1]));
					System.out.println("token[2] = " + token[2]);
				}

				symbolToUP.put(token[1], token[2]);
				idToUP.put(token[0], token[2]);
				idToSymbol.put(token[0], token[1]);
			}
		}
	}

	public static void main(String[] args)
	{
		getUniProtOfSymbol("AR");
	}
}
