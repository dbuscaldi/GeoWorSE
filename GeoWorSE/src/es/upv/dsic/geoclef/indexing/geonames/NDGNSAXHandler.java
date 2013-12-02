package es.upv.dsic.geoclef.indexing.geonames;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.mit.jwi.IDictionary;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import es.upv.dsic.GeoNames.GeoNamesDisambiguator;
import es.upv.dsic.GeoNames.GeoNamesLocation;
import es.upv.dsic.GeoNames.PlaceInstance;
import es.upv.dsic.GeoPlanet.GeoPlanetDisambiguator;
import es.upv.dsic.GeoPlanet.GeoPlanetPlace;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.geography.InvalidCoordinateRangeException;
import es.upv.dsic.geoclef.geography.WorldPoint;
import es.upv.dsic.geoclef.indexing.DocumentVector;
import es.upv.dsic.geoclef.indexing.ElemStack;
import es.upv.dsic.geoclef.indexing.LocationsGroup;

public class NDGNSAXHandler extends DefaultHandler {
	  /* A buffer for each XML element */
	  protected StringBuffer textBuffer = new StringBuffer();
	  protected StringBuffer docID = new StringBuffer();
	  
	  protected DocumentVector mDocuments;
	  
	  protected ElemStack stackel;
	  
	  protected AbstractSequenceClassifier classifier;
	  protected IDictionary dict;
	  
	  public NDGNSAXHandler(File xmlFile) 
	  	throws ParserConfigurationException, SAXException, IOException {
		
	    this.dict=GeoWorSE.dict;
	    this.classifier=GeoWorSE.classifier;
	    
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
	    mDocuments = new DocumentVector();
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
	     	mDocuments.addElement(new Document());
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
	    	(mDocuments.lastDocument()).add(new Field("id", docID.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
	    	(mDocuments.lastDocument()).add(new Field("text", textBuffer.toString(), Field.Store.YES, Field.Index.ANALYZED));	
	    	LocationsGroup locs = this.geoEntities();
	    	(mDocuments.lastDocument()).add(new Field("geo", locs.getFC(), Field.Store.YES, Field.Index.ANALYZED));
	    	(mDocuments.lastDocument()).add(new Field("wn", locs.getSC(), Field.Store.YES, Field.Index.ANALYZED));
	    	if(!locs.isScattered()){
	    		(mDocuments.lastDocument()).add(new Field("coord", locs.getGeoCoordinates(), Field.Store.YES, Field.Index.NOT_ANALYZED));
	    	}
	    }
	  }
	  
	  public DocumentVector getDocuments() {
	  	return mDocuments;
	  }
	  
	  /**
	   * ritorna una stringa contenente tutte le entit� geografiche estratte dal testo del documento
	   * ed arricchite con GeoNames
	   * NOTA: non disambigua
	   * @return
	  */
	 protected LocationsGroup geoEntities() {
		 LocationsGroup locs = new LocationsGroup();
		 String taggedTxt=classifier.classifyWithInlineXML(this.textBuffer.toString());
//		 System.err.println(taggedTxt);
		 
		 Pattern locptrn = Pattern.compile(".LOCATION..+?./LOCATION.");
		 Matcher m=locptrn.matcher(taggedTxt);
		 while(m.find()){
			 String item=(m.group()).replaceAll(".(/)?LOCATION.", ""); 
//			 System.err.println("detected LOC: "+item);
			 Vector<GeoNamesLocation> referents = GeoNamesDisambiguator.getReferents(item);
			 for(GeoNamesLocation l : referents){
				 locs.addFCelem(l.getName());
				 locs.addSCelem(l.getRegion());
				 Vector<String> altNames = GeoNamesDisambiguator.getAlternateNames(l.getID());
				 for(String n : altNames){
					 if(!n.equals(l.getName())) locs.addFCelem(n);
				 }
				 WorldPoint wp;
				 try {
					wp = new WorldPoint(l.getLat(), l.getLon());
					locs.addPoint(wp);
				 } catch (InvalidCoordinateRangeException e) {
					e.printStackTrace();
				 }
			 }
			 
		 }
		 
		 //locs.setGeoReference();
		 locs.setScattering();
		 return locs;
	 }

		
	}

