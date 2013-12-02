package es.upv.dsic.geoclef.indexing;

import java.util.Vector;

import org.apache.lucene.document.Document;

/**
 * @author davide
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DocumentVector extends Vector {
	public Document getDocumentAt(int i){
		return (Document)this.elementAt(i);
	}
	
	public Document lastDocument(){
		return (Document)this.lastElement();
	}
}
