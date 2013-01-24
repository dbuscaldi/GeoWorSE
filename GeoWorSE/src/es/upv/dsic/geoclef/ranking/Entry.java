package es.upv.dsic.geoclef.ranking;

import java.util.List;
import java.util.Vector;

import es.upv.dsic.geoclef.geography.InvalidCoordinateRangeException;
import es.upv.dsic.geoclef.geography.WorldPoint;

public class Entry implements Comparable {
	private String docId;
	private String text;
	private String points;
	private float weight;
	
	public Entry(String doc, String text, float w){
		this.docId=doc;
		this.text=text;
		this.weight=w;
	}
	
	public Entry(String doc, String text, String points, float w){
		this.docId=doc;
		this.text=text;
		this.points=points;
		this.weight=w;
	}
	
	public String getDoc(){
		return this.docId;
	}
	
	public String getText(){
		return this.text;
	}
	
	public float getWeight(){
		return this.weight;
	}
	
	public Float getCmpWeight(){
		return new Float(this.weight);
	}
	
	public void setWeight(float w){
		this.weight=w;
	}
	
	public String getPointsNames(){
		return this.points;
	}
	
	public List<WorldPoint> getEntryPoints() throws NumberFormatException, InvalidCoordinateRangeException{
		List<WorldPoint> l = new Vector<WorldPoint>();
		if(this.points != null && this.points.length() > 1){
			String [] coords = (this.points.trim()).split(" "); //list of coordinates in the document
			for(int j=0; j< coords.length; j++){
				String [] pc = coords[j].split(":");
				WorldPoint p = new WorldPoint(Float.parseFloat(pc[1]), Float.parseFloat(pc[0]));
				l.add(p);
			}
		}
		return l;
	}
	
	public int compareTo(Object anotherEntry) throws ClassCastException {
	    if (!(anotherEntry instanceof Entry))
	      throw new ClassCastException("An Entry object expected.");
	    Float tf=new Float(this.weight);
	    Float of = ((Entry)anotherEntry).getCmpWeight();
	    //return tf.compareTo(of); //ordine crescente
	    return of.compareTo(tf); //ordine decrescente
	  }
}
