package es.upv.dsic.geoclef.indexing.LGL;

public class LGLToponym {
	private String id;
	private String name;
	private String lat;
	private String lon;
	private String country;
	private String admin;
	
	public LGLToponym(String id, String name, String lat, String lon, String country, String admin){
		this.id=id;
		this.name=name;
		this.lat=lat;
		this.lon=lon;
		this.country=country;
		this.admin=admin;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getCountry(){
		return this.country;
	}
	
	public String getAdmin(){
		return this.admin;
	}
	
	public boolean equals(LGLToponym t){
		return this.id==t.id;
	}
}
