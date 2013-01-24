package es.upv.dsic.geoclef.indexing.wsd;

import java.util.Vector;

public class Instance {
	private String word;
	private String assignedSense;
	private Vector<String> context;
	
	public Instance(String w, String reference){
		this.word=w;
		this.assignedSense=reference;
	}
	
	public void addContext(String contextword){
		if(context==null) context=new Vector<String>();
		this.context.addElement(contextword);
	}
}
