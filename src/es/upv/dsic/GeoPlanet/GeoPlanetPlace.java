package es.upv.dsic.GeoPlanet;

public class GeoPlanetPlace {
	private String woeid;
	private String name;
	private String parentWoeID;
	
	public GeoPlanetPlace(String par1, String par2, String par3){
		this.woeid=par1;
		this.name=par2;
		this.parentWoeID=par3;
	}
	
	public String getParentID(){
		return this.parentWoeID;
	}
	
	public String getID(){
		return this.woeid;
	}
	
	public String getName(){
		return this.name;
	}
	
	public boolean equals(GeoPlanetPlace p){
		return this.woeid.equals(p.woeid);
	}
}
