package es.upv.dsic.geoclef.ranking;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;


import es.upv.dsic.geoclef.topics.Topic;

public class BaseRanker implements Ranker {
	private Vector<Entry> entries;
	private Iterator<Entry> itr;
	private static int P_LIMIT=5000;
		

	@SuppressWarnings("unchecked")
	public BaseRanker(Topic t, IndexSearcher searcher, ScoreDoc[] hits){
		entries= new Vector<Entry>();
		int end=Math.min(hits.length, P_LIMIT);
		for(int i=0; i< end; i++){
			try {
				Document d= searcher.doc(hits[i].doc);
				String docText=d.get("text");
				String docId=d.get("id");
				float docWeight=hits[i].score;
				this.entries.addElement(new Entry(docId, docText, docWeight));
			} catch (IOException ex) {
				ex.printStackTrace();
				System.exit(-1);
			}
		}
		Collections.sort(entries);
		itr=this.entries.iterator();
	
	}	
	
		
	public Entry getNext(){
		return this.itr.next();
	}
		
	public boolean hasMoreEntries(){
		return this.itr.hasNext();
	}
}
