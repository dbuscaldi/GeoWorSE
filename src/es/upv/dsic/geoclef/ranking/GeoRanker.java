package es.upv.dsic.geoclef.ranking;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

import es.upv.dsic.geoclef.geography.GeoUtils;
import es.upv.dsic.geoclef.geography.InvalidCoordinateRangeException;
import es.upv.dsic.geoclef.geography.WorldPoint;
import es.upv.dsic.geoclef.topics.Topic;

public class GeoRanker implements Ranker {
	private Vector<Entry> entries;
	private Iterator<Entry> itr;
	private static int P_LIMIT=5000;
	
	@SuppressWarnings("unchecked")
	public GeoRanker(Topic t, IndexSearcher searcher, ScoreDoc[] hits){
		entries= new Vector<Entry>();
		if(t.isGeoReferenced()){
			
			int end=Math.min(hits.length, P_LIMIT);
			//TODO: valutare utilità spread dei pesi dei documenti (se spread basso => tutti i documenti sono simili)
			//valutare l'uso di media, deviazione standard?
			
			/*
			float maxWeight=0;
			float minWeight=Float.POSITIVE_INFINITY;
			for(int i=0; i< end; i++){
				try{
					float tmpWeight=hits.score(i);
					if(tmpWeight > maxWeight) maxWeight=tmpWeight;
					if(tmpWeight < minWeight) minWeight=tmpWeight;
				} catch(Exception e){
					e.printStackTrace();
					System.exit(-1);
				}
			}
			
			float spreadWeight=maxWeight-minWeight;
			//System.err.println("max: "+maxWeight+" min: "+minWeight+" spread: "+spreadWeight);
			*/
			
			for(int i=0; i< end; i++){
				try {
					Document d= searcher.doc(hits[i].doc);
					String coordField=d.get("coord");
					String docText=d.get("text");
					String docId=d.get("id");
					float docWeight=hits[i].score;
					if(coordField != null && coordField.length() > 1){
						String [] coords = (coordField.trim()).split(" "); //list of coordinates in the document
						//2 casi: area o punto
						if(!t.isPointReference()){
							//area	
							int count_in=0;
							for(int j=0; j< coords.length; j++){
								String [] pc = coords[j].split(":");
								WorldPoint p;
								try {
									p = new WorldPoint(Float.parseFloat(pc[0]), Float.parseFloat(pc[1]));
								
								
									if(GeoUtils.inPolygon(p, t.getArea())){
										count_in++;
									} else {
										/*System.err.println("Point outside polygon found: " + p.canonicRepr());
										 System.err.println("Topic SS: "+t.getStandardSearchStr(true));
										 System.err.println("Area:");
										 Iterator<WorldPoint> jtr =t.getArea().iterator();
										 while(jtr.hasNext()){
										 System.err.println(jtr.next().canonicRepr());
										}
										System.err.println(docText.substring(0, Math.min(docText.length(), 1024)));*/
									}
								} catch (NumberFormatException e1) {
									e1.printStackTrace();
								} catch (InvalidCoordinateRangeException e1) {
									System.err.println(e1.getMessage());
									e1.printStackTrace();
								}
							}
							
							//TODO: stimare correttamente le correzioni
							float reduction = 1/(100*docWeight); //-> MAP 0,3215 //(float) 0.01;
							//float reduction = spreadWeight/(100*docWeight);
							
							//float reduction = (1-docWeight); // -> MAP 0,3311 in GC05
							//float reduction = spreadWeight*(1-docWeight); 
							
							//float reduction = 1/(float)Math.pow(10, docWeight+1);
							
							float correction = (float) reduction*(float)count_in/(float)coords.length;
							//float correctionOut = (float) (reduction*(float)(coords.length-count_in));
							Entry e = new Entry(docId, docText, (float) (docWeight*(1+correction)));
							//Entry e = new Entry(docId, docText, (docWeight*(1+correction-correctionOut)));
							//System.err.println("previous weight: "+docWeight+" -> new weight: "+e.getWeight());
							this.entries.addElement(e);
												
						} else {
							//punto
							float minDistance=Float.POSITIVE_INFINITY;
							for(int j=0; j< coords.length; j++){
								String [] pc = coords[j].split(":");
								
								WorldPoint p;
								try {
									p = new WorldPoint(Float.parseFloat(pc[0]), Float.parseFloat(pc[1]));
									float distance = p.kilometricDistance(t.getCentroid());
									if(distance < minDistance) minDistance=distance;
								} catch (NumberFormatException e) {
									e.printStackTrace();
								} catch (InvalidCoordinateRangeException e) {
									System.err.println(e.getMessage());
									e.printStackTrace();
								}
							}
						
							if(minDistance < Float.POSITIVE_INFINITY){
								//System.err.println("min distance: "+minDistance+" -> correction: "+Math.exp(-minDistance));
								docWeight=docWeight* (1 + (float)(Math.exp(-minDistance)));
								Entry e = new Entry(docId, docText, docWeight);
								this.entries.addElement(e);
							}
						}
					} else {
						//no "coord" field
						Entry e = new Entry(docId, docText, docWeight);
						this.entries.addElement(e);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			}
			
			
		} else {
			//no geo reference!
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
