package es.upv.dsic.GeoNames;

import java.util.Vector;

import es.upv.dsic.geoclef.geography.InvalidCoordinateRangeException;
import es.upv.dsic.geoclef.geography.WorldPoint;

public class PlaceInstance {
	private String placename;
	private GeoNamesLocation assigned;
	private Vector<GeoNamesLocation> candidates;
	private Vector<GeoNamesLocation> context;
	private Vector<Double> contextWeights; //pop divided by number of referents
	private double [] localWeights; //one weight for each referent
	private boolean disambiguated;
	
	public PlaceInstance(String placename){
		this.placename=normalizePlaceName(placename);
		this.disambiguated=false;
		this.assigned=null;
		this.candidates= GeoNamesDisambiguator.getReferents(this.placename);
		if(candidates.size() == 1) {
			this.disambiguated=true;
			this.assigned=this.candidates.elementAt(0);
		}
		localWeights= new double [candidates.size()+1];
		for(int i=0; i< this.candidates.size(); i++){
			localWeights[i]= 0;
		}
		this.context= new Vector<GeoNamesLocation>();
		this.contextWeights = new Vector<Double> ();
	}
	
	public PlaceInstance(GeoNamesLocation disambiguatedLoc){
		this.placename=disambiguatedLoc.getName();
		this.disambiguated=true;
		this.assigned=disambiguatedLoc;
		candidates= new Vector<GeoNamesLocation>();
		candidates.add(disambiguatedLoc);
	}
	
	private String normalizePlaceName(String input){
		if(input.equalsIgnoreCase("u.s.")) return "United States";
		else if(input.equalsIgnoreCase("america")) return "United States";
		else if(input.equalsIgnoreCase("british")) return "United Kingdom of Great Britain and Northern Ireland";
		else if(input.equalsIgnoreCase("britain")) return "United Kingdom of Great Britain and Northern Ireland";
		else if(input.equalsIgnoreCase("u.k.")) return "United Kingdom of Great Britain and Northern Ireland";
		else if(input.equalsIgnoreCase("united kingdom")) return "United Kingdom of Great Britain and Northern Ireland";
		else if(input.equalsIgnoreCase("american")) return "United States";
		else {
			//capitalize input string
			String [] words = input.split("\\s");
			StringBuffer buf = new StringBuffer();
			for(String s : words){
				if(s.length()==0) break;
				buf.append(s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());
				buf.append(" ");
			}
			return (buf.toString()).trim();
		}
	}
	/**
	 * add all places for a given placename
	 * @param placename
	 
	public void addAmbiguousContext(String placename){
		Vector <GeoNamesLocation> tmp = GeoNamesDisambiguator.getReferents(placename);
		for(GeoNamesLocation l : tmp){
			context.add(l);
			contextWeights.add(new Double((float)l.getPopulation()/(float)tmp.size()));
		}
	}*/
	
	public void addAmbiguousContext(PlaceInstance instance){
		int size=instance.getCandidates().size();
		for(GeoNamesLocation l : instance.getCandidates()){
			context.add(l);
			contextWeights.add(new Double((float)l.getPopulation()/(float)size));
		}
	}
	
	public Vector<GeoNamesLocation> getCandidates() {
		return this.candidates;
	}
	
	public int getNumberOfReferents(){
		return this.candidates.size();
	}

	/**
	 * add a disambiguated place
	 * @param place
	 */
	public void addContext(GeoNamesLocation place){
		context.add(place);
		contextWeights.add(new Double(place.getPopulation()));
	}
	
	public boolean isDisambiguated(){
		return this.disambiguated;
	}
	/**
	 * disambiguation method (uses the "Adige" method with variant based on population instead than frequency)
	 * @return
	 */
	public GeoNamesLocation disambiguate(){
		int cnt=0;
		for(GeoNamesLocation ge : candidates){
			WorldPoint source;
			try {
				source = new WorldPoint(ge.getLat(), ge.getLon());
				for(int i=0; i < context.size(); i++){
					GeoNamesLocation c = context.elementAt(i);
					WorldPoint target = new WorldPoint(c.getLat(), c.getLon());
					double distance = source.kilometricDistance(target);
					double weight = contextWeights.elementAt(i) * (1/ Math.pow((distance+1), 2)); //no Text distance
					localWeights[cnt]+=weight; //SUM of weights in context
				}
				
			} catch (InvalidCoordinateRangeException e) {
				e.printStackTrace();
				localWeights[cnt]=0;
			}
			cnt++;
		}
		
		//arg max over weights
		double tmpMax=0;
		int argMax=0;
		boolean ambiguous=true;
		for(int i=0; i < localWeights.length; i++){
			if(localWeights[i] == Double.NaN) localWeights[i]=0; //cMass = 0 ?
			if(localWeights[i] > tmpMax){
				tmpMax = localWeights[i];
				argMax=i;
				ambiguous=false;
			}else if(localWeights[i] == tmpMax){
				ambiguous=true;
			}
		}
		if(!ambiguous){
			this.assigned=candidates.elementAt(argMax);
			this.disambiguated=true;
			return candidates.elementAt(argMax);
		} else {
			return null;
		}
	}

	public GeoNamesLocation getPlace() {
		if(disambiguated) return this.assigned;
		else return null;
	}
	
	public String getName(){
		return this.placename;
	}
}
