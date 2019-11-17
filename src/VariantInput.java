/*
 * Methods for reading VCF entries from VCF files and dividing
 * the entries into separate groups by graph ID
 */

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;

public class VariantInput {
	
	/*
	 * Count the number of VCF files in a list
	 */
	public static int countFiles(String fileList) throws Exception
	{
		Scanner input = new Scanner(new FileInputStream(new File(fileList)));
		int count = 0;
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.length() == 0)
			{
				continue;
			}
			count++;
		}
		input.close();
		return count;
	}
	
	/*
	 * Get a list of all variants from a group of files, binning them by graphID
	 */
	@SuppressWarnings("unchecked")
	public static TreeMap<String, ArrayList<Variant>> readAllFiles(String fileList) throws Exception
	{
		Scanner input = new Scanner(new FileInputStream(new File(fileList)));
		ArrayList<String> fileNames = new ArrayList<String>();
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.length() == 0)
			{
				continue;
			}
			fileNames.add(line);
		}
		TreeMap<String, ArrayList<Variant>>[] variantsPerFile = new TreeMap[fileNames.size()];
		for(int i = 0; i<fileNames.size(); i++)
		{
			variantsPerFile[i] = getSingleList(fileNames.get(i), i);
		}
		TreeMap<String, ArrayList<Variant>> res = new TreeMap<String, ArrayList<Variant>>();
		for(int i = 0; i<fileNames.size(); i++)
		{
			for(String s : variantsPerFile[i].keySet())
			{
				if(!res.containsKey(s))
				{
					res.put(s, new ArrayList<Variant>());
				}
				for(Variant v : variantsPerFile[i].get(s))
				{
					res.get(s).add(v);
				}
			}
		}
		input.close();
		return res;
	}
	
	/*
	 * Get a list of variants binned by graphID for a single VCF file
	 */
	private static TreeMap<String, ArrayList<Variant>> getSingleList(String filename, int sample) throws Exception
	{
		Scanner input = new Scanner(new FileInputStream(new File(filename)));
		ArrayList<Variant> allVariants = new ArrayList<Variant>();
		while(input.hasNext())
		{
			String line = input.nextLine();
			if(line.length() == 0 || line.startsWith("#"))
			{
				continue;
			}
			allVariants.add(fromVcfEntry(line, sample));
		}
		
		System.out.println(filename + " has " + allVariants.size() + " variants");
		input.close();
		
		return divideIntoGraphs(allVariants);
	}
	
	/*
	 * Take a list of variants and bin them by graphID
	 */
	private  static TreeMap<String, ArrayList<Variant>> divideIntoGraphs(ArrayList<Variant> data)
	{
		TreeMap<String, ArrayList<Variant>> groups = new TreeMap<String, ArrayList<Variant>>();
		for(Variant v : data)
		{
			String graphID = v.graphID;
			if(!groups.containsKey(graphID))
			{
				groups.put(graphID, new ArrayList<Variant>());
			}
			groups.get(graphID).add(v);
		}
		return groups;
	}
	
	/*
	 * From a line of a VCF file, extract the information needed for merging
	 * and return it as a Variant object
	 */
	private static Variant fromVcfEntry(String line, int sample) throws Exception
	{
		VcfEntry entry = new VcfEntry(line);
		
		int start = (int)entry.getPos();
		int end = start + entry.getLength();
		
		String id = entry.getGraphID();
		
		return new Variant(sample, entry.getId(), start, end, id);
	}
}