package es.upv.dsic.GeoNames;

public class GeoNamesLocation {
	private String id;
	private String name;
	private String region;
	private String country;
	private String lat;
	private String lon;
	private long population;
	
	public GeoNamesLocation(String par1, String par2, String par3, String coordsPostGIS, String par4, String country){
		this.id=par1;
		this.name=par2;
		
		this.region=par3;
		if(this.region==null) this.region="";
		
		int startCI=coordsPostGIS.indexOf("(")+1;
		int endCI=coordsPostGIS.lastIndexOf(")");
		String coords = coordsPostGIS.substring(startCI, endCI);
		String[] lonlat = coords.split(" ");
		this.lon=lonlat[0];
		this.lat=lonlat[1];
		
		this.population=Long.parseLong(par4)+1;
		this.country=country;
	}
	
	public String getID(){
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getLat(){
		return this.lat;
	}
	
	public String getLon(){
		return this.lon;
	}
	
	public String getRegion(){
		return this.region;
	}
	
	public String getCountry(){
		return this.country;
	}
	
	public String getCoordsAsText(){
		return "lat:"+this.lat+" lon:"+this.lon;
	}
	
	public String toString(){
		return (this.id+": "+this.name+" ("+this.region+", "+this.country+") @ "+this.getCoordsAsText()+" pop. "+this.population);
	}
	
	public long getPopulation(){
		return this.population;
	}
}
