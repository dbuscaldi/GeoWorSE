package es.upv.dsic.geoclef.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import es.upv.dsic.geoWordNet.GeoWNException;
import es.upv.dsic.geoWordNet.GeoWordNetDictionary;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.indexing.DocumentVector;
import es.upv.dsic.geoclef.indexing.XMLDocumentHandlerSAX;

public class StatCollector {
	
	private static boolean SCALED_MODE=true;
	static Hashtable<ISynset, Integer> geoSynsets = new Hashtable<ISynset, Integer>();
	//stat values:
	private static int monosemicTopos=0; //number of monosemic toponyms
	private static int toponymNum=0;
	
	private static int freq_threshold=0;
	
  public static void examineDir(String dir, String lang) 	{
  	Date start = new Date();
    try {
      examineDocs(new File(dir));

      Date end = new Date();

      System.out.print(end.getTime() - start.getTime());
      System.out.println(" total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }
	
  public static void main(String[] args) throws IOException {
    String usage = "java " + StatCollector.class + " <root_directory>";
//    if (args.length == 0) {
//      System.err.println("Usage: " + usage);
//      System.exit(1);
//    }
    
    GeoWorSE.init();
    
    
	
    examineDir("/home/datasets/geoclef/data/gh95", "English");
     
    System.out.println("Writing KML file...");
	
	BufferedWriter out= new BufferedWriter(new OutputStreamWriter(new FileOutputStream("WN_GeoCLEF_synsets_GH95_scaled.kml")));
	
	out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n");
	out.write("<name>WN_GeoCLEF_Synsets.kml</name>\n<open>1</open>\n<description>WordNet Places in the GeoCLEF collection</description>\n");
	
	if(SCALED_MODE){
		out.write("<Style id=\"size5\">\n");
		out.write("<IconStyle>\n<scale>2.0</scale>\n<Icon>\n<href>http://maps.google.com/mapfiles/ms/micons/ylw-pushpin.png</href>\n");
		out.write("</Icon>\n</IconStyle>\n</Style>/n");
		out.write("<Style id=\"size4\">\n");
		out.write("<IconStyle>\n<scale>1.0</scale>\n<Icon>\n<href>http://maps.google.com/mapfiles/ms/micons/ylw-pushpin.png</href>\n");
		out.write("</Icon>\n</IconStyle>\n</Style>/n");
		out.write("<Style id=\"size3\">\n");
		out.write("<IconStyle>\n<scale>0.7</scale>\n<Icon>\n<href>http://maps.google.com/mapfiles/ms/micons/ylw-pushpin.png</href>\n");
		out.write("</Icon>\n</IconStyle>\n</Style>/n");
		out.write("<Style id=\"size2\">\n");
		out.write("<IconStyle>\n<scale>0.5</scale>\n<Icon>\n<href>http://maps.google.com/mapfiles/ms/micons/ylw-pushpin.png</href>\n");
		out.write("</Icon>\n</IconStyle>\n</Style>/n");
		out.write("<Style id=\"size1\">\n");
		out.write("<IconStyle>\n<scale>0.3</scale>\n<Icon>\n<href>http://maps.google.com/mapfiles/ms/micons/ylw-pushpin.png</href>\n");
		out.write("</Icon>\n</IconStyle>\n</Style>/n");
	}
	for(ISynset syn : geoSynsets.keySet()){
		double lon=0, lat=0;
		try{
			lon = GeoWorSE.geoWNdict.getLon(syn.getOffset());
			lat = GeoWorSE.geoWNdict.getLat(syn.getOffset());
		} catch (GeoWNException gwne){
			lon=-1000;
			System.err.println(gwne.getMessage());
			gwne.printStackTrace();
		}
		if(lon!=-1000 && geoSynsets.get(syn) > freq_threshold){
			out.write("<Placemark>\n");
			
			out.write("<name>"+(syn.getOffset()+":"+syn.getWord(1)).replaceAll("&", "&amp;")+"</name>\n");
			out.write("<description>"+(syn.getGloss())+"</description>\n");
			if(SCALED_MODE){
				int scale = (int) Math.round(Math.log10(geoSynsets.get(syn)));
				out.write("<styleUrl>#size"+scale+"</styleUrl>");
			}
			out.write("<Point><coordinates>"+lon+","+lat+"</coordinates></Point>\n");
			out.write("</Placemark>\n");
			out.flush();
		} 
	}
	out.write("</Document>");
	out.write("</kml>");    
	out.flush();
	out.close();
	
	System.out.println("done.");
    
	/* write synset frequencies */
	PrintStream out2= new PrintStream(new FileOutputStream("Geo_synsets_frequency_GH95.txt"));
	
	List<Entry<ISynset, Integer>> list = new ArrayList<Entry<ISynset, Integer>>(geoSynsets.entrySet());
	Collections.sort(list, new Comparator<Entry<ISynset,Integer>>() {
		public int compare(Entry<ISynset, Integer> e1, Entry<ISynset, Integer> e2) {
			Integer i1 = (Integer) e1.getValue();
			Integer i2 = (Integer) e2.getValue();
			return i2.compareTo(i1);
		}
	});
	
	out2.println("Offset\tword(1)\tfrequency");
	for(Entry<ISynset, Integer> e: list){
		out2.println(e.getKey().getOffset()+"\t"+e.getKey().getWord(1)+"\t"+e.getValue().intValue());
	}
    
	
	System.err.println("Number of toponyms found: "+toponymNum);
    System.err.println("Number of monosemic toponyms: "+monosemicTopos);
    System.err.println("% over total in the collection: "+((float)monosemicTopos*100/(float)toponymNum));
   
    
    GeoWorSE.close();
  }

  public static void examineDocs(File file)
    throws IOException {
  	// do not try to index files that cannot be read
  	if (file.canRead()) {
      if (file.isDirectory()) {
      	String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            examineDocs(new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file);
        try {
        		XMLStatDocHandlerSAX hdlr = new XMLStatDocHandlerSAX(file);
        		StatisticData stats = hdlr.getStats();
        		monosemicTopos+=stats.getMonoTopos();
        		toponymNum += stats.getTopos();
        		for(Entry<ISynset, Integer> k : hdlr.getGeoSynsets().entrySet()){
        			if(geoSynsets.containsKey(k.getKey())) geoSynsets.put(k.getKey(), k.getValue()+geoSynsets.get(k.getKey()));
        			else geoSynsets.put(k.getKey(), k.getValue());
        		}
        }
        catch (Exception e) {
        	e.printStackTrace();
        } 
      }
    }
  }

}
