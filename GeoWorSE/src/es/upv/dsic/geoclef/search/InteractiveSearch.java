package es.upv.dsic.geoclef.search;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryParser.QueryParser;

class InteractiveSearch {
  
  private static final int MAX_HITS = 100;

public static String analyzeText(String file, String queryText) throws IOException{
  	String tline="";
  	
  	BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
    String tlineB=""; //lineBefore
    System.err.println("searching...");
    
    queryText=queryText.replaceAll("\"","").trim(); //TODO: qualcosa di + furbo...
    while (true) {
    		tlineB=tline;
    		tline=f.readLine();
    		if (tline==null) break;
    		if(tline.indexOf(queryText) > -1){
    			System.out.println(tlineB);
    			System.out.println(tline);
    			String tlineA=f.readLine();
    			System.out.println(tlineA);
    			return (tlineB+'\n'+tline+'\n'+tlineA);
    		}
    }
    f.close();
	return "not matched!";
  }
  
  private static String snippet(String query, String fulltext){
	int start = fulltext.indexOf(query);
	int ns = Math.max(0, start-100);
	int es = Math.min(ns+200, fulltext.length());
	//System.out.println(query+"\nstart: "+start+", ns: "+ns+" , es: "+es);
	return fulltext.substring(ns, es);
  }
  
  public static void main(String[] args) {
  	/*String usage = "java " + InteractiveSearch.class + " <lang>\n\n(lang: one among 'English', 'Spanish', 'French' or 'Italian')";
  	if (args.length == 0 || !(args[0].equals("English") || args[0].equals("Spanish") || args[0].equals("French") || args[0].equals("Italian"))) {
  		System.err.println("Usage: " + usage);
  		System.exit(1);
  	}
  	String lang = args[0];
  	*/
  	try {
      IndexReader reader = IndexReader.open(FSDirectory.open(new File("indexLGLEnglish")));
      IndexSearcher searcher = new IndexSearcher(reader);
      Analyzer analyzer = new SnowballAnalyzer(Version.LUCENE_36, "English");

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      while (true) {
	System.out.print("Query: ");
	
	String line = in.readLine();
	//String rawQuery = line;
	
	if (line.length() == -1)
	  break;
	QueryParser qp = new QueryParser(Version.LUCENE_36, "text", analyzer);
	Query query = qp.parse(line);
	
	System.out.println("Searching for: " + query.toString("contents"));

	TopDocs results = searcher.search(query, MAX_HITS);
	ScoreDoc[] hits = results.scoreDocs;
	System.out.println(results.totalHits + " total matching documents");

	final int HITS_PER_PAGE = 10;
	for (int start = 0; start < hits.length; start += HITS_PER_PAGE) {
	  int end = Math.min(hits.length, start + HITS_PER_PAGE);
	  for (int i = start; i < end; i++) {
	    Document doc = searcher.doc(hits[i].doc);
	    String docID = doc.get("id");
	    String fullText = doc.get("text");
	    String geoTerms = doc.get("geo");
	    //String wnTerms = doc.get("wn");
	    if (docID != null) {
              System.out.println(i + ". " + docID + " , geoLoc: "+geoTerms);
              System.out.println("score: "+hits[i].score);
              //System.out.println("snippet:\n"+snippet(line, fullText));
              System.out.println(fullText);
              //System.out.println(fullText.substring(0,Math.min(2048, fullText.length())));
              System.out.println();
	    } else {
	    		System.out.println(i + ". " + "Unknown id");
	    }
	  }

	  if (hits.length > end) {
	    System.out.print("more (y/n) ? ");
	    line = in.readLine();
	    if (line.length() == 0 || line.charAt(0) == 'n')
	      break;
	  }
	}
      }
      searcher.close();

    } catch (Exception e) {
      System.out.println(" caught a " + e.getClass() +
			 "\n with message: " + e.getMessage());
    }
  }
}
