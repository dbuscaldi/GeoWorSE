package es.upv.dsic.tools;

import java.io.IOException;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.search.GeoSearch;

public class FrequencyFilter {
	private static Analyzer analyzer = new SnowballAnalyzer(GeoWorSE.LVERSION, "English");
	
	public static int getFrequency(String w, String field) throws IOException{
		return GeoSearch.searcher.docFreq(new Term(field, StringTools.stemEnglish(w.toLowerCase())));
	}
	
	private static double getResult(Query q) throws IOException{
		TopDocs results = GeoSearch.searcher.search(q, 1000);
	  	ScoreDoc[] hits = results.scoreDocs;
		int phraseFreq = results.totalHits; 
	
		double maxDocs = (double)GeoSearch.searcher.maxDoc();
		
		double theta = (double)phraseFreq/(double)maxDocs;
		double gain = theta*(theta-1-Math.log(theta));
		//if(phraseFreq > 0) System.err.println("Query: "+q.toString()+" hits: "+hits.length()+" , gain: "+gain);
			
		return gain;
	}
	
	public static double calcPhraseGain(Vector<String> words,  String field) throws Exception {
		QueryParser qp = new QueryParser(GeoWorSE.LVERSION, field, analyzer);
		StringBuffer parseString = new StringBuffer();
		
		for(String w : words){
			parseString.append("+\""+w+"\" ");
		}
		Query query = qp.parse(parseString.toString().trim());
			
		return getResult(query);
	}
	
	public static double calcPhraseGain(String line, String field) throws Exception {
		QueryParser qp = new QueryParser(GeoWorSE.LVERSION, field, analyzer);
		StringBuffer parseString = new StringBuffer();
		
		parseString.append("+\""+line+"\" ");
		Query query = qp.parse(parseString.toString().trim());
		
		return getResult(query);
	}
	
	public static double mixedFieldGain(Vector<String> a, String field_a, String b, String field_b) throws Exception {
		QueryParser qp = new QueryParser(GeoWorSE.LVERSION, field_a, analyzer);
		StringBuffer parseString = new StringBuffer();
		
		for(String w : a){
			parseString.append("+\""+w+"\" ");
		}
		
		parseString.append("+"+field_b+":\""+b+"\"");
		
		Query query = qp.parse(parseString.toString().trim());
		
		return getResult(query);
	}
}
