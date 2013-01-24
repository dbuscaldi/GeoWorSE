package es.upv.dsic.geoWordNet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.geography.InvalidCoordinateRangeException;
import es.upv.dsic.geoclef.geography.WorldPoint;
import es.upv.dsic.tools.StringTools;

public class GeoWordNetDictionary {
	private HashMap dict;
	private HashSet allowedWNCodes; //this is used to check whether a synset is geographical or not
	//private String fileName=GeoWorSE.GEOWN_HOME+"WNCoord.dat";
	public final static int WN_30=0;
	public final static int WN_20=1;
	public static int VERSION=WN_20; //wordnet 2.0 default
	
	public GeoWordNetDictionary(){
		dict= new HashMap();
	    BufferedReader reader;
	    String fileName=GeoWorSE.GEOWN_HOME;
	    if(fileName.indexOf("GeoWN3.") > -1){
	    	fileName+="mapping.dat";
	    	VERSION=WN_30;
	    } else {
	    	fileName+="WNCoord.dat";
	    	VERSION=WN_20;
	    }
	    
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		    String line;
		    while((line=reader.readLine())!= null){
		    	if(VERSION==WN_20){
		    		String[] elems = line.split(" ");
		    		dict.put(elems[0], elems[1]+":"+elems[2]);	
		    	} else {
		    		String[] elems = line.split("\t");
		    		dict.put(elems[0], elems[2]+":"+elems[3]);
		    	}
			}
		    reader.close();
		
			allowedWNCodes=new HashSet();
			reader = new BufferedReader(new InputStreamReader(new FileInputStream("wnhypes.txt")));
			line="";
			while((line=reader.readLine())!= null){
				allowedWNCodes.add(line.trim());
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	
	/**
	 * returns true if the passed offset is contained
	 * @param offset
	 * @return
	 */
	public boolean contains(int offset){
		String off_str = StringTools.leftZeroPad(""+offset, 8);
		if(dict.containsKey(off_str)) return true;
		else return false;
	}
	
	public float getLat(String offset) throws GeoWNException {
		try{
			String[] val=((String)dict.get(offset)).split(":");
			return Float.parseFloat(val[0]);
		} catch (Throwable t){
			throw new GeoWNException();
		}
	}
	
	public float getLon(String offset) throws GeoWNException {
		try{
			String[] val=((String)dict.get(offset)).split(":");
			return Float.parseFloat(val[1]);
		} catch (Throwable t){
			throw new GeoWNException();
		}
	}
	
	public float getLat(int offset) throws GeoWNException{
		return getLat(StringTools.leftZeroPad(""+offset, 8));
	}
	
	public float getLon(int offset) throws GeoWNException{
		return getLon(StringTools.leftZeroPad(""+offset, 8));
	}
	
	public WorldPoint getWP(int offset) throws GeoWNException, InvalidCoordinateRangeException{
		return new WorldPoint(getLat(offset), getLon(offset));
	}
	
	@SuppressWarnings("unchecked")
	public List<WorldPoint> getWorld() {
		Vector<WorldPoint> ret = new Vector<WorldPoint>();
		Iterator<String> itr = this.dict.values().iterator();
		while(itr.hasNext()){
			String [] s = itr.next().split(":");
			try {
				ret.addElement(new WorldPoint(Float.parseFloat(s[0]), Float.parseFloat(s[1])));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (InvalidCoordinateRangeException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	public boolean isGeographicalFeature(String feature_code){
		return allowedWNCodes.contains(feature_code);
	}
	
}
