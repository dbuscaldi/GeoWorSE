package es.upv.dsic.GeoPlanet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Vector;

public class GeoPlanetDisambiguator {
	String url = "jdbc:postgresql://localhost:5432/geoplanet";
	Connection dbConn;
	
	public GeoPlanetDisambiguator(){
		Properties props = new Properties();
		props.setProperty("user","davide");
		props.setProperty("password","legend73");
		try {
    		Class.forName("org.postgresql.Driver");
    		dbConn=DriverManager.getConnection(url, props);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * checks whether the placename exists in GeoPlanet or not
	 * @param placename
	 * @return
	 */
	public int countSenses(String placename){
		int nsenses=0;
		Statement st;
		try {
			st = dbConn.createStatement();
		
			st.executeQuery("select woeid from places where name = '"+placename+"';");
			ResultSet r = st.getResultSet();
			while(r.next()){
				nsenses++;
			}
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return nsenses;
	}
	/**
	 * returns a set of GeoPlanetPlaces that could refer to the named place
	 * @param placename
	 * @return
	 */
	public Vector<GeoPlanetPlace> getReferents(String placename){
		Vector<GeoPlanetPlace> ret = new Vector<GeoPlanetPlace>();
		Statement st;
		try {
			st = dbConn.createStatement();
		
			st.executeQuery("select woeid, name, parent from places where name = '"+placename+"';");
			ResultSet r = st.getResultSet();
			while(r.next()){
				GeoPlanetPlace p = new GeoPlanetPlace(r.getString(1), r.getString(2), r.getString(3));
				ret.addElement(p);
			}
			st.close();
			
			Statement st2 = dbConn.createStatement();
			
			st2.executeQuery("select distinct places.woeid, places.name, places.parent from aliases join places on aliases.woeid=places.woeid where aliases.name = '"+placename+"';");
			ResultSet r2 = st2.getResultSet();
			
			while(r2.next()){
				ret.addElement(new GeoPlanetPlace(r2.getString(1), r2.getString(2), r2.getString(3)));
			}
			
			st2.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 * get the path of containing entities
	 * @param woeid
	 * @param mode
	 * @return
	 */
	public Vector<GeoPlanetPlace> getPath(GeoPlanetPlace p){
		Vector<GeoPlanetPlace> ret = new Vector <GeoPlanetPlace>();
		if(p.getID().equals("1")) return ret;
		try{
			Statement st = dbConn.createStatement();
			st.executeQuery("select woeid, name, parent from places where woeid='"+p.getParentID()+"'");
			ResultSet r = st.getResultSet();
			while(r.next()){
				GeoPlanetPlace p_new = new GeoPlanetPlace(r.getString(1), r.getString(2), r.getString(3));
				ret.addElement(p_new);
				ret.addAll(getPath(p_new));
			}
		} catch (SQLException e){
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 * disambiguate place with context
	 * @param name
	 * @param context
	 * @return vector of most probable referents (if possible: 1 referent only)
	 */
	public Vector<GeoPlanetPlace> disambiguate(Vector<String> placenames){
		for(String s : placenames){
			
		}
	}
	
}
