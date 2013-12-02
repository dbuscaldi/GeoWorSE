/*
 * Created on 8-giu-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package es.upv.dsic.geoclef.search;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.SAXException;

import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.ranking.BaseRanker;
import es.upv.dsic.geoclef.ranking.Entry;
import es.upv.dsic.geoclef.ranking.GeoRanker;
import es.upv.dsic.geoclef.ranking.Ranker;
import es.upv.dsic.geoclef.topics.Topic;
import es.upv.dsic.geoclef.topics.Topic07Handler;

/**
 * @author davide
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ArtificialAmbiguitySearch {
	public final static int TITLE_ONLY=0;
	public final static int TITLE_DESC=1;
	public final static int ALL=2;
	
	private static String runID="";
	private static String outputFile="results-Geo";
	private static double errorLevel=0.0;
	
	//search parameters
	private final static double geoBoost=0.25; //0.5 best TDN-2007, per TD-2008 0.25
	private final static double wordnetBoost=0.25; //0.25 best per TD
	
	public final static boolean USE_MAP_RANKING=true; //use map-based ranking (true) or not
	private final static int mode=ALL; //use title_desc or all fields
	public final static boolean PASSAGE_SEARCH=false;
	public final static boolean SIMULATED_ERROR=true;
	
	//visualization parameters
	public final static boolean MAP_ACTIVE=false; //if you want to see the map (true) or not
	private static final int MAX_HITS = 1000;
	
	private static PrintStream out;
	
	private static String fileExtension(){
		String ext="err";
		ext+=Double.toString(errorLevel*100).substring(0,2);
		switch(mode){
			case TITLE_ONLY: ext+="TO"; break;
			case TITLE_DESC: ext+="TD"; break;
			default: ext+="ALL";
		}
		runID=ext; //uso ext per il runID
		if (USE_MAP_RANKING) ext=ext+"_GR";
		return ext;
	}
	
	private static void doSearch(Topic t){
		try {
			  
			  String geoSStr=t.getWNGeoSStr(mode, geoBoost, wordnetBoost); //con WordNet
			  //String geoSStr=t.getGeoSStr(mode, geoBoost);
			  //String geoSStr=t.getStandardSearchStr(mode);
			  //System.err.println(geoSStr);
			  
			  IndexReader reader; 
			  
			  if(PASSAGE_SEARCH){
				  if(SIMULATED_ERROR){
					  reader = IndexReader.open(FSDirectory.open(new File("p_indexEnglish-Err"+Double.toString(errorLevel*100))));
				  } else {
					  reader = IndexReader.open(FSDirectory.open(new File("p_indexEnglish")));
				  }
			  } else {
				  if(SIMULATED_ERROR){
					  reader = IndexReader.open(FSDirectory.open(new File("indexEnglish-Err"+Double.toString(errorLevel*100))));
				  } else {
					  reader = IndexReader.open(FSDirectory.open(new File("indexEnglish")));
				  }
			  }
			  
			  IndexSearcher searcher = new IndexSearcher(reader);
			  Analyzer analyzer = new SnowballAnalyzer(GeoWorSE.LVERSION, "English");

//		      System.err.println("Query: "+geoSStr);
		      QueryParser qp = new QueryParser(GeoWorSE.LVERSION, "text", analyzer);
		  	  Query query = qp.parse(geoSStr);		      
		      //System.err.println("Searching for: " + query.toString("contents"));

		  	  TopDocs results = searcher.search(query, MAX_HITS);
		  	  ScoreDoc[] hits = results.scoreDocs;
//		      System.err.println(hits.length() + " total matching documents");
		      //System.out.println("<docs>");of spies. His house in Istanbul
		      Ranker ranker;
		      if(USE_MAP_RANKING){
		    	  ranker = new GeoRanker(t, searcher, hits);
		      }
		      else {
		    	  ranker = new BaseRanker(t, searcher, hits);
		      }
		      int i=0; //i=1 for 2008?
		      HashSet<String> docs= new HashSet<String>(); 
		      while(ranker.hasMoreEntries() && i < MAX_HITS){
		    	  Entry e = ranker.getNext();
		    	  String docID= e.getDoc();
		    	  if(PASSAGE_SEARCH) {
		    		  int cutIdx = docID.lastIndexOf("-");
		    		  if(cutIdx > -1) docID=docID.substring(0, cutIdx);
		    		  if(docID.length() > 8){
		    			  if(!docs.contains(docID)){
		    				  out.println(t.getID()+" Q0 "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
			    			  //System.out.println(t.getID()+" Q0 "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
			    			  docs.add(docID);
			    			  i++;
		    			  }
		    		  }
		    		  
		    	  } else {
		    		  if (docID != null) {
		    			  out.println(t.getID()+" Q0 "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
			    		  //System.out.println(t.getID()+" Q0 "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
			    		  //System.out.println("<doc>"+docID.trim()+"</doc>");
			    		  //System.out.println(i + ". " + docID);
			    	  } else {
			    		  out.println(t.getID()+" Q0 unknown "+i+" "+e.getWeight()+" "+runID);
			    		  //System.out.println(t.getID()+" Q0 unknown "+i+" "+e.getWeight()+" "+runID);
			    		  //System.out.println("<doc></doc>");
			    		  //System.out.println(i + ". " + "Unknown id");
			    	  }
		    		  i++;
		    	  }
		    	  
		      }
		      out.flush();
		      //System.out.println("</docs>");
		      searcher.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		GeoWorSE.init();
	    
		//		leggo i topic
	    //Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2005-2007format.xml"), GeoWorSE.classifier, mode);
	    //Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2006-2007format.xml"), classifier, mode);
	    //Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2007.xml"), classifier, mode);
		Topic07Handler th=new Topic07Handler(new File("/home/datasets/geoclef/topics/alltopics.xml"), GeoWorSE.classifier, mode);
	    //Topic08Handler th=new Topic08Handler(new File("/home/datasets/geoclef/topics/en2008.xml"), GeoWorSE.classifier, mode);
		
		Vector<Topic> ts=th.getTopics();
		
		for(errorLevel=0.0; errorLevel < 0.7; errorLevel+=0.1){
			outputFile="results-Geo";
			outputFile+="-"+fileExtension()+"_gb"+geoBoost+"_wnb"+wordnetBoost+".txt";
			
			out = new PrintStream(new File(outputFile));
			for(int i=0; i< ts.size(); i++){
				doSearch(ts.elementAt(i));

			}
			
		    out.close();
	    }

//		questo solo nel caso si voglia esaminare un solo topic
		//Topic top = th.getTopic("10.2452/80-GC");
		//doSearch(top);
		
		GeoWorSE.close();
	}
}
