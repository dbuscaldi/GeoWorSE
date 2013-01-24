package es.upv.dsic.geoclef.indexing.wsd;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.tools.bzip2.CBZip2InputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.item.SynsetID;
import es.upv.dsic.geoWordNet.GeoWNException;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.geography.WorldPoint;
import es.upv.dsic.geoclef.indexing.DocumentVector;
import es.upv.dsic.geoclef.indexing.LocationsGroup;
import es.upv.dsic.wordnet.mappings.Mapping;

class WSDComparator implements Comparator {

	public int compare(Object arg0, Object arg1) {
		Map.Entry o1 = (Map.Entry)arg0;
		Map.Entry o2 = (Map.Entry)arg1;
		Float val1 = (Float)o1.getValue();
		Float val2 = (Float)o2.getValue();
		return Float.compare(val1, val2);
	}
	
}

class RevWSDComparator implements Comparator {
	public int compare(Object arg0, Object arg1) {
		Map.Entry o1 = (Map.Entry)arg0;
		Map.Entry o2 = (Map.Entry)arg1;
		Float val1 = (Float)o1.getValue();
		Float val2 = (Float)o2.getValue();
		return -Float.compare(val1, val2);
	}
}


public class GeoCLEFWSDDocumentHandler extends DefaultHandler {
  /* A buffer for each XML element */
  protected StringBuffer textBuffer = new StringBuffer();
  protected StringBuffer docID = new StringBuffer();
  
  //protected StringBuffer expansion = new StringBuffer(); //WordNet expansion
  protected int phraseCnt;
  
  protected String wPOS = "";
  protected String lemma = "";
  protected DocumentVector documents;
  protected Vector<String> geoNames;
  protected Hashtable synmap;
  
  protected WSDElemStack stackel;
  
  protected Mapping wnmap;
  protected String [] stopterms = {"aerospace", "north", "south", "west", "east", "main", "bay", "rapid", "beach", "palm", "depot", "crown", "harbor", "ocean"};
  
  
  public GeoCLEFWSDDocumentHandler(File xmlFile) 
  	throws ParserConfigurationException, SAXException, IOException {
	
    this.documents=new DocumentVector ();
    this.wnmap=new Mapping(Mapping.WN_16, Mapping.WN_20);
    
	// Now let's move to the parsing stuff
    SAXParserFactory spf = SAXParserFactory.newInstance();
    SAXParser parser = spf.newSAXParser();
    
    try {
//    	apply BZIP2 decompression and passing a stream to parser
    	
    	FileInputStream readTwoBytes=new FileInputStream(xmlFile);
    	readTwoBytes.read();
    	readTwoBytes.read(); //Hack to the CBzip2 package bug
    	
    	InputStream is = new CBZip2InputStream(readTwoBytes);
    	parser.parse(is, this);
    	is.close();
    } catch (org.xml.sax.SAXParseException spe) {
    	System.out.println("SAXParser caught SAXParseException at line: " +
        spe.getLineNumber() + " column " +
        spe.getColumnNumber() + " details: " +
		spe.getMessage());
    }
  }

  // call at document start
  public void startDocument() throws SAXException {
	  synmap = new Hashtable();
	  stackel = new WSDElemStack();
	  geoNames = new Vector<String>();
	  phraseCnt=0;
  }

  // call at element start
  public void startElement(String namespaceURI, String localName,
    String qualifiedName, Attributes attrs) throws SAXException {

    String eName = localName;
     if ("".equals(eName)) {
       eName = qualifiedName; // namespaceAware = false
     }
     
     stackel.addElement(eName);
     
     if(eName=="DOC") {
       	documents.addElement(new Document());
       	geoNames=new Vector<String>();
       	textBuffer.setLength(0);
       	docID.setLength(0);
       	//headlineBuffer.setLength(0);
     }
     
     if(eName.equals("TERM")){
    	 this.synmap.clear();
    	 if(attrs != null){
    		 this.lemma=attrs.getValue("LEMA");
    		 this.wPOS=attrs.getValue("POS");
    		 if(this.lemma.endsWith(".")) this.lemma=this.lemma.replaceAll("\\.", "");
    	 }
     }
     
     if(eName.equals("SYNSET") && attrs != null){
    	 String id=attrs.getValue("CODE");
    	 Float score= Float.parseFloat(attrs.getValue("SCORE"));
    	 synmap.put(id, score);
         //System.err.println("Code: "+id+" , score: "+score);
     }
      
  }

  // call when cdata found
  public void characters(char[] text, int start, int length)
    throws SAXException {
  	if(stackel.idReady()){
  		docID.append(text, start, length);
  	} else if (stackel.textReady() || stackel.headlineReady()) {
  		textBuffer.append(text, start, length);
  	}
    //elementBuffer.append(text, start, length);
    //System.err.println(text);
  }

  // call at element end
  public void endElement(String namespaceURI, String simpleName,
    String qualifiedName)  throws SAXException {

    String eName = simpleName;
    if ("".equals(eName)) {
      eName = qualifiedName; // namespaceAware = false
    }
    stackel.pop();
    
    if(eName.equals("TERM") && !synmap.isEmpty()){
    	Map.Entry best = (Map.Entry) Collections.max(synmap.entrySet(), new WSDComparator());
    	Iterator itr = synmap.entrySet().iterator();
    	//String [] syn_id = ((String)best.getKey()).split("-");
        
        String syn20 = wnmap.map.get((String)best.getKey());
        if(this.wPOS.equals("NNP") && isGeographical(syn20, this.lemma)) {
        	/*System.err.println("Geo noun: "+this.lemma);
        	String currText=textBuffer.toString().replaceAll("\n", " ").replaceAll(" +", " ");
        	System.err.println("Text: "+currText);
        	System.err.println("press a key to continue:");
        	try {
    			System.in.read();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}*/
    		this.geoNames.addElement(this.lemma+":"+syn20);	
        }
        	
    }
    
    
    if (eName.equals("DOC")){
    	String docText = removeWNformat(textBuffer.toString());
    	//System.err.println("ID: "+docID.toString()+"\ntextBuffer: "+docText);
    	(documents.lastDocument()).add(new Field("id", docID.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    	(documents.lastDocument()).add(new Field("text", docText, Field.Store.YES, Field.Index.ANALYZED));	
    	LocationsGroup locs = this.geoEntities();
    	(documents.lastDocument()).add(new Field("geo", locs.getFC(), Field.Store.YES, Field.Index.ANALYZED));
    	(documents.lastDocument()).add(new Field("wn", locs.getSC(), Field.Store.YES, Field.Index.ANALYZED));
    	//System.err.println(locs.toString());
    	if(!locs.isScattered()){
    		(documents.lastDocument()).add(new Field("coord", locs.getGeoCoordinates(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    	}
    }
  }
  
  public DocumentVector getDocuments() {
  	return documents;
  }
  
  
  
  private String removeWNformat(String text) {
	  String formattedText = textBuffer.toString().replaceAll("\n", " ");
	  formattedText = formattedText.replaceAll(" +", " ");
	  formattedText = formattedText.replace('_', ' ');
	  return formattedText;
  }

  /**
   * ritorna una stringa contenente tutte le entit� geografiche estratte dal testo del documento
   * ed arricchite con WordNet
   * @return
  */
 protected LocationsGroup geoEntities() {
	 LocationsGroup locs = new LocationsGroup(); //TODO:
	 
	 for(int i=0; i< geoNames.size(); i++){
		 String item=geoNames.elementAt(i); //TODO:separare synset da lemma
		 String [] NEparts = item.split(":");
		 String lema=NEparts[0].trim();
		 String offset=NEparts[1];
		 ISynsetID disambiguated_synset=null;
		 
		 try {
			IIndexWord idxWord = GeoWorSE.dict.getIndexWord(lema.replace(' ', '_'), POS.NOUN);
			if(idxWord != null){
				int nsenses=idxWord.getWordIDs().size();
				ISynset wsyn;
				if(nsenses > 1){
					Vector<ISynsetID> geoSyns = new Vector<ISynsetID>();
					for(int j=0; j<nsenses; j++){
						ISynsetID isyn=idxWord.getWordIDs().get(j).getSynsetID();
						String t_off=String.valueOf(isyn.getOffset());
						if(Integer.valueOf(offset)==isyn.getOffset()){
							disambiguated_synset=isyn;
							geoSyns.addElement(isyn);
							//abbiamo gia controllato se era geografico prima
						} else {
							if(isGeographical(t_off, lema)){
								geoSyns.addElement(isyn);
							}
						}
					}
					int nGeoSenses=geoSyns.size();
				
					
					switch(IndexNUSUBCGeoCLEF.MODE){
						case 1 :  //Random
							Random r = new Random();
							int random_index = r.nextInt(nGeoSenses);
							wsyn=GeoWorSE.dict.getSynset(geoSyns.elementAt(random_index));
							break;
						case 2 : //MFS
							wsyn=GeoWorSE.dict.getSynset(geoSyns.elementAt(0));
							break;
						case 3 : //LFS
							wsyn=GeoWorSE.dict.getSynset(geoSyns.elementAt(nGeoSenses-1));
							break;
						default:
							wsyn=GeoWorSE.dict.getSynset(disambiguated_synset);
					}
					
				} else {
					IWordID wordID=idxWord.getWordIDs().get(0);
					IWord word = GeoWorSE.dict.getWord(wordID);
					wsyn=word.getSynset();
				}
				
				//IWord word = GeoWorSE.dict.getWord(wordID);
				//locs.addFCelem(word.getLemma()); //OLD version
				locs.addFCelem(lema);

				//synonyms
				//ISynset wsyn=word.getSynset();
				//se wsyn qui è NULL, non facciamo niente
				if (wsyn==null) continue;
				List<IWord> syns = wsyn.getWords();
				for(Iterator jtr=syns.iterator(); jtr.hasNext();){
					IWord curr=(IWord)jtr.next();
					//if(curr!=word) locs.addFCelem(curr.getLemma().replace('_', ' '));
					if(curr.getLemma() != idxWord.getLemma()) locs.addFCelem(curr.getLemma().replace('_', ' '));
				}
				//holonyms
				Vector<String> holos = this.getInheritedHolonyms(wsyn);
				for(int j=0; j < holos.size(); j++){
					locs.addSCelem(holos.elementAt(j));
				}
				
				//geographical coordinates
				try{
					locs.addPoint(GeoWorSE.geoWNdict.getWP(wsyn.getOffset()));
				} catch (GeoWNException gwne){
					//maybe synset is an area synset:
					//look for meronyms and generate a shape
					List<ISynsetID> geoRefMeros= getSubLocations(wsyn);
					if(geoRefMeros.size() > 0){
						List<WorldPoint> pset = new Vector<WorldPoint>();
						for(Iterator<ISynsetID> itr=geoRefMeros.listIterator(); itr.hasNext();){
							ISynsetID curr = itr.next();
							pset.add(GeoWorSE.geoWNdict.getWP(curr.getOffset()));
						}
						locs.addPointSet(pset);
					}
				}
			} /* else {
				System.err.println("not in WordNet: "+item);
				//System.exit(-1);
			} */
		 } catch (Exception e){
			 //locs.addFCelem(item);
			 System.err.println("unknown error for: "+item);
			 e.printStackTrace();
			 System.exit(-1);
		 }
	 }
	 //locs.setGeoReference();
	 locs.setScattering();
	 return locs;
 }

 protected Vector<String> getInheritedHolonyms(ISynset syn) {
	 List<ISynsetID> holos = syn.getRelatedSynsets(Pointer.HOLONYM_PART);
	 Vector<String> result = new Vector<String>();
	 
	 for(Iterator itr=holos.listIterator(); itr.hasNext();){
		 ISynsetID holonymID = (ISynsetID) itr.next();
		 ISynset holonym = GeoWorSE.dict.getSynset(holonymID);
		 List<IWord> holo_ws = holonym.getWords();
		 for(Iterator jtr=holo_ws.listIterator(); jtr.hasNext();){
			result.addElement(((IWord)jtr.next()).getLemma().replace('_', ' '));
		 }
		 Vector<String> inherited=this.getInheritedHolonyms(holonym);
		 for(int j=0; j<inherited.size(); j++){
				result.addElement(inherited.elementAt(j));
		 }
	 }
	 
	 return result;
 }
 
 protected List<ISynsetID> getSubLocations(ISynset syn) {
	 List<ISynsetID> meros = syn.getRelatedSynsets(Pointer.MERONYM_PART);
	 List<ISynsetID> ret = new Vector<ISynsetID>();
	 
	 for(Iterator itr=meros.listIterator(); itr.hasNext();){
		 ISynsetID meronymID = (ISynsetID) itr.next();
		 if(GeoWorSE.geoWNdict.contains(meronymID.getOffset())) ret.add(meronymID);
	 }
	 
	 return ret;
 }
 
 protected boolean isGeographical(String offset, String lemma) {
	  boolean has_geographical_hype=false;
	  boolean is_contained=false;
	  StringBuffer info= new StringBuffer();
	  for(String s : stopterms){
		  if(lemma.equals(s)) return false;
	  }
	  if(GeoWorSE.geoWNdict.isGeographicalFeature(lemma)) return false;
	  try{
		  ISynset s = GeoWorSE.dict.getSynset(new SynsetID(Integer.parseInt(offset), POS.NOUN));
		  //Estraggo iperonimi
		  List<ISynsetID> hypernyms = s.getRelatedSynsets(Pointer.HYPERNYM);
		  List<IWord> words;
		  for(ISynsetID sid: hypernyms){
			  words = GeoWorSE.dict.getSynset(sid).getWords();
			  for(IWord w : words){
				  if(GeoWorSE.geoWNdict.isGeographicalFeature(w.getLemma().replace('_', ' '))){
					  has_geographical_hype=true;
					  info.append(" hype:"+w.getLemma());
				  }
				  
			  }
		  }
		  //Estraggo olonimi
		  List<ISynsetID> holonyms = s.getRelatedSynsets(Pointer.HOLONYM_PART);
		  for(ISynsetID sid: holonyms){
		  	words = GeoWorSE.dict.getSynset(sid).getWords();
		  	for(IWord w : words){
		  		info.append(" holo:"+w.getLemma());
		  	}
		  }
		  if(holonyms.size() > 0) is_contained=true;
	  } catch(Exception e) {
		  //System.err.println(offset+" not in WN as noun.");
		  //e.printStackTrace();
		  return false;
	  }
	  if(is_contained && has_geographical_hype) {
		  //System.err.println("related synsets: "+info.toString());
		  return true;
	  }
	  else return false;
 }
  
}
  