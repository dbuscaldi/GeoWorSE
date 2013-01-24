/*
 * Created on 8-giu-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package es.upv.dsic.geoclef.search;

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Vector;

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
public class GeoSearch {
	public final static int TITLE_ONLY=0;
	public final static int TITLE_DESC=1;
	public final static int ALL=2;
	
	private static String runID="";
	private static String outputFile="results-GeoNames"; //"results-GeoWN";
	private static PrintStream out;
	private static double errorLevel=0.0;
	
	//search parameters
	private final static double geoBoost=0.25; //0.5 best TDN-2007, per TD-2008 0.25
	private final static double wordnetBoost=0.25; //0.25 best per TD
	
	public final static boolean USE_MAP_RANKING=false; //use map-based ranking (true) or not
	public final static boolean QUERY_MERONYM_EXPANSION = false; //this field is used to expand toponyms in queries with their meronyms (CLEF 2005 participation)
	public final static boolean DIVERSITY_SEARCH=false; //this field is used in Geographical Diversity experiments
	private final static int mode=TITLE_DESC; //use title_desc or all fields for GeoCLEF
	
	
	public final static boolean PASSAGE_SEARCH=false;
	public final static boolean SIMULATED_ERROR=false;
	public final static boolean GEONAMES_INDEX=true; //not compatible with passage search nor simulated error
	
	public final static boolean NO_GEO=false; //desactivate every geo-index feature
	public final static boolean NO_WSD=false; //if activated, search in nd_indexEnglish (all senses)
	
	public final static boolean LGL=false; //Search the LGL collection
	//visualization parameters
	public final static boolean MAP_ACTIVE=false; //if you want to see the map (true) or not
	private static final int MAX_HITS = 1000;
	public static IndexSearcher searcher;
	
	private static void checkConsistency() throws Exception{
		//this method provides consistency of selected options
		if(LGL) {
			if(SIMULATED_ERROR==true || PASSAGE_SEARCH==true) throw new Exception("Incompatible Options: LGL and Passage Search or Simulated error");
		}
		if(NO_GEO){
			if(GEONAMES_INDEX==true) throw new Exception("Incompatible Options: NO_GEO with GEONAMES_INDEX");
		}
	}
	
	private static String fileExtension(){
		String ext;
		if(SIMULATED_ERROR) {
			ext="err";
			ext+=Double.toString(errorLevel*10).substring(0,2);
		} else ext="std";
		switch(mode){
			case TITLE_ONLY: ext+="_TO"; break;
			case TITLE_DESC: ext+="_TD"; break;
			default: ext+="_ALL";
		}
		runID=ext; //uso ext per il runID
		if(PASSAGE_SEARCH) ext+="_pssg";
		if(USE_MAP_RANKING) ext+="_GR";
		if(NO_WSD) ext+="_NOWSD";
		if(DIVERSITY_SEARCH) ext+="_DS";
		return ext;
	}
	
	private static void doSearch(Topic t){
		try {
			  //prepare topics
			  String geoSStr="";
			  if(NO_GEO) geoSStr=t.getStandardSearchStr(mode);
			  else {
				  if(!QUERY_MERONYM_EXPANSION) geoSStr=t.getWNGeoSStr(mode, geoBoost, wordnetBoost);
				  else geoSStr = t.getQMEGeoSStr(mode, geoBoost, wordnetBoost);
			  }
			  
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
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}
	
	public static void main(String[] args) throws Exception {
		checkConsistency(); //check configuration options
		GeoWorSE.init();
	    
		IndexReader reader;
		//set correct index searcher
	  	if(LGL) {
	  		if(NO_WSD){
	  			reader = IndexReader.open(FSDirectory.open(new File("nd_indexLGLEnglish")));
	  		} else reader = IndexReader.open(FSDirectory.open(new File("indexLGLEnglish")));
		 }
		 else{
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
					if(NO_WSD){
						if(!GEONAMES_INDEX) reader = IndexReader.open(FSDirectory.open(new File("nd_indexEnglish")));
						else reader = IndexReader.open(FSDirectory.open(new File("nd_gn_indexEnglish")));
					}
					else{
						if(!GEONAMES_INDEX){
							reader = IndexReader.open(FSDirectory.open(new File("indexEnglish")));  
						} else {
							reader = IndexReader.open(FSDirectory.open(new File("gn_indexEnglish")));
						} 
					}
				}
			}
		}
	    searcher = new IndexSearcher(reader);
		  
		//leggo i topic
		Topic07Handler th;
	    //th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2005-2007format.xml"), GeoWorSE.classifier, mode);
	    //th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2006-2007format.xml"), classifier, mode);
	    //th=new Topic07Handler(new File("/home/datasets/geoclef/topics/en2007.xml"), classifier, mode);
		if(!LGL) th=new Topic07Handler(new File("/home/datasets/geoclef/topics/alltopics.xml"), GeoWorSE.classifier, mode);
		else th=new Topic07Handler(new File("/home/davide/Works/LGL/LGL/topicsLGL-IR.xml"), GeoWorSE.classifier, mode);
		//Topic08Handler th=new Topic08Handler(new File("/home/datasets/geoclef/topics/en2008.xml"), GeoWorSE.classifier, mode);
		
		Vector<Topic> ts=th.getTopics();
		
		
		
		if(SIMULATED_ERROR){
			for(errorLevel=0.0; errorLevel < 0.7; errorLevel+=0.1){
				if(!PASSAGE_SEARCH) outputFile="results-GeoWN-"+fileExtension()+"_gb"+geoBoost+"_wnb"+wordnetBoost+".txt";
				else outputFile="results-GeoWN-pssg-"+fileExtension()+"_gb"+geoBoost+"_wnb"+wordnetBoost+".txt";
				
				out = new PrintStream(new File(outputFile));
				for(int i=0; i< ts.size(); i++){
					if(!DIVERSITY_SEARCH) doSearch(ts.elementAt(i));
					else doDivSearch(ts.elementAt(i));

				}
				out.close();
				searcher.close();
		    }
		} else {
			if(NO_GEO){
				if(!LGL) outputFile="results-Lucene-"+fileExtension()+".txt";
				else outputFile="results-LGL-BaseLucene-"+fileExtension()+".txt";
			} else {
				if(!LGL){
					if(GEONAMES_INDEX) outputFile="results-GeoNames-"+fileExtension()+"_gb"+geoBoost+"_wnb"+wordnetBoost+".txt";
					else outputFile="results-GeoWN-"+fileExtension()+"_gb"+geoBoost+"_wnb"+wordnetBoost+".txt";	
				} else outputFile="results-LGL-"+fileExtension()+"_gb"+geoBoost+"_wnb"+wordnetBoost+".txt";
			}
			out = new PrintStream(new File(outputFile));
			  
			for(int i=0; i< ts.size(); i++){
				if(!DIVERSITY_SEARCH) doSearch(ts.elementAt(i));
				else doDivSearch(ts.elementAt(i));
			}
			out.close();
			searcher.close();
		}
		

//		questo solo nel caso si voglia esaminare un solo topic
		//Topic top = th.getTopic("10.2452/80-GC");
		//doSearch(top);
		
		GeoWorSE.close();
	}
	
	private static void doDivSearch(Topic t){
		try {	  
			  //prepare topics
			  Vector<String> searchStrings=t.getDiversitySearchStrings(mode, geoBoost, wordnetBoost);
			  for(String ss: searchStrings) System.err.println(ss);
			  System.err.println();
			   
			  Analyzer analyzer = new SnowballAnalyzer(GeoWorSE.LVERSION, "English");
			  
			  for(int j=0; j<searchStrings.size(); j++){
				  String geoSStr = searchStrings.elementAt(j);
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
			      final int MAXHITS = 1000;
			      int i=0; //i=1 for 2008?
			      HashSet<String> docs= new HashSet<String>(); 
			      while(ranker.hasMoreEntries() && i < MAXHITS){
			    	  Entry e = ranker.getNext();
			    	  String docID= e.getDoc();
			    	  if(PASSAGE_SEARCH) {
			    		  int cutIdx = docID.lastIndexOf("-");
			    		  if(cutIdx > -1) docID=docID.substring(0, cutIdx);
			    		  if(docID.length() > 8){
			    			  if(!docs.contains(docID)){
			    				  //NOTE: in DivSearch, the "Q" field is used to indicate the set of results generated by the same query
			    				  out.println(t.getID()+" Q"+j+" "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
				    			  //System.out.println(t.getID()+" Q0 "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
				    			  docs.add(docID);
				    			  i++;
			    			  }
			    		  }
			    		  
			    	  } else {
			    		  if (docID != null) {
			    			  out.println(t.getID()+" Q"+j+" "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
				    		  //System.out.println(t.getID()+" Q0 "+docID.trim()+" "+i+" "+e.getWeight()+" "+runID);
				    		  //System.out.println("<doc>"+docID.trim()+"</doc>");
				    		  //System.out.println(i + ". " + docID);
				    	  } else {
				    		  out.println(t.getID()+" Q"+j+" unknown "+i+" "+e.getWeight()+" "+runID);
				    		  //System.out.println(t.getID()+" Q0 unknown "+i+" "+e.getWeight()+" "+runID);
				    		  //System.out.println("<doc></doc>");
				    		  //System.out.println(i + ". " + "Unknown id");
				    	  }
			    		  i++;
			    	  }
			    	  
			      }
			      out.flush();
			 
			  }
		      //System.out.println("</docs>");
		      
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}
}
