package es.upv.dsic.geoclef;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.util.Version;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import es.upv.dsic.geoWordNet.GeoWordNetDictionary;

class ConfigHandler extends DefaultHandler {
	private StringBuffer textBuffer = new StringBuffer();
	String WN_HOME, GEOWN_HOME, STANFORD_HOME, WN_MAPPINGS_HOME;
    Version LUCENE_VERSION;
	private Map<String, Version >  versions;   
	private Stack<String> stackel;
	private String type="";
	  
	public ConfigHandler(File xmlFile)
	  	throws ParserConfigurationException, SAXException, IOException {
			versions =  new HashMap<String, Version>();
		    versions.put("LUCENE_29", Version.LUCENE_29);  
		    versions.put("LUCENE_30", Version.LUCENE_30);
		    versions.put("LUCENE_31", Version.LUCENE_31);
		    versions.put("LUCENE_32", Version.LUCENE_32);
		    versions.put("LUCENE_33", Version.LUCENE_33);
		    versions.put("LUCENE_34", Version.LUCENE_34);
		    versions.put("LUCENE_35", Version.LUCENE_35);
		    versions.put("LUCENE_36", Version.LUCENE_36);
		    
			SAXParserFactory spf = SAXParserFactory.newInstance();
	  	    SAXParser parser = spf.newSAXParser();
	  	    try {
	  	      parser.parse(xmlFile, this);
	  	    } catch (org.xml.sax.SAXParseException spe) {
	  	      System.err.println("TopicParser caught SAXParseException at line: " +
	  	        spe.getLineNumber() + " details: " +
	  			spe.getMessage());
	  	    }
	  }
	
	  public void startDocument() throws SAXException {
	    stackel = new Stack<String>();
	  }

	  public void startElement(String namespaceURI, String localName,
	    String qualifiedName, Attributes attrs) throws SAXException {

	    String eName = localName;
	     if ("".equals(eName)) {
	       eName = qualifiedName; // namespaceAware = false
	     }
	     stackel.addElement(eName);
	     
	     if(eName=="param") {
	    	 textBuffer.delete(0, textBuffer.length());
	     }
	     
	     if (attrs != null) {
	    	 this.type=attrs.getValue("name");
	     }
	     //elementBuffer.setLength(0);
	     
	  }

	  public void characters(char[] text, int start, int length)
	    throws SAXException {
	  	if(((String)stackel.peek()).equals("param")){
	  		textBuffer.append(text, start, length);
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
	    
	    if (eName.equals("param")){
	    	if (type.equals("WN_HOME")){
	    		this.WN_HOME=textBuffer.toString();
	    	} else if (type.equals("GEOWN_HOME")){
	    		this.GEOWN_HOME=textBuffer.toString();
	    	} else if (type.equals("WN_MAPPINGS")){
	    		this.WN_MAPPINGS_HOME=textBuffer.toString();
	    	} else if (type.equals("LUCENE_VERSION")) {
	    		this.LUCENE_VERSION=versions.get(textBuffer.toString().trim());
	    	} else {
	    		this.STANFORD_HOME=textBuffer.toString();
	    	}
	    	
	    }
	  }
}
public class GeoWorSE {
	public static String WN_HOME, GEOWN_HOME, STANFORD_HOME, WN_MAPPINGS_HOME;
	public static IDictionary dict;
	public static AbstractSequenceClassifier classifier;
	public static GeoWordNetDictionary geoWNdict;
	public static Version LVERSION=Version.LUCENE_31;
	
	public static void init(){
		//load config file (config.xml)
		try {
			ConfigHandler ch = new ConfigHandler(new File("config.xml"));
			WN_HOME=ch.WN_HOME;
			GEOWN_HOME=ch.GEOWN_HOME;
			STANFORD_HOME=ch.STANFORD_HOME;
			WN_MAPPINGS_HOME=ch.WN_MAPPINGS_HOME;
			LVERSION=ch.LUCENE_VERSION;
			
			String serializedClassifier = GeoWorSE.STANFORD_HOME;      
		    System.err.println("Initializing NERC from file=" + serializedClassifier);
		    classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		    System.err.println("NERC initialized.");
		    
		    // init WordNet
		    String wnhome=GeoWorSE.WN_HOME;
			String separator = System.getProperty("file.separator");
			String path = wnhome+separator+"dict";
			URL url = null;
			try{
				url = new URL("file", null, path);
			} catch (MalformedURLException e){
				e.printStackTrace();
				System.exit(-1);
			}
			if (url==null) {
				System.err.println("Error: unable to open WordNet");
				System.exit(-1);
			}
			dict = new Dictionary(url);
			dict.open();
			
			// load GeoWN
//			loading geoWordNet data:
		    System.err.println("loading geoWordNet...");
		    geoWNdict= new GeoWordNetDictionary();
		    System.err.println("done.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		} 
	}


	public static void close() {
		dict.close();
	}
}
