package es.upv.dsic.geoclef.stats;

import edu.mit.jwi.item.ISynset;
import es.upv.dsic.GeoNames.GeoNamesDisambiguator;
import es.upv.dsic.GeoNames.GeoNamesLocation;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.indexing.geonames.GNSAXHandler;

import java.io.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;


class GeoPlanetStats {
  //static Hashtable<GeoNamesLocation, Integer> places; //it is document count, not instance count
  static Hashtable<GeoNamesLocation, Integer> GH95_places;
  static Hashtable<GeoNamesLocation, Integer> LAT94_places;
  
  public static void indexDir(String dir, String lang) 	{
  	Date start = new Date();
    try {
      checkDocs(new File(dir));
      
      Date end = new Date();

      System.out.print(end.getTime() - start.getTime());
      System.out.println(" total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }
	
  public static void main(String[] args) throws IOException {
    String usage = "java " + GeoPlanetStats.class + " <root_directory>";
//    if (args.length == 0) {
//      System.err.println("Usage: " + usage);
//      System.exit(1);
//    }
    
    //places = new Hashtable<GeoNamesLocation, Integer>();
    GH95_places = new Hashtable<GeoNamesLocation, Integer>();
    LAT94_places = new Hashtable<GeoNamesLocation, Integer>();
    GeoWorSE.init();
    GeoNamesDisambiguator.init();
    
    indexDir("/home/datasets/geoclef/data", "English");
    
    GeoWorSE.close();
    
    PrintStream out= new PrintStream(new FileOutputStream("GeoNames_frequencies_GH95.txt"));
    
    List<Entry<GeoNamesLocation, Integer>> list = new ArrayList<Entry<GeoNamesLocation, Integer>>(GH95_places.entrySet());
	
    Collections.sort(list, new Comparator<Entry<GeoNamesLocation,Integer>>() {
		public int compare(Entry<GeoNamesLocation, Integer> e1, Entry<GeoNamesLocation, Integer> e2) {
			Integer i1 = (Integer) e1.getValue();
			Integer i2 = (Integer) e2.getValue();
			return i2.compareTo(i1);
		}
	});
    
    for(Entry<GeoNamesLocation, Integer> e :list){
    	out.println(e.getKey().getID()+"\t"+e.getValue());
    }
    
    out.close();
    
    PrintStream out1= new PrintStream(new FileOutputStream("GeoNames_frequencies_LA94.txt"));
    
    List<Entry<GeoNamesLocation, Integer>> list2 = new ArrayList<Entry<GeoNamesLocation, Integer>>(LAT94_places.entrySet());
	
    Collections.sort(list2, new Comparator<Entry<GeoNamesLocation,Integer>>() {
		public int compare(Entry<GeoNamesLocation, Integer> e1, Entry<GeoNamesLocation, Integer> e2) {
			Integer i1 = (Integer) e1.getValue();
			Integer i2 = (Integer) e2.getValue();
			return i2.compareTo(i1);
		}
	});
    
    for(Entry<GeoNamesLocation, Integer> e :list2){
    	out.println(e.getKey().getID()+"\t"+e.getValue());
    }
    out1.close();
    
  }

  public static void checkDocs(File file)
    throws IOException {
  	// do not try to index files that cannot be read
  	if (file.canRead()) {
      if (file.isDirectory()) {
      	String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            checkDocs(new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file);
        try {
        		GNSAXHandler hdlr = new GNSAXHandler(file);
        		HashSet<GeoNamesLocation> geo_locs=hdlr.getLocations();
        		if(file.getAbsolutePath().indexOf("gh95") > -1){
        			//System.err.println("GH path");
        			for(GeoNamesLocation gl : geo_locs){
        				if(!GH95_places.contains(gl)){
        					GH95_places.put(gl, new Integer(1));
        				} else {
        					Integer val = (GH95_places.get(gl));
        					GH95_places.put(gl, new Integer(val.intValue()+1));
        				}
        			}
        		}
        		if(file.getAbsolutePath().indexOf("latimes94") > -1){
        			//System.err.println("LA94 path");
        			for(GeoNamesLocation gl : geo_locs){
        				if(!LAT94_places.contains(gl)){
        					LAT94_places.put(gl, new Integer(1));
        				} else {
        					Integer val = (LAT94_places.get(gl));
        					LAT94_places.put(gl, new Integer(val.intValue()+1));
        				}
        			}
        		}
        		//places.addAll(geo_locs);
        		
            //writer.addDocument(hdlr.getDocument());
            //writer.addDocument(FileDocument.Document(file));
        }
        catch (Exception e) {
        	e.printStackTrace();
        } 
      }
    }
  }
}
