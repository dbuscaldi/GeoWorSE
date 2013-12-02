package es.upv.dsic.geoclef.topics;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.search.GeoSearch;


public class GetTopicStats {
	public static boolean USE_MAP_RANKING=false; //use map-based ranking (true) or not
	private static int mode=GeoSearch.ALL; //use title_desc or all fields
	
	public static boolean MAP_ACTIVE=false; //if you want to see the map (true) or not
	
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		GeoWorSE.init();
	    
		//		leggo i topic
	    //Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2005-2007format.xml"), GeoWorSE.classifier, mode);
	    //Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2006-2007format.xml"), classifier, mode);
	    //Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2007.xml"), classifier, mode);
		Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/alltopics.xml"), GeoWorSE.classifier, mode);
	    //Topic08Handler th=new Topic08Handler(new File("/home/datasets/geoclef/topics/en2008.xml"), GeoWorSE.classifier, mode);
		
		Vector<Topic> ts=th.getTopics();
		
		int totalAmbiguous=0;
		int totalToponyms=0;
		System.out.println("id\t#toponyms\t#amb.toponyms");
		for(int i=0; i< ts.size(); i++){
			Topic t = ts.elementAt(i);
			int n_amb=t.countAmbiguousLocations();
			int n_topos=t.getLocations().size();
			totalAmbiguous+=n_amb;
			totalToponyms+=n_topos;
			System.out.println(t.getID()+"\t"+n_topos+"\t"+n_amb);
		}
		System.out.println("total:\t"+totalToponyms+"\t"+totalAmbiguous);

//		questo solo nel caso si voglia esaminare un solo topic
		//Topic top = th.getTopic("10.2452/80-GC");
		//doSearch(top);
		GeoWorSE.close();
	}

}
