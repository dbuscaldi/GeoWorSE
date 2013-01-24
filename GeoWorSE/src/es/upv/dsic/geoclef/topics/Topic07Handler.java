/*
 * Created on 8-giu-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package es.upv.dsic.geoclef.topics;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import es.upv.dsic.geoclef.geography.InvalidCoordinateRangeException;
import es.upv.dsic.geoclef.search.GeoSearch;

/**
 * @author davide
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Topic07Handler extends DefaultHandler {
	 private StringBuffer titleBuffer = new StringBuffer();
	 private StringBuffer topicID = new StringBuffer();
	 private StringBuffer descBuffer = new StringBuffer();
	 private StringBuffer narrBuffer=new StringBuffer();
	  
	 private AbstractSequenceClassifier classifier;
	 
	 private Vector<Topic> topics;
	 private Stack stackel;
	 private int mode;
	  
	  public Topic07Handler(File xmlFile, AbstractSequenceClassifier classifier, int mode)
	  	throws ParserConfigurationException, SAXException, IOException {
		  	this.classifier=classifier;
		  	this.mode=mode;
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
	  	      System.out.println("TopicParser caught SAXParseException at line: " +
	  	        spe.getLineNumber() + " details: " +
	  			spe.getMessage());
	  	    }
	  }
	  
//	 call at document start
	  public void startDocument() throws SAXException {
	   topics = new Vector<Topic>();
	    stackel = new Stack();
	  }

	  // call at element start
	  public void startElement(String namespaceURI, String localName,
	    String qualifiedName, Attributes attrs) throws SAXException {

	    String eName = localName;
	     if ("".equals(eName)) {
	       eName = qualifiedName; // namespaceAware = false
	     }
	     
	     stackel.addElement(eName);
	     if(eName=="top") {
	     	topics.addElement(new Topic());
	     	titleBuffer.setLength(0);
	     	topicID.setLength(0);
	     	descBuffer.setLength(0);
	     	narrBuffer.setLength(0);
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
	  	if(((String)stackel.peek()).equals("num")){
	  		topicID.append(text, start, length);
	  	} else if (((String)stackel.peek()).equals("title")) {
	  		titleBuffer.append(text, start, length);
	  	} else if (((String)stackel.peek()).equals("desc")) {
	  		descBuffer.append(text, start, length);
	  	} else if (((String)stackel.peek()).equals("narr")) {
	  		narrBuffer.append(text, start, length);
	  	}
	  }

	  // call at element end
	  public void endElement(String namespaceURI, String simpleName,
	    String qualifiedName)  throws SAXException {

	    String eName = simpleName;
	    if ("".equals(eName)) {
	      eName = qualifiedName; // namespaceAware = false
	    }
	    stackel.pop();
	    
	    if (eName.equals("top")){
	    		((Topic)topics.lastElement()).setID(topicID.toString());
	    		((Topic)topics.lastElement()).setTitle(titleBuffer.toString());
	    		((Topic)topics.lastElement()).setDesc(descBuffer.toString());
	    		((Topic)topics.lastElement()).setNarr(narrBuffer.toString()); 
	    		((Topic)topics.lastElement()).addLocations(getLocations());
	    		try {
					((Topic)topics.lastElement()).setGeographicalData();
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
					System.exit(-1);
				}
	    }
	    //mDocument.add(Field.Text(eName, elementBuffer.toString()));
	    
	  }
	  
	  public Vector<Topic> getTopics(){
	  	return this.topics;
	  }
	  /**
	   * returns all the locations found in title, desc and narrative
	   * @return
	   */
	  private Vector<String> getLocations(){
		  HashSet<String> locs = new HashSet<String>();
		  //StringVector locs= new StringVector();
		  String txt = titleBuffer.toString()+". ";
		  if(this.mode >= GeoSearch.TITLE_DESC) txt+=descBuffer.toString();
		  if(this.mode == GeoSearch.ALL) txt=txt+" "+narrBuffer.toString();
		  
		  String taggedTxt=classifier.classifyWithInlineXML(txt);
		  Pattern locptrn = Pattern.compile(".LOCATION..+?./LOCATION.");
		  Matcher m=locptrn.matcher(taggedTxt);
		  while(m.find()){
			  String item=(m.group()).replaceAll(".(/)?LOCATION.", ""); 
			  locs.add(item);
		  }
		  Vector<String> ret = new Vector<String>();
		  for(String l : locs){
			  ret.addElement(l);
		  }
		  
		  return ret;
	  }

	public Topic getTopic(String string) {
		for(int i=0; i < this.topics.size(); i++){
			String tid=(((Topic)topics.elementAt(i)).getID()).trim();
			if (tid.equals(string)){
				return (Topic)topics.elementAt(i);
			}
		}
		return null;
	}
	  
}
