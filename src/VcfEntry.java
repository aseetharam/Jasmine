/*
 * Methods for storing VCF v4.2 entries for structural variants
 * It allows easy access to main variant fields, plus some methods
 * to do more parsing and error-checking for things like type and length.
 */

import java.util.Arrays;

public class VcfEntry {

	String originalLine;
	String[] tabTokens;
	
	public VcfEntry(String line) throws Exception
	{
		originalLine = line;
		tabTokens = line.split("\t");
		if(tabTokens.length < 8)
		{
			throw new Exception("VCF line had too few entries: "
					+ Arrays.toString(tabTokens));
		}
	}
	
	/*
	 * Reconstruct the VCF line by concatenating and tab-separating the fields
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder("");
		for(int i = 0; i<tabTokens.length; i++)
		{
			sb.append(tabTokens[i]);
			if(i < tabTokens.length - 1)
			{
				sb.append("\t");
			}
		}
		return sb.toString();
	}
	
	public String getChromosome()
	{
		return tabTokens[0];
	}
	
	public void setChromosome(String s)
	{
		tabTokens[0] = s;
	}
	
	public long getPos() throws Exception
	{
		try {
			return Long.parseLong(tabTokens[1]);
		} catch(Exception e) {
			throw new Exception("Tried to access invalid VCF position: " + tabTokens[1]);
		}
	}
	
	public void setPos(long val)
	{
		tabTokens[1] = val+"";
	}
	
	public String getId()
	{
		return tabTokens[2];
	}
	
	public void setId(String s)
	{
		tabTokens[2] = s;
	}
	
	public String getRef()
	{
		return tabTokens[3];
	}
	
	public void setRef(String s)
	{
		tabTokens[3] = s;
	}
	
	public String getAlt()
	{
		return tabTokens[4];
	}
	
	public void setAlt(String s)
	{
		tabTokens[4] = s;
	}
	
	public int getLength() throws Exception
	{
		try {
			String s = getInfo("SVLEN");
			return (int)(.5 + Double.parseDouble(s));
		} catch(Exception e) {
            String seq = getSeq();
            String type = getType();
            if(type.equals("INS")) return seq.length();
            else return -seq.length();
		}
	}
	
	public String getType() throws Exception
	{
		String res = getInfo("SVTYPE");
		if(res.length() == 0)
		{
			int refLength = getRef().length();
			int altLength = getAlt().length();
			if(getAlt().startsWith("<") && getAlt().endsWith(">"))
			{
				return getAlt().substring(1, getAlt().length() - 1);
			}
			if(refLength > altLength)
			{
				return "INS";
			}
			else if(refLength < altLength)
			{
				return "DEL";
			}
			else
			{
				return "";
			}
		}
		else return res;
	}
	
	public String getStrand() throws Exception
	{
		return getInfo("STRANDS");
	}
	
	public String getGraphID() throws Exception
	{
		String id = getChromosome();
		if(Settings.USE_TYPE)
		{
			id += "_" + getType();
		}
		if(Settings.USE_STRAND)
		{
			id += "_" + getStrand();
		}
		return id;
	}
	
	public String getSeq() throws Exception
	{
		if(hasInfoField("SEQ"))
		{
			return getInfo("SEQ");
		}
		String ref = getRef(), alt = getAlt();
		if(alt.startsWith("<"))
		{
			return "";
		}
		String type = getType();
		
		// If the SV is a deletion, swap REF and ALT and treat as an insertion
		if(type.equals("DEL"))
		{
			String tmp = ref;
			ref = alt;
			alt = tmp;
			type = "INS";
		}
		if(ref.equals("X") || ref.equals("N"))
		{
			return alt;
		}
		else if(alt.equals("X") || ref.equals("N"))
		{
			return ref;
		}
		else if(type.equals("INS"))
		{
			int startPad = 0, endPad = 0;
			int totalPad = ref.length();
			while(startPad + endPad < totalPad && startPad < alt.length())
			{
				if(ref.charAt(startPad) == alt.charAt(startPad))
				{
					startPad++;
				}
				else if(ref.charAt(ref.length() - 1 - endPad) == alt.charAt(alt.length() - 1 - endPad))
				{
					endPad++;
				}
				else
				{
					break;
				}
			}
			if(startPad + endPad == totalPad)
			{
				return alt.substring(startPad, alt.length() - endPad);
			}
			else
			{
				return alt;
			}
		}
		else
		{
			return alt;
		}
	}
	
	/*
	 * Set a particular VCF INFO field, adding the field if it doesn't already exist
	 */
	public void setInfo(String field, String val) throws Exception
	{
		String[] infoFields = tabTokens[7].split(";");
		for(String semitoken : infoFields)
		{
			int equalIndex = semitoken.indexOf('=');
			if(equalIndex == -1)
			{
				continue;
			}
			String key = semitoken.substring(0, equalIndex);
			if(key.equals(field))
			{
				String updatedToken = key + "=" + val;
				
				// Special case if this is the first INFO field
				if(tabTokens[7].startsWith(semitoken))
				{
					tabTokens[7] = tabTokens[7].replaceFirst(semitoken, updatedToken);
				}
				else
				{
					tabTokens[7] = tabTokens[7].replaceAll(";" + semitoken, ";" + updatedToken);
				}
				return;
			}
		}
		
		// Field not found, so add it!
		tabTokens[7] += ";" + field + "=" + val;
	}
	
	/*
	 * Get the value of a particular INFO field
	 */
	public String getInfo(String field) throws Exception
	{
		String infoToken = tabTokens[7];
		String[] semicolonSplit = infoToken.split(";");
		for(String semitoken : semicolonSplit)
		{
			int equalIndex = semitoken.indexOf('=');
			if(equalIndex == -1)
			{
				continue;
			}
			String key = semitoken.substring(0, equalIndex);
			if(key.equals(field))
			{
				return semitoken.substring(1 + equalIndex);
			}
		}
		return "";
	}
	
	public boolean hasInfoField(String fieldName)
	{
		String infoToken = tabTokens[7];
		String[] semicolonSplit = infoToken.split(";");
		for(String semitoken : semicolonSplit)
		{
			int equalIndex = semitoken.indexOf('=');
			if(equalIndex == -1)
			{
				continue;
			}
			String key = semitoken.substring(0, equalIndex);
			if(key.equals(fieldName))
			{
				return true;
			}
		}
		return false;
	}

}
