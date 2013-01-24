package es.upv.dsic.geoclef.stats;

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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import es.upv.dsic.geoWordNet.GeoWNException;
import es.upv.dsic.geoWordNet.GeoWordNetDictionary;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.geography.WorldPoint;
import es.upv.dsic.geoclef.indexing.DocumentVector;
import es.upv.dsic.geoclef.indexing.ElemStack;
import es.upv.dsic.geoclef.indexing.LocationsGroup;

public class XMLStatDocHandlerSAX extends DefaultHandler {
  /* A buffer for each XML element */
  protected StringBuffer textBuffer = new StringBuffer();
  protected StringBuffer docID = new StringBuffer();
  protected Hashtable<ISynset, Integer> locIDs;
  
  //protected DocumentVector mDocuments;
  
  protected ElemStack stackel;
  
  protected StatisticData stats;
  
  protected AbstractSequenceClassifier classifier;
  protected IDictionary dict;
  protected GeoWordNetDictionary gwDict;
  
  public XMLStatDocHandlerSAX(File xmlFile) 
  	throws ParserConfigurationException, SAXException, IOException {
	
	this.locIDs=new Hashtable<ISynset, Integer>();
    this.dict=GeoWorSE.dict;
    this.classifier=GeoWorSE.classifier;
    this.gwDict=GeoWorSE.geoWNdict;
    this.stats=new StatisticData();
    
	// Now let's move to the parsing stuff
    SAXParserFactory spf = SAXParserFactory.newInstance();
    
    // use validating parser?
    //spf.setValidating(false);
    // make parser name space aware?
    //spf.setNamespaceAware(true);

    SAXParser parser = spf.newSAXParser();
    //System.out.println("parser is validating: " + parser.isValidating());
    try {
      parser.parse(xmlFile, this);
    } catch (org.xml.sax.SAXParseException spe) {
      System.out.println("SAXParser caught SAXParseException at line: " +
        spe.getLineNumber() + " column " +
        spe.getColumnNumber() + " details: " +
		spe.getMessage());
    }
  }

  // call at document start
  public void startDocument() throws SAXException {
    //mDocuments = new DocumentVector();
    stackel = new ElemStack();
  	//mDocument = new Document();
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
     //	mDocuments.addElement(new Document());
     	textBuffer.setLength(0);
     	docID.setLength(0);
     	//headlineBuffer.setLength(0);
     }
     
     // list the attribute(s)
     if (attrs != null) {
       for (int i = 0; i < attrs.getLength(); i++) {
         String aName = attrs.getLocalName(i); // Attr name
         if ("".equals(aName)) { aName = attrs.getQName(i); }
         // perform application specific action on attribute(s)
         // for now just dump out attribute name and value
         //System.out.println("attr " + aName+"="+attrs.getValue(i));
       }
     }
     //elementBuffer.setLength(0);
     
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
    if (eName.equals("DOC")){
    	//(mDocuments.lastDocument()).add(new Field("id", docID.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    	//(mDocuments.lastDocument()).add(new Field("text", textBuffer.toString(), Field.Store.YES, Field.Index.ANALYZED));	
    	LocationsGroup locs = this.geoEntities();
    	/*(mDocuments.lastDocument()).add(new Field("geo", locs.getFC(), Field.Store.YES, Field.Index.ANALYZED));
    	(mDocuments.lastDocument()).add(new Field("wn", locs.getSC(), Field.Store.YES, Field.Index.ANALYZED));
    	if(!locs.isScattered()){
    		(mDocuments.lastDocument()).add(new Field("coord", locs.getGeoCoordinates(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    	}*/
    }
  }
  
  /*public DocumentVector getDocuments() {
  	return mDocuments;
  }*/
  
  public StatisticData getStats(){
	  return this.stats;
  }
  
  /**
   * ritorna una stringa contenente tutte le entit� geografiche estratte dal testo del documento
   * ed arricchite con WordNet
   * @return
  */
 protected LocationsGroup geoEntities() {
	 LocationsGroup locs = new LocationsGroup();
	 String taggedTxt=classifier.classifyWithInlineXML(this.textBuffer.toString());
	 
	 //System.err.println(taggedTxt);
	 Pattern locptrn = Pattern.compile(".LOCATION..+?./LOCATION.");
	 Matcher m=locptrn.matcher(taggedTxt);
	 while(m.find()){
		 String item=(m.group()).replaceAll(".(/)?LOCATION.", ""); 
		 //System.err.println(item);
		 try {
			IIndexWord idxWord = dict.getIndexWord((item.trim()).replace(' ', '_'), POS.NOUN);
			if(idxWord != null){
				IWordID wordID=idxWord.getWordIDs().get(0);
				if(idxWord.getWordIDs().size() == 1) this.stats.addMonoTopo();
				this.stats.addTopo();
				IWord word = dict.getWord(wordID);
				locs.addFCelem(word.getLemma());
				
				//synonyms
				ISynset wsyn=word.getSynset();
				if(!locIDs.containsKey(wsyn)) locIDs.put(wsyn, new Integer(1));
				else locIDs.put(wsyn, new Integer((locIDs.get(wsyn))+1));
				
				List<IWord> syns = wsyn.getWords();
				for(Iterator jtr=syns.iterator(); jtr.hasNext();){
					IWord curr=(IWord)jtr.next();
					if(curr!=word) locs.addFCelem(curr.getLemma().replace('_', ' '));
				}
				//holonyms
				Vector<String> holos = this.getInheritedHolonyms(wsyn);
				for(int j=0; j < holos.size(); j++){
					locs.addSCelem(holos.elementAt(j));
				}
				
				//geographical coordinates
				try{
					locs.addPoint(gwDict.getWP(wsyn.getOffset()));
				} catch (GeoWNException gwne){
					//maybe synset is an area synset:
					//look for meronyms and generate a shape
					List<ISynsetID> geoRefMeros= getSubLocations(wsyn);
					if(geoRefMeros.size() > 0){
						List<WorldPoint> pset = new Vector<WorldPoint>();
						for(Iterator<ISynsetID> itr=geoRefMeros.listIterator(); itr.hasNext();){
							ISynsetID curr = itr.next();
							pset.add(gwDict.getWP(curr.getOffset()));
						}
						locs.addPointSet(pset);
					}
				}
			} else {
				//System.err.println("not in WordNet: "+item);
				locs.addFCelem(item);
			}
		 } catch (Exception e){
			 locs.addFCelem(item);
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
		 ISynset holonym = dict.getSynset(holonymID);
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
		 if(gwDict.contains(meronymID.getOffset())) ret.add(meronymID);
	 }
	 
	 return ret;
 }
 
 public Hashtable<ISynset, Integer> getGeoSynsets(){
	 return this.locIDs;
 }
	
}
  