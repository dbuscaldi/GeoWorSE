package es.upv.dsic.geoclef.indexing.LGL;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import es.upv.dsic.geoclef.indexing.DocumentVector;
import es.upv.dsic.geoclef.indexing.LGL.LGLElemStack;

public class LGLDocumentHandlerSAX extends DefaultHandler {
	  protected String docID = new String();
	  protected StringBuffer titleBuffer = new StringBuffer();
	  protected StringBuffer textBuffer = new StringBuffer();
	  
	  protected StringBuffer currGazID = new StringBuffer();
	  protected StringBuffer currToponym = new StringBuffer();
	  protected StringBuffer latBuffer = new StringBuffer();
	  protected StringBuffer lonBuffer = new StringBuffer();
	  protected StringBuffer countryBuffer = new StringBuffer();
	  protected StringBuffer adminBuffer = new StringBuffer();
	  
	  //protected Vector<LGLToponym> toponyms = new Vector<LGLToponym>();
	  protected HashSet<LGLToponym> toponyms = new HashSet<LGLToponym>();
	  
	  protected DocumentVector mDocuments;
	  
	  protected LGLElemStack stackel;
	  
	  public LGLDocumentHandlerSAX(File xmlFile) 
	  	throws ParserConfigurationException, SAXException, IOException {
		
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
	    stackel = new LGLElemStack();
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
	     if(eName=="article") {
	     	mDocuments.addElement(new Document());
	     	textBuffer.setLength(0);
	     	titleBuffer.setLength(0);
	     	
	     	//toponyms= new Vector<LGLToponym>();
	     	toponyms= new HashSet<LGLToponym>();
	     	if (attrs != null) {
		    	 docID=attrs.getValue("docid");
		     }
	     }
	     
	     if(eName=="toponym") {
	    	 currGazID.setLength(0);
	    	 currToponym.setLength(0);
	   	  	 latBuffer.setLength(0);
	   	  	 lonBuffer.setLength(0);
	   	  	 countryBuffer.setLength(0);
	   	  	 adminBuffer.setLength(0);
		 }
	     
	     
	  }

	  // call when cdata found
	  public void characters(char[] text, int start, int length)
	    throws SAXException {
	  	if(stackel.titleReady()){
	  		titleBuffer.append(text, start, length);
	  	} else if (stackel.textReady()) {
	  		textBuffer.append(text, start, length);
	  	} else if (stackel.toponymIDReady()){
	  		currGazID.append(text, start, length);
	  	} else if (stackel.toponymReady()){
	  		currToponym.append(text, start, length);
	  	} else if (stackel.latReady()){
	  		latBuffer.append(text,start,length);
	  	} else if (stackel.lonReady()){
	  		lonBuffer.append(text,start,length);
	  	} else if (stackel.countryReady()){
	  		countryBuffer.append(text,start,length);
	  	} else if (stackel.admin1Ready()){
	  		adminBuffer.append(text,start,length);
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
	    if (eName.equals("toponym")){
	    	LGLToponym t = new LGLToponym(currGazID.toString(), currToponym.toString(), latBuffer.toString(), lonBuffer.toString(), countryBuffer.toString(), adminBuffer.toString());
	    	toponyms.add(t);
	    }
	    if (eName.equals("article")){
	    	(mDocuments.lastDocument()).add(new Field("id", docID.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
	    	(mDocuments.lastDocument()).add(new Field("text", titleBuffer.toString()+" \n"+textBuffer.toString(), Field.Store.YES, Field.Index.ANALYZED));	
	    	
	    	StringBuffer fcTopos = new StringBuffer();
	    	HashSet<String> scTopoNames = new HashSet<String>();
	    	
	    	for(LGLToponym t : toponyms){
	    		fcTopos.append(t.getName());
	    		fcTopos.append(" ");
	    		
	    		scTopoNames.add(t.getCountry());
	    		scTopoNames.add(t.getAdmin());
	    	}
	    	
	    	StringBuffer scTopos = new StringBuffer();
	    	for (String s : scTopoNames){
	    		scTopos.append(s);
	    		scTopos.append(" ");
	    	}
	    	
	    	(mDocuments.lastDocument()).add(new Field("geo", fcTopos.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
	    	(mDocuments.lastDocument()).add(new Field("wn", scTopos.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
	    }
	  }
	  
	  public DocumentVector getDocuments() {
	  	return mDocuments;
	  }
		
	}
