package es.upv.dsic.geoclef.indexing;

import java.util.Stack;

/**
 * @author davide
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ElemStack extends Stack {
	
	public boolean passageTextReady(){
		return ((String)this.peek()).equals("PASSAGE");
	}
	
	public boolean textReady(){
		return ((String)this.peek()).equals("TEXT");
	}
	
	public boolean idReady(){
		return ((String)this.peek()).equals("DOCNO");
	}
	
	public boolean headlineReady(){
		return ((String)this.peek()).equals("HEADLINE");
	}
	
	public boolean toponymReady(){
		return ((String)this.peek()).equals("TOPONYM");
	}
}
