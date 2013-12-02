package es.upv.dsic.geoclef.geography;

/**
 * Class that represents a point on the Earth surface
 * @author buscaldi
 *
 */
public class WorldPoint {
	protected float X;
	protected float Y;
	
	/**
	 * Constructor that uses float values for coordinates
	 * @param lat
	 * @param lon
	 * @throws InvalidCoordinateRangeException
	 */
	public WorldPoint(float lat, float lon) throws InvalidCoordinateRangeException{
		if(Math.abs(lat) > 90) throw new InvalidCoordinateRangeException("Latitude 90 exceeded: "+lat);
		if(Math.abs(lon) > 180) throw new InvalidCoordinateRangeException("Longitude 180 exceeded: "+lon);
		this.Y=lat;
		this.X=lon;
	}
	
	/**
	 * Constructor that uses Strings as coordinates (it converts them to float values)
	 * @param lat
	 * @param lon
	 * @throws InvalidCoordinateRangeException
	 */
	public WorldPoint(String lat, String lon) throws InvalidCoordinateRangeException {
		this(Float.parseFloat(lat), Float.parseFloat(lon));
	}
	
	/**
	 * Returns the euclidean distance in decimal degrees (flatland distance)
	 * This is not the real distance between two points
	 * @param p
	 * @return
	 */
	public float euclideanDistance(WorldPoint p){
		double d=0;
		double xdiff=p.X-this.X;
		double ydiff=p.Y-this.Y;
		
		d=Math.sqrt(Math.pow(xdiff, 2)+Math.pow(ydiff, 2));
		
		return (float)d;
	}
	
	/**
	 * Returns the spherical distance (in Kms.) of this point from point p
	 * This is the actual distance on Earth's surface
	 * @param p
	 * @return
	 */
	public float kilometricDistance(WorldPoint p){
		double R = 6371; // Earth radius
		//we need to convert everything to radians
		double lambdaP=p.X*Math.PI/180;
		double lambdaS=this.X*Math.PI/180;
		double phiP=p.Y*Math.PI/180;
		double phiS=this.Y*Math.PI/180;
		
		double lonD = lambdaS-lambdaP;
		double d = Math.acos(Math.sin(phiS)*Math.sin(phiP) + 
                Math.cos(phiS)*Math.cos(phiP) *
                Math.cos(lonD)) * R;
		return (float)d;
	}
	
	public boolean equals(Object p){
		return(this.X==((WorldPoint)p).X && this.Y==((WorldPoint)p).Y);
	}
	
	/**
	 * Returns a string representation of the point in the format X:lon , Y:lat
	 * @return
	 */
	public String repr() {
		return "X:"+this.X+" , Y:"+this.Y;
	}
	
	/**
	 * Returns a string representation of the point in the format (N|S)lat (E|W)lon
	 * @return
	 */
	public String canonicRepr(){
		String lat;
		if(this.Y >0){
			lat="N";
		} else lat = "S";
		String lon;
		if(this.X > 0){
			lon="E";
		} else lon ="W";
		
		return this.Y+lat+" "+this.X+lon;
		
	}

	public String indexRepr() {
		return this.Y+":"+this.X;
	}

	public float yCoord() {
		return this.Y;
	}

	public float xCoord() {
		return this.X;
	}
}
