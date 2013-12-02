package es.upv.dsic.geoclef.geography;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

class PoinTan implements Comparable<PoinTan> {
	public WorldPoint p;
	public Double tan;
	
	public PoinTan(WorldPoint p, Double tan){
		this.p=p;
		this.tan=tan;
	}

	public int compareTo(PoinTan pt) {
		return tan.compareTo(pt.tan);
	}
	
}

/**
 * class that provides some useful geometric methods
 * @author buscaldi
 *
 */
public final class GeoUtils {

	/**
	 * calculates the Std. Deviation of the given list of points
	 * @param points
	 * @return
	 */
	public static float deviation(List<WorldPoint> points){
		int nElems = points.size();
		if (nElems <= 1) return 0;
		Vector<Float> distances=new Vector<Float>();
		WorldPoint centroid;
		try {
			centroid = centroid(points);
		
			Iterator<WorldPoint> i = points.iterator();
			double sum=0;
			while(i.hasNext()){
				WorldPoint p = i.next();
				double d=Math.pow(p.euclideanDistance(centroid),2);
				distances.addElement(new Float(d));
				sum+=d;
			}
		
			double sigma=Math.sqrt((sum/(double)nElems));
			return (float)sigma;
		
		} catch (InvalidCoordinateRangeException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * calculates the centroid of the given list of points
	 * @param points
	 * @return
	 * @throws InvalidCoordinateRangeException 
	 */
	public static WorldPoint centroid(List<WorldPoint> points) throws InvalidCoordinateRangeException{
		float sX=0;
		float sY=0;
		int nElems=points.size();
		
		for(WorldPoint p : points){
			sX+=p.X;
			sY+=p.Y;
		}
		float cX=sX/(float)nElems;
		float cY=sY/(float)nElems;
		
		return(new WorldPoint(cY, cX)); //ricordarsi: Y=lat, X=lon
	}
	
	private static float crossProd(WorldPoint p1, WorldPoint p2, WorldPoint p3){
		return (p2.X - p1.X)*(p3.Y - p1.Y) - (p3.X - p1.X)*(p2.Y - p1.Y);
	}
	
	/**
	 * returns a list of points corresponding to the Convex Hull of the given list
	 * @param points
	 * @return
	 */
	public static List<WorldPoint> convexHull(List<WorldPoint> points){
		//algoritmo di Graham
		//find pivot (point with lowest Y coord)
		if(points.size() < 3) return points; //no hull
		Iterator<WorldPoint> itr = points.iterator();
		WorldPoint pivot=itr.next();
		while(itr.hasNext()){
			WorldPoint curr=itr.next();
			if(curr.Y == pivot.Y && !pivot.equals(curr)){
				if(curr.X < pivot.X){
					pivot=curr;
				}
			}
			if(curr.Y < pivot.Y) pivot=curr;
		}
		//calculate the angle of each point with respect to Y and sort points 
		List<PoinTan> ptans= new Vector<PoinTan>();
		itr=points.iterator();
		while(itr.hasNext()){
			WorldPoint curr=itr.next();
			if(!curr.equals(pivot)) {
				Double cos = new Double((curr.X-pivot.X)/pivot.euclideanDistance(curr));
				Double theta = Math.acos(cos);
				ptans.add(new PoinTan(curr, theta));
			}
		}
		Collections.sort(ptans);
		/*
		System.err.println("Points ranked by angle:");
		System.err.println(pivot.repr());
		for(int i=0; i<ptans.size(); i++){
			WorldPoint p=ptans.get(i).p;
			System.err.println(p.repr()+ "tan: "+ptans.get(i).tan);
		}
		System.err.println("-----end of ptans----");
		*/
		Vector<WorldPoint> s= new Vector<WorldPoint>();
		s.addElement(pivot);
		s.addElement(ptans.get(0).p);
		
		for(int i=1; i<ptans.size(); i++){
			WorldPoint p1=s.elementAt(s.size()-2);
			WorldPoint p2=s.lastElement();
			WorldPoint p3=ptans.get(i).p;
			//System.err.println("crossprod of: "+p1.repr()+" , "+p2.repr()+" , "+p3.repr()+" : "+crossProd(p1, p2, p3));
			while(s.size() > 2 && crossProd(p1, p2, p3) <= 0){
				s.removeElementAt(s.size()-1);
				//System.err.println("cancello "+p2.repr());
				//aggiorno p1 e p2
				if(s.size() > 2) {
					p1=s.elementAt(s.size()-2);
					p2=s.lastElement();
				}
				//System.err.println("size: "+s.size());
				//System.err.println("inWhile: crossprod of: "+p1.repr()+" , "+p2.repr()+" , "+p3.repr()+" : "+crossProd(p1, p2, p3));
				
			}
			s.addElement(p3);
			//System.err.println("size now: "+s.size());
		}
		/*
		System.err.println("Result of CH algorithm: (size "+s.size()+ " )");
		for(int i=0; i<s.size(); i++){
			WorldPoint p=s.elementAt(i);
			System.err.println(p.repr());
		}
		System.err.println("-----end of CH----");
		*/
		return s;
	}
	
	/**
	 * returns a list of points where the farthest one has been removed
	 * @param points
	 * @return
	 */
	public static List<WorldPoint> quitFarthest(List<WorldPoint> points){
		int nElems = points.size();
		Vector<Float> distances=new Vector<Float>();
		for(int i=0; i< nElems; i++){
			WorldPoint p = points.get(i);
			float sum=0;
			for(int j=0; j < nElems; j++ ){
				if(j!=i){
					sum+=p.euclideanDistance(points.get(j));
				}
			}
			distances.addElement(new Float(sum/(float)nElems-1));
			//System.err.println("Avg. distance of point "+p.canonicRepr()+" from other points: "+(Float)distances.lastElement());
		}
		float max=0;
		int argMax=-1;
		for(int i=0; i< distances.size(); i++){
			Float f = (Float)distances.elementAt(i);
			if(f.floatValue()> max){
				argMax=i;
				max=f.floatValue();
			}
		}
		
		if(argMax > -1){
			//mastrussi per evitare l'eccezione ConcurrentModification
			//List<WorldPoint> ret = new Vector<WorldPoint>();
			//System.err.print("removing point: ");
			WorldPoint removed = points.remove(argMax); //return value thrown away
			//System.err.println(removed.canonicRepr());
		}
		return points;
	}
	
	/**
	 * point-in-polygon algorithm (Graham's scan)
	 * @param p
	 * @param points
	 * @return
	 */
	public static boolean inPolygon(WorldPoint p, List<WorldPoint> points){
		int j=points.size()-1;
		boolean oddNodes = false;
		
		for (int i=0; i<points.size(); i++) {
		    if (points.get(i).Y < p.Y && points.get(j).Y >= p.Y
		    ||  points.get(j).Y < p.Y && points.get(i).Y >= p.Y) {
		      if (points.get(i).X+(p.Y-points.get(i).Y)/(points.get(j).Y-points.get(i).Y)*(points.get(j).X-points.get(i).X)< p.X) {
		        oddNodes=!oddNodes; }
		    }
		    j=i; 
		}

		return oddNodes;
		
	}
}
