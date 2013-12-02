package es.upv.dsic.geoclef.collection;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import es.upv.dsic.GeoNames.GeoNamesDisambiguator;
import es.upv.dsic.GeoNames.GeoNamesLocation;
import es.upv.dsic.GeoNames.PlaceInstance;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.indexing.ElemStack;

public class XMLDocTaggerSAXHandler extends DefaultHandler {
	  /* A buffer for each XML element */
	  protected StringBuffer textBuffer = new StringBuffer();
	  protected StringBuffer headlineBuffer = new StringBuffer();
	  protected StringBuffer idBuffer = new StringBuffer();
	  
	  protected ElemStack stackel;
	  protected StringBuffer fullFile;
	  
	  protected AbstractSequenceClassifier classifier;
	  protected IDictionary dict;
	  protected HashSet<GeoNamesLocation> geoSet;
	  
	  public XMLDocTaggerSAXHandler(File xmlFile) 
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
		this.fullFile=new StringBuffer();
	    stackel = new ElemStack();
	    fullFile.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	    
	  }

	  // call at element start
	  public void startElement(String namespaceURI, String localName,
	    String qualifiedName, Attributes attrs) throws SAXException {

	    String eName = localName;
	     if ("".equals(eName)) {
	       eName = qualifiedName; // namespaceAware = false
	     }
	     fullFile.append("<"+eName+">");
	     stackel.addElement(eName);
	     if(eName=="DOC") {
	     	textBuffer.setLength(0);
	     	headlineBuffer.setLength(0);
	     	idBuffer.setLength(0);
	     	geoSet=new HashSet<GeoNamesLocation>();
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
	  	if(stackel.headlineReady()){
	  		headlineBuffer.append(text, start, length);
	  	} else if (stackel.textReady()) {
	  		textBuffer.append(text, start, length);
	  	} else if (stackel.idReady()){
	  		idBuffer.append(text, start, length);
	  	}
	  	fullFile.append(text, start, length);
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
	    	this.makeGeoEntities();
	    	fullFile.append("<TOPONYMS>");
	    	for(GeoNamesLocation l : this.geoSet){
	    		fullFile.append("\n<TOPONYM ");
	    		fullFile.append("geonameid=\""+l.getID()+"\" ");
	    		fullFile.append("lat=\""+l.getLat()+"\" ");
	    		fullFile.append("lon=\""+l.getLon()+"\" >");
	    		fullFile.append(l.getName());
	    		fullFile.append("</TOPONYM>");
	    	}
	    	fullFile.append("\n</TOPONYMS>\n");
	    }
	    fullFile.append("</"+eName+">");
	  }
	  
	  public String getDocuments(){
		  return this.fullFile.toString();
	  }
	  
	  public HashSet<GeoNamesLocation> getLocations(){
		  return this.geoSet;
	  }
	  /**
	   * ritorna una stringa contenente tutte le entit� geografiche estratte dal testo del documento
	   * ed arricchite con WordNet
	   * @return
	  */
	 protected void makeGeoEntities() {
		 //LocationsGroup locs = new LocationsGroup();
		 String taggedTxt=classifier.classifyWithInlineXML(this.textBuffer.toString());
		 
		 Pattern locptrn = Pattern.compile(".LOCATION..+?./LOCATION.");
		 Matcher m=locptrn.matcher(taggedTxt);
		 Vector<PlaceInstance> instances = new Vector<PlaceInstance>();
		
		 //System.err.println("doc ID: "+this.docID.toString());
		 if(this.idBuffer.indexOf("LA") > -1) {
			 //System.err.println("Los Angeles");
			 instances.add(new PlaceInstance(GeoNamesDisambiguator.makeLocationFromID("5368361")));
		 }
		 if(this.idBuffer.indexOf("GH") > -1) {
			 //System.err.println("Glasgow");
			 instances.add(new PlaceInstance(GeoNamesDisambiguator.makeLocationFromID("2648579")));
		 }
		 
		 while(m.find()){
			 String item=(m.group()).replaceAll(".(/)?LOCATION.", ""); 
//			 System.err.println("detected LOC: "+item);
			 instances.add(new PlaceInstance(item));
		 }
		 
		 for(int i=0; i < instances.size(); i++){
			 PlaceInstance ins=instances.elementAt(i);
			 
			 if(ins.getNumberOfReferents() > 1){
				 //System.err.println("disambiguating "+ins.getName());
				 for(int j=0; j<i; j++){
					 if(instances.elementAt(j).isDisambiguated()){
						 ins.addContext(instances.elementAt(j).getPlace());
					 } else {
						 ins.addAmbiguousContext(instances.elementAt(j));
					 }
				 }
				 for(int j=i+1; j < instances.size(); j++){
					 ins.addAmbiguousContext(instances.elementAt(j));
				 }
				 GeoNamesLocation l = ins.disambiguate();
				 if(l != null){
					 //System.err.println("set to "+l.toString());
					 this.geoSet.add(l); //Questo mette solo i toponimi con più di un referente!!!
				 } 
			 } else {
				 GeoNamesLocation loc=ins.getPlace();
				 if(loc!=null) this.geoSet.add(loc);
			 }
		 }
		 
	 }

		
	}

