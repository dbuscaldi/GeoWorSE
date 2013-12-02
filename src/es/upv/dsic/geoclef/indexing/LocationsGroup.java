package es.upv.dsic.geoclef.indexing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import es.upv.dsic.geoclef.geography.GeoUtils;
import es.upv.dsic.geoclef.geography.InvalidCoordinateRangeException;
import es.upv.dsic.geoclef.geography.WorldPoint;

public class LocationsGroup {
	private HashSet fcSet;
	private HashSet scSet;
	private List<WorldPoint> shape;
	private WorldPoint georeference;
	private boolean scattered; //dipende dalla distribuzione dei punti (scattering = troppo sparso per essere utile)
	
	public LocationsGroup(){
		this.fcSet=new HashSet();
		this.scSet=new HashSet();
		shape = new Vector<WorldPoint>();
		//georeference=null;
	}
	/**
	 * check whether it contains a point or not
	 * @param p
	 * @return
	 */
	private boolean contains(WorldPoint p){
		if(shape.contains(p)) return true;
		else return false;
	}
	/**
	 * adds a point to the locationsgroup
	 * @param p
	 */
	public void addPoint(WorldPoint p){
		if(!this.contains(p)) this.shape.add(p); //senza ripetizioni
	}
	
	/**
	 * include the provided PointSet to the current Set of Points
	 * @param s
	 */
	public void addPointSet(List<WorldPoint> s){
		Iterator<WorldPoint> i=s.iterator();
		while(i.hasNext()){
			WorldPoint p = i.next();
			if(!this.contains(p)) this.shape.add(p); //senza ripetizioni
		}
		//this.shape.addAll(s);
	}
	
	/**
	 * will set the georeference point for this area and check whether scattering occurs or not
	 *
	 */
	public void setGeoReference(){
		//TODO: non sembra utile, a parte lo scattered
		float dev=GeoUtils.deviation(this.shape);
		System.err.println("-------------------------------------------");
		System.err.println("Loc. Group: "+ this.getFC());
		System.err.println("Standard Deviation of locations group: "+dev);
		if(dev > 1000) this.scattered = true;
		
		List<WorldPoint> reduced = this.shape;
		while(dev > 50){
			reduced = GeoUtils.quitFarthest(reduced);
			dev=GeoUtils.deviation(reduced);
		}
		System.err.println("Normalized Standard Deviation of locations group: "+dev);
		
		WorldPoint centroid;
		try {
			centroid = GeoUtils.centroid(reduced);
			System.err.println("Centroid: "+centroid.repr());
		} catch (InvalidCoordinateRangeException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		System.err.println("-------------------------------------------");
		
	}
	
	public void setScattering(){
		float dev=GeoUtils.deviation(this.shape);
		if(dev > 1000) this.scattered = true;
	}
	
	public boolean isScattered(){
		return this.scattered;
	}
	
	/**
	 * adds a first class location to the group
	 * @param s
	 */
	public void addFCelem(String s){
		this.fcSet.add(s);
	}
	
	/**
	 * adds a second class location (containing entity) to the group
	 * @param s
	 */
	public void addSCelem(String s){
		this.scSet.add(s);
	}
	
	/**
	 * returns a stringvector containing all the first class elements in the group
	 * @return
	 */
	public Vector<String> getFCelems(){
		Vector<String> firstClass= new Vector<String>();
		Iterator itr= fcSet.iterator();
		while(itr.hasNext()){
			firstClass.addElement((String)itr.next());
		}
		return firstClass;
	}
	
	/**
	 * returns a stringvector containing all the second class elements in the group
	 * @return
	 */
	public Vector<String> getSCelems(){
		Vector<String> secondClass = new Vector<String>();
		Iterator itr= scSet.iterator();
		while(itr.hasNext()){
			secondClass.addElement((String)itr.next());
		}
		return secondClass;
	}
	
	/**
	 * returns a stringvector containing all the second class elements in the group
	 * @return
	 */
	public Vector<String> getAllelems(){
		Vector<String> res = new Vector<String>();
		res.addAll(this.getFCelems());
		res.addAll(this.getSCelems());
		return res;
	}
	
	public String getAll(){
		Vector<String> res = this.getAllelems();
		StringBuffer buf = new StringBuffer();
		for(String s : res){
			buf.append(s);
			buf.append(" ");
		}
		return buf.toString().trim();
	}
	
	public String getFC(){
		Vector<String> firstClass= this.getFCelems();
		StringBuffer buf=new StringBuffer();
		for(int i=0; i< firstClass.size(); i++){
			buf.append(firstClass.elementAt(i));
			buf.append(" ");
		}
		return buf.toString();
	}
	
	public String getSC(){
		Vector<String> secondClass= this.getSCelems();
		StringBuffer buf=new StringBuffer();
		for(int i=0; i< secondClass.size(); i++){
			buf.append(secondClass.elementAt(i));
			buf.append(" ");
		}
		return buf.toString();
	}
	/**
	 * returns a space-separated list of WorldPoint coordinates (format: "lat:lon")
	 * @return
	 */
	public String getGeoCoordinates(){
		StringBuffer buf= new StringBuffer();
		if(this.shape.size() > 0){
			Iterator<WorldPoint> itr = shape.iterator();
			while(itr.hasNext()){
				WorldPoint p = itr.next();
				buf.append(p.indexRepr()+" ");
			}
		} else {
			return "";
		}
		return buf.toString().trim();
	}
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append("1st class:\n");
		buf.append(getFC());
		buf.append("\n2nd class:\n");
		buf.append(getSC());
		buf.append("\ncoordinates:\n");
		buf.append(getGeoCoordinates()+"\n");
		return buf.toString();
	}
}
