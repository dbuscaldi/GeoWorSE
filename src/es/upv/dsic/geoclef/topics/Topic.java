/*
 * Created on 8-giu-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package es.upv.dsic.geoclef.topics;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Vector;

import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import es.upv.dsic.geoWordNet.GeoWNException;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.geography.GeoUtils;
import es.upv.dsic.geoclef.geography.WorldPoint;
import es.upv.dsic.geoclef.search.GeoSearch;
import es.upv.dsic.geoclef.visual.MapPanel;
import es.upv.dsic.geoclef.visual.WindowUtilities;
import es.upv.dsic.tools.FrequencyFilter;

/**
 * @author davide
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Topic {
	private String id;
	private String title;
	private String desc;
	private String concept;
	private String narr;
	private Vector<String> locations;
	private HashSet<String> meronyms;
	private boolean area; //true if this topic needs documents included in some area, false if the topics have to be in range within certain distance
	private List<WorldPoint> shapeArea; //only if area is true
	private WorldPoint centroid; //only if area is false
	private boolean hasGeoRef=false;
	
	public Topic(){
		locations = new Vector<String>();
		meronyms = new HashSet<String>();
	}	
	
	/**
	 * @param string
	 */
	public void setID(String string) {
		this.id=string;
	}
	/**
	 * @param string
	 */
	public void setTitle(String string) {
		this.title=string;
	}
	/**
	 * @param string
	 */
	public void setDesc(String string) {
		this.desc=string;
		
	}
	/**
	 * @param string
	 */
	public void setConcept(String string) {
		this.concept=string;
	}
	/**
	 * @param string
	 */
	public void addLocation(String string) {
		this.locations.addElement(string);
	}
	
	public void addLocations(Vector<String> locs){
		this.locations=locs;
	}
	/**
	 * returns true if this topic has been assigned geographical scope
	 * @return
	 */
	public boolean isGeoReferenced(){
		return this.hasGeoRef;
	}
	
	/**
	 * returns true if this topic has been assigned a point scope,
	 * false if it has been assigned an area scope
	 * @return
	 */
	public boolean isPointReference(){
		return !this.area;
	}
	
	public void printGeoReference(){
		if(!hasGeoRef) return;
		if(area) {
			System.out.println("Area:");
			Iterator<WorldPoint> itr=this.shapeArea.iterator();
			while(itr.hasNext()){
				System.out.println(itr.next().repr());
			}
		} else {
			System.out.println("Centroid:");
			System.out.println(this.centroid.repr());
		}
	}
	
	public void setGeographicalData() throws Exception{
		// step 1: set shape
		for(int i=0; i< this.locations.size(); i++){
			String item = this.locations.elementAt(i);
			IIndexWord idxWord = GeoWorSE.dict.getIndexWord((item.trim()).replace(' ', '_'), POS.NOUN);
			if(idxWord != null){
				IWordID wordID=idxWord.getWordIDs().get(0);
				IWord word = GeoWorSE.dict.getWord(wordID);
				ISynset wsyn=word.getSynset();
					  
				//geographical coordinates
				try{
					WorldPoint p = GeoWorSE.geoWNdict.getWP(wsyn.getOffset());
					//System.err.println("location: "+word.getLemma()+", coord: "+p.repr());
					if(this.shapeArea == null){
						this.shapeArea= new Vector<WorldPoint>();
					}
					if(!shapeArea.contains(p)) this.shapeArea.add(p);
					
					//aggiungiamo anche le parti componenti
					List<ISynsetID> geoRefMeros= getSubLocations(wsyn);
					if(geoRefMeros.size() > 0){
						if(this.shapeArea == null){
							this.shapeArea= new Vector<WorldPoint>();
						}
						List<WorldPoint> pset = new Vector<WorldPoint>();
						for(Iterator<ISynsetID> itr=geoRefMeros.listIterator(); itr.hasNext();){
							ISynsetID curr = itr.next();
							try{
								WorldPoint curr_wp=GeoWorSE.geoWNdict.getWP(curr.getOffset());
								if(!shapeArea.contains(curr_wp) && !pset.contains(curr_wp))
									pset.add(curr_wp);
								
								//add meronyms...
								ISynset syn = GeoWorSE.dict.getSynset(curr);
								//check if they appear in collection !!!
								String s_str=syn.getWord(1).getLemma().replace('_', ' ');
								if(!meronyms.contains(s_str)){
									double t_gain = FrequencyFilter.calcPhraseGain(s_str, "text");
									double g_gain = FrequencyFilter.calcPhraseGain(s_str, "geo");
									if(t_gain > 0 || g_gain > 0) this.meronyms.add(s_str);
								}
								//System.err.println("adding included location: "+(syn.getWord(1)).getLemma()+", coord: "+curr_wp.repr());
								
							} catch(GeoWNException e){}
						}
						this.shapeArea.addAll(pset);
					}
				} catch (GeoWNException gwne){
					//maybe synset is an area synset:
					//look for meronyms and generate a shape
					List<ISynsetID> geoRefMeros= getSubLocations(wsyn);
					if(geoRefMeros.size() > 0){
						if(this.shapeArea == null){
							this.shapeArea= new Vector<WorldPoint>();
						}
						List<WorldPoint> pset = new Vector<WorldPoint>();
						for(Iterator<ISynsetID> itr=geoRefMeros.listIterator(); itr.hasNext();){
							ISynsetID curr = itr.next();
							try{
								WorldPoint curr_wp=GeoWorSE.geoWNdict.getWP(curr.getOffset());
								if(!shapeArea.contains(curr_wp) && !pset.contains(curr_wp))
									pset.add(curr_wp);
								
								//add meronyms...
								ISynset syn = GeoWorSE.dict.getSynset(curr);
								//check if they appear in collection !!
								String s_str=syn.getWord(1).getLemma().replace('_', ' ');
								if(!meronyms.contains(s_str)){
									double t_gain = FrequencyFilter.calcPhraseGain(s_str, "text");
									double g_gain = FrequencyFilter.calcPhraseGain(s_str, "geo");
									if(t_gain > 0 || g_gain > 0) this.meronyms.add(s_str);
								}
								
								//ISynset syn = GeoWorSE.dict.getSynset(curr);
								//System.err.println("adding area location for "+word.getLemma()+" : "+(syn.getWord(1)).getLemma()+", coord: "+curr_wp.repr());
							} catch(GeoWNException e){}
						}
						this.shapeArea.addAll(pset);
					}
				}
			} 		   
		}
		//step 2: calculate area and/or point
		if(this.shapeArea != null){
		
			if(this.shapeArea.size() > 2){
				//abbiamo un'area!
				/*Iterator<WorldPoint> itr = this.shapeArea.iterator();
				while(itr.hasNext()){
					System.err.println(itr.next().repr());
				}*/
				if(GeoSearch.MAP_ACTIVE) WindowUtilities.openInJFrame(new MapPanel(this.shapeArea), 380, 400);
				
				//System.err.println("len: shape before ch: "+this.shapeArea.size());
				float deviation=GeoUtils.deviation(this.shapeArea);
				//System.err.println("current dev: "+deviation);
				
				//Riduzione dell'area per prevenire errori
				double scale = Math.log10(deviation);
				if(scale > 2){
					List<WorldPoint> tempArea= GeoUtils.quitFarthest(this.shapeArea);
					float new_deviation=GeoUtils.deviation(tempArea);
					double diff=deviation-new_deviation;
					double eps= Math.pow(10, (scale-1));
					//System.err.println("eps: "+eps+" scale: "+scale+" diff: "+diff);
					while(diff > eps && tempArea.size() > 2) {
						//attempt to quit noise by removing farthest points
						tempArea=GeoUtils.quitFarthest(tempArea);
						new_deviation=GeoUtils.deviation(tempArea);
						diff=deviation-new_deviation;
						deviation=new_deviation;
						//System.err.println("current dev: "+new_deviation+" diff: "+diff);
					}
				
					//System.err.println("len: shape after farthest points removal: "+this.shapeArea.size());
				}
				this.shapeArea=GeoUtils.convexHull(this.shapeArea);
				//System.err.println("len: shape after ch: "+this.shapeArea.size());
				if(GeoSearch.MAP_ACTIVE) WindowUtilities.drawOnLastFrame(this.shapeArea);
				
				this.area=true;
				this.hasGeoRef=true;
				//System.err.println("std.dev: "+GeoUtils.deviation(this.shapeArea));
				
			} else {
				//è un punto
				this.centroid=GeoUtils.centroid(this.shapeArea);
				this.area=false;
				this.hasGeoRef=true;
				if(GeoSearch.MAP_ACTIVE) WindowUtilities.openInJFrame(new MapPanel(this.shapeArea), 380, 400);
				
			}
			if(GeoSearch.MAP_ACTIVE){
				try {
					//block for allowing to look at the maps
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				WindowUtilities.closeAllFrames();
			}
		} else {
			this.hasGeoRef=false;
			this.area=false;
		}
		
	}
	
	protected List<ISynsetID> getSubLocations(ISynset syn) {
		 List<ISynsetID> meros = syn.getRelatedSynsets(Pointer.MERONYM_PART);
		 List<ISynsetID> ret = new Vector<ISynsetID>();
		 
		 for(ISynsetID meronymID : meros){
			 if(GeoWorSE.geoWNdict.contains(meronymID.getOffset())) ret.add(meronymID);
			 /* here starts the recursive part */
			 List<ISynsetID> subsubLocations = getSubLocations(GeoWorSE.dict.getSynset(meronymID));
			 for(ISynsetID submeronymID : subsubLocations){
				 if(GeoWorSE.geoWNdict.contains(submeronymID.getOffset())) ret.add(submeronymID);
			 }
		 }
		 
		 return ret;
 }
	
	public void print(){
		System.out.println("Topic id:"+ this.id +"\n"+
				"desc: "+this.desc +"\n"+
				"concept: "+ this.concept +"\n"+
				"title:"+this.title+"\n"+
				"locations:");
		for(int i=0; i< locations.size(); i++){
			System.out.println(locations.elementAt(i));
		}
	}
	
	/**
	 * @return
	 */
	public String getID() {
		return this.id;
	}
	
	/**
	 * nuova versione con parte WordNet obbligatoria (ignora la parte geo)
	 * e calcolo del boost in funzione del numero di parti geografiche della query
	 * (introdotto per compensare liste lunghe di nomi geografici)
	 * @param part
	 * @return
	 * @throws IOException
	 */
	public String getSearchString(int mode) throws IOException{
		BufferedReader cwReader = new BufferedReader(new InputStreamReader(new FileInputStream("common_words")));
		HashSet<String> commonWords= new HashSet<String>();
		String w=cwReader.readLine();
		while(w!=null){
			commonWords.add(w);
			w=cwReader.readLine();
		}
		cwReader.close();
		
		StringBuffer searchStr=new StringBuffer();
		
		StringTokenizer st=new StringTokenizer(this.title);
		HashSet<String> plainBOW = new HashSet<String>();
		
		while(st.hasMoreTokens()){
			String tok=st.nextToken();
			if(!commonWords.contains(tok)) plainBOW.add(tok);
		}
		
		if(mode >= GeoSearch.TITLE_DESC){
			st=new StringTokenizer(this.desc);
				
			while(st.hasMoreTokens()){
				String tok=st.nextToken();
				if(!commonWords.contains(tok)) plainBOW.add(tok);
			}
		
			if(mode == GeoSearch.ALL){
				//ALL mode
				String[] splittedNarr=queryToBoW(this.narr);
				for(int k=0; k < splittedNarr.length; k++){
					if(!commonWords.contains(splittedNarr[k].trim())) plainBOW.add(splittedNarr[k].trim());
				}
			}
		}
		for(int i=0; i < this.locations.size(); i++){
			String loc=this.locations.elementAt(i);
			if(plainBOW.contains(loc)){
				plainBOW.remove(loc);
			}
			if(locations.size() == 1) searchStr.append("+wn:\"");
			else searchStr.append("wn:\"");
			searchStr.append(this.locations.elementAt(i));
			if(locations.size()>1){
				searchStr.append("\"^0.25 ");
			} else searchStr.append("\" ");
		}
		
		for(String s : plainBOW){
			searchStr.append("text:\"");
			searchStr.append(s);
			searchStr.append("\" ");
		}
		
		return searchStr.toString().trim();
	
	}
	
	/**
	 * Genera una stringa di ricerca geografica (per una ricerca composta: campi "text" + campi "geo")
	 * geoBoost: boost factor per le locations
	 * @throws IOException
	 * 
	 */
	public String getGeoSStr(int mode, double geoBoost) throws IOException {
		BufferedReader cwReader = new BufferedReader(new InputStreamReader(new FileInputStream("common_words")));
		Hashtable<String, String> commonWords= new Hashtable<String, String>();
		String w=cwReader.readLine();
		while(w!=null){
			commonWords.put(w, w);
			w=cwReader.readLine();
		}
		cwReader.close();
		
		StringBuffer searchStr=new StringBuffer();
			
		StringTokenizer st=new StringTokenizer(this.title);
		while(st.hasMoreTokens()){
			String tok=st.nextToken();
			if(commonWords.get(tok)==null){
				searchStr.append("\"");
				searchStr.append(tok);
				searchStr.append("\" ");
			}
		}
		
		if(mode >= GeoSearch.TITLE_DESC){
			st=new StringTokenizer(this.desc);
				
			while(st.hasMoreTokens()){
				String tok=st.nextToken();
				if(commonWords.get(tok)==null){
					searchStr.append("\"");
					searchStr.append(tok);
					searchStr.append("\" ");
				}
			}
		
			//System.out.println(searchStr);
			if(mode == GeoSearch.ALL) {
				String[] splittedNarr=queryToBoW(this.narr);
				for(int k=0; k < splittedNarr.length; k++){
					searchStr.append("text:\"");
					searchStr.append(splittedNarr[k].trim());
					searchStr.append("\" ");
				}
			}
		}
		for(int i=0; i < this.locations.size(); i++){
			searchStr.append("geo:\"");
			searchStr.append(this.locations.elementAt(i));
			searchStr.append("\"^"+geoBoost+" ");
		}
		
		
		return searchStr.toString().trim();
		
	}
	
	/**
	 * Genera una stringa di ricerca geografica (per una ricerca composta: campi "text" + campi "geo")
	 * geoBoost: boost factor per le locations
	 * WNBoost: boost factor per termini estratti da wordnet
	 * @throws IOException
	 * 
	 */
	public String getWNGeoSStr(int mode, double geoBoost, double WNBoost) throws IOException {
		BufferedReader cwReader = new BufferedReader(new InputStreamReader(new FileInputStream("common_words")));
		Hashtable<String, String> commonWords= new Hashtable<String, String>();
		String w=cwReader.readLine();
		while(w!=null){
			commonWords.put(w, w);
			w=cwReader.readLine();
		}
		cwReader.close();
		
		StringBuffer searchStr=new StringBuffer();
			
		StringTokenizer st=new StringTokenizer(this.title);
		while(st.hasMoreTokens()){
			String tok=st.nextToken();
			if(commonWords.get(tok)==null){
				searchStr.append("\"");
				searchStr.append(tok);
				searchStr.append("\" ");
			}
		}
		if(mode >= GeoSearch.TITLE_DESC){
			st=new StringTokenizer(this.desc);
					
			while(st.hasMoreTokens()){
				String tok=st.nextToken();
				if(commonWords.get(tok)==null){
					searchStr.append("\"");
					searchStr.append(tok);
					searchStr.append("\" ");
				}
			}
			
			//System.out.println(searchStr);
			if(mode == GeoSearch.ALL) {
				String[] splittedNarr=queryToBoW(this.narr);
				for(int k=0; k < splittedNarr.length; k++){
					if(commonWords.get(splittedNarr[k].trim())==null){
						searchStr.append("text:\"");
						searchStr.append(splittedNarr[k].trim());
						searchStr.append("\" ");
					}
				}
			}
		}
		
		for(int i=0; i < this.locations.size(); i++){
			//searchStr.append("(geo:\"");
			//searchStr.append(this.locations.elementAt(i));
			//searchStr.append("\"^"+geoBoost+" ");
			//searchStr.append("OR wn:\"");
			searchStr.append("wn:\"");
			searchStr.append(this.locations.elementAt(i));
			searchStr.append("\"^"+WNBoost+" ");
			
		}
		
		return searchStr.toString().trim();
		
	}
	
	/**
	 * stringa di ricerca corrispondente alla ricerca standard con Lucene
	 * non usa POS-tagging n� WordNet. Tutte le parole sono usate come search terms,
	 * escluso le stop-words.
	 * @param part
	 * @return
	 * @throws IOException
	 */
	public String getStandardSearchStr(int mode) throws IOException {
		BufferedReader cwReader = new BufferedReader(new InputStreamReader(new FileInputStream("common_words")));
		Hashtable<String, String> commonWords= new Hashtable<String, String>();
		String w=cwReader.readLine();
		while(w!=null){
			commonWords.put(w, w);
			w=cwReader.readLine();
		}
		cwReader.close();
		
		StringBuffer searchStr=new StringBuffer();
		StringTokenizer st=new StringTokenizer(this.title);
		while(st.hasMoreTokens()){
			String tok=st.nextToken();
			if(commonWords.get(tok)==null){
				searchStr.append("\"");
				searchStr.append(tok);
				searchStr.append("\" ");
			}
		}
		
		if(mode >= GeoSearch.TITLE_DESC){
			st=new StringTokenizer(this.desc);
				
			while(st.hasMoreTokens()){
				String tok=st.nextToken();
				if(commonWords.get(tok)==null){
					searchStr.append("\"");
					searchStr.append(tok);
					searchStr.append("\" ");
				}
			}
	
	
			if(mode==GeoSearch.ALL) {
				String[] splittedNarr=queryToBoW(this.narr);
				for(int k=0; k < splittedNarr.length; k++){
					searchStr.append("text:\"");
					searchStr.append(splittedNarr[k].trim());
					searchStr.append("\" ");
				}
			}
		}
		return searchStr.toString().trim();
		
	}
	public void setNarr(String string) {
		this.narr=string;
		
	}
	private String[] queryToBoW(String query){
		String formattedQuery=query.replaceAll("¿", "");
		formattedQuery=formattedQuery.replaceAll("\\?", " ");
		formattedQuery=formattedQuery.replaceAll(",", " ");
		formattedQuery=formattedQuery.replaceAll(";", " ");
		formattedQuery=formattedQuery.replaceAll(":", " ");
		formattedQuery=formattedQuery.replaceAll("\\(", " ");
		formattedQuery=formattedQuery.replaceAll("\\)", " ");
		formattedQuery=formattedQuery.replaceAll("\\.", " ");
		formattedQuery=formattedQuery.replaceAll("\"", " ");
		formattedQuery=formattedQuery.replaceAll("'", " ");
		formattedQuery=formattedQuery.replaceAll("’", " ");
		formattedQuery=formattedQuery.replaceAll(" +", " ");
		formattedQuery=formattedQuery.trim();
		
		return formattedQuery.split(" ");
	}
	
	public List<WorldPoint> getArea() {
		return this.shapeArea;
	}
	public WorldPoint getCentroid() {
		return this.centroid;
	}
	/**
	 * returns a string vector containing all the locations contained in the topic
	 * @return
	 */
	public Vector<String> getLocations(){
		return this.locations;
	}
	/**
	 * used for statistics purposes: 
	 * @return
	 */
	public int countAmbiguousLocations(){
		int cnt=0;
		for(int i=0; i< locations.size(); i++){
			String item = locations.elementAt(i);
			try {
				IIndexWord idxWord = GeoWorSE.dict.getIndexWord((item.trim()).replace(' ', '_'), POS.NOUN);
				if(idxWord != null) {
					if(idxWord.getWordIDs().size() > 1) cnt++;
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		return cnt;
	}
	
	/**
	 * This search string uses meronyms to expand the toponyms in the original queries
	 * @param mode
	 * @param geoboost
	 * @param wordnetboost
	 * @return
	 * @throws IOException
	 */
	
	public String getQMEGeoSStr(int mode, double geoboost, double wordnetboost) throws IOException {
		StringBuffer searchStr = new StringBuffer();
		searchStr.append(getWNGeoSStr(mode, geoboost, wordnetboost));
		if(this.meronyms != null){
			for(String m : meronyms){
				searchStr.append(" (geo:\"");
				searchStr.append(m);
				searchStr.append("\"^"+geoboost+" ");
				searchStr.append("OR wn:\"");
				searchStr.append(m);
				searchStr.append("\"^"+wordnetboost+" )");
				
			}
		}
		
		return searchStr.toString();
	}
	
	/**
	 * This method returns a set of queries, each one with a toponym
	 * @param mode
	 * @param geoboost
	 * @param wordnetboost
	 * @return
	 * @throws Exception 
	 */
	
	public Vector<String> getDiversitySearchStrings(int mode, double geoboost,
			double wordnetboost) throws Exception {
		Vector<String> query_vector=new Vector<String>();
		BufferedReader cwReader = new BufferedReader(new InputStreamReader(new FileInputStream("common_words")));
		Hashtable<String, String> commonWords= new Hashtable<String, String>();
		
		String w=cwReader.readLine();
		while(w!=null){
			commonWords.put(w, w);
			w=cwReader.readLine();
		}
		cwReader.close();
		
		StringBuffer searchStr=new StringBuffer();
		Vector<String> queryTerms = new Vector<String>();
		
		StringTokenizer st=new StringTokenizer(this.title);
		while(st.hasMoreTokens()){
			String tok=st.nextToken();
			if(commonWords.get(tok)==null && !this.locations.contains(tok)){
				searchStr.append("\"");
				searchStr.append(tok);
				queryTerms.add(tok);
				searchStr.append("\" ");
			}
		}
		if(mode >= GeoSearch.TITLE_DESC){
			st=new StringTokenizer(this.desc);
					
			while(st.hasMoreTokens()){
				String tok=st.nextToken();
				if(commonWords.get(tok)==null && !this.locations.contains(tok)){
					searchStr.append("\"");
					searchStr.append(tok);
					queryTerms.add(tok);
					searchStr.append("\" ");
				}
			}
			
			//System.out.println(searchStr);
			if(mode == GeoSearch.ALL) {
				String[] splittedNarr=queryToBoW(this.narr);
				for(int k=0; k < splittedNarr.length; k++){
					if(commonWords.get(splittedNarr[k].trim())==null && !this.locations.contains(splittedNarr[k].trim())){
						searchStr.append("text:\"");
						searchStr.append(splittedNarr[k].trim());
						queryTerms.add(splittedNarr[k].trim());
						searchStr.append("\" ");
					}
				}
			}
		}
		
		for(int i=0; i < this.locations.size(); i++){
			
			StringBuffer extendedSearchStr = new StringBuffer();
			extendedSearchStr.append(searchStr.toString());
			if(GeoSearch.NO_GEO){
				queryTerms.add(this.locations.elementAt(i));
				if(FrequencyFilter.calcPhraseGain(queryTerms, "text") > 0){
					extendedSearchStr.append("\"");
					extendedSearchStr.append(this.locations.elementAt(i));
					extendedSearchStr.append("\"");
					query_vector.add(extendedSearchStr.toString());
				}
				queryTerms.removeElementAt(queryTerms.size());
			} else {
				if(FrequencyFilter.mixedFieldGain(queryTerms, "text", this.locations.elementAt(i), "geo") > 0){
					extendedSearchStr.append("(geo:\"");
					extendedSearchStr.append(this.locations.elementAt(i));
					extendedSearchStr.append("\"^"+geoboost+" ");
					extendedSearchStr.append("OR wn:\"");
					extendedSearchStr.append(this.locations.elementAt(i));
					extendedSearchStr.append("\"^"+wordnetboost+" )");
					query_vector.add(extendedSearchStr.toString());
				}
			}
			
			
		}
		
		if(this.meronyms != null){
			for(String m : meronyms){
				StringBuffer extendedSearchStr = new StringBuffer();
				extendedSearchStr.append(searchStr.toString());
				if(GeoSearch.NO_GEO){
					queryTerms.add(m);
					if(FrequencyFilter.calcPhraseGain(queryTerms, "text") > 0){
						extendedSearchStr.append("\""+m+"\"");
						query_vector.add(extendedSearchStr.toString());
					}
					queryTerms.removeElementAt(queryTerms.size());
				} else {
					if(FrequencyFilter.mixedFieldGain(queryTerms, "text", m, "geo") > 0){
						extendedSearchStr.append("(geo:\""+m+"\"^"+geoboost+" ");
						extendedSearchStr.append("OR wn:\""+m+"\"^"+wordnetboost+" )");
						query_vector.add(extendedSearchStr.toString());
					}
				}
				
			}
		}
		
		return query_vector;
	}
}
