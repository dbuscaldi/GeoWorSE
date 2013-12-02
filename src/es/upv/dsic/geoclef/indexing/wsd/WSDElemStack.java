package es.upv.dsic.geoclef.indexing.wsd;

import java.util.Stack;

public class WSDElemStack extends Stack {
	
	public boolean wfReady() {
		return ((String)this.peek()).equals("WF");
	}
	
	public boolean textReady(){
		return this.contains("TEXT");
	}
	
	public boolean termReady(){
		return ((String)this.peek()).equals("TERM");
	}
	
	public boolean idReady(){
		return ((String)this.peek()).equals("DOCNO");
	}
	
	public boolean headlineReady(){
		return this.contains("HEADLINE");
	}
}
