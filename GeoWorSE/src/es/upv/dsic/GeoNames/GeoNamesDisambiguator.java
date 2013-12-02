package es.upv.dsic.GeoNames;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Vector;

public class GeoNamesDisambiguator {
	private static String url = "jdbc:postgresql://localhost:5432/geonames"; //in Protein
	//private static String url = "jdbc:postgresql://localhost:5432/geo";//in beryllium
	private static Connection dbConn;
	private static boolean VERBOSE=false;
	
/*	public GeoNamesDisambiguator(){
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
*/	
	public static void init(){
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
	
	public static Vector<GeoNamesLocation> getReferents(String placename){
		/*
		SELECT locations.geonameid, locations.region, ASTEXT(locations.coordinates), population from locations where name = 'Italy'
		UNION
		SELECT locations.geonameid, locations.region, ASTEXT(locations.coordinates), locations.population from alternatename join locations on alternatename.geonameid=locations.geonameid where alternatename = 'Italy' and isolanguage='en';
		*/
		Vector<GeoNamesLocation> ret = new Vector<GeoNamesLocation>();
		Statement st;
		String placenameSQL = placename.replaceAll("'", "''");
		if(VERBOSE) System.err.println("Getting referents for "+placename+"...");
		long t1 = System.currentTimeMillis();
		try {
			st = dbConn.createStatement();
		
			//st.executeQuery("SELECT locations.geonameid, locations.region, ASTEXT(locations.coordinates), population from locations where name = '"+placenameSQL+"'" +
			//		"UNION SELECT locations.geonameid, locations.region, ASTEXT(locations.coordinates), locations.population from alternatename join locations on alternatename.geonameid=locations.geonameid where alternatename = '"+placenameSQL+"' and isolanguage='en';");
			st.executeQuery("SELECT geoname.geonameid, geoname.region, st_astext(geoname.latlon_point), geoname.population, countryinfo.name from geoname LEFT JOIN countryinfo on geoname.country=countryinfo.iso_alpha2 where geoname.name = '"+placenameSQL+"'" +
					"UNION SELECT geoname.geonameid, geoname.region, st_astext(geoname.latlon_point), geoname.population, countryinfo.name from alternatename LEFT JOIN geoname on alternatename.geonameid=geoname.geonameid JOIN countryinfo on geoname.country=countryinfo.iso_alpha2 where alternatename = '"+placenameSQL+"' and isolanguage='en';");
			ResultSet r = st.getResultSet();
			while(r.next()){
				GeoNamesLocation l = new GeoNamesLocation(r.getString(1), placename, r.getString(2), r.getString(3), r.getString(4), r.getString(5));
				ret.addElement(l);
			}
			r.close();
			st.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(VERBOSE) System.err.println("done. "+(System.currentTimeMillis()-t1)+" ms elapsed.");
		
		return ret;
	}
	
	/**
	 * returns a location from an ID
	 * @param id
	 * @return
	 */
	public static GeoNamesLocation makeLocationFromID(String id){
		Statement st;
		GeoNamesLocation l=null;
		if(VERBOSE) System.err.println("Making location from ID "+id+"...");
		long t1 = System.currentTimeMillis();
		try {
			st = dbConn.createStatement();
			//st.executeQuery("SELECT locations.geonameid, locations.name, locations.region, ASTEXT(locations.coordinates), population from locations where geonameid = '"+id+"' ;");
			st.executeQuery("SELECT geoname.geonameid, geoname.name, geoname.region, st_astext(latlon_point), geoname.population, countryinfo.name from geoname LEFT JOIN countryinfo on geoname.country=countryinfo.iso_alpha2 where geoname.geonameid = '"+id+"' ;");
			ResultSet r = st.getResultSet();
			while(r.next()){
				l = new GeoNamesLocation(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6));
				break;
			}
			r.close();
			st.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(VERBOSE) System.err.println("done. "+(System.currentTimeMillis()-t1)+" ms elapsed.");
		
		return l; //Wrong ID passed?
		//TODO: invece di return null creare eccezione
	}
	
	public static Vector<String> getAlternateNames(String id){
		Statement st;
		Vector<String> ret=new Vector<String>();
		if(VERBOSE) System.err.println("Getting synonyms for "+id+"...");
		long t1 = System.currentTimeMillis();
		try {
			st = dbConn.createStatement();
			//FIXME: this works only for English, we should render it independent from language
			//st.executeQuery("SELECT alternatename from alternatename where geonameid = '"+id+"' ;");
			st.executeQuery("SELECT alternatename from alternatename where isolanguage='en' and geonameid = '"+id+"' ;");
			ResultSet r = st.getResultSet();
			while(r.next()){
				ret.addElement(r.getString(1));
			}
			r.close();
			st.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(VERBOSE) System.err.println("done. "+(System.currentTimeMillis()-t1)+" ms elapsed.");
		
		return ret;
	}
	
	public static Vector<String> getParents(String id){
		Statement st;
		Vector<String> ret=new Vector<String>();
		if(VERBOSE) System.err.println("Getting parents for "+id+"...");
		long t1 = System.currentTimeMillis();
		try {
			st = dbConn.createStatement();
		
			st.executeQuery("WITH RECURSIVE breadcrumb(son, father) AS ( " +
					"SELECT son, father FROM hierarchy WHERE son = '"+id+"' " +
							"UNION SELECT hierarchy.son, hierarchy.father FROM hierarchy, breadcrumb " +
							"WHERE breadcrumb.father = hierarchy.son) " +
							"SELECT DISTINCT son, geoname.name FROM breadcrumb JOIN geoname ON breadcrumb.son=geoname.geonameid; ");
			ResultSet r = st.getResultSet();
			while(r.next()){
				ret.addElement(r.getString(2));
			}
			r.close();
			st.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(VERBOSE) System.err.println("done. "+(System.currentTimeMillis()-t1)+" ms elapsed.");
		
		return ret;
	}
	
	public static String getContinent(String country){
		Statement st;
		String ret="";
		if(VERBOSE) System.err.println("Getting continent for "+country+"...");
		try {
			st = dbConn.createStatement();
		
			st.executeQuery("SELECT geoname.name from COUNTRYINFO JOIN hierarchy on countryinfo.geonameid=hierarchy.son JOIN geoname on hierarchy.father=geoname.geonameid where countryinfo.name='"+country+"'; " );
			ResultSet r = st.getResultSet();
			while(r.next()) {
				ret=r.getString(1);
			}
			r.close();
			st.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
}
