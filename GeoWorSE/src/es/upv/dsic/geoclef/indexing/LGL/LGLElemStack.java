package es.upv.dsic.geoclef.indexing.LGL;

import java.util.Stack;

/**
 * @author davide
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LGLElemStack extends Stack {
	
	public boolean titleReady(){
		return ((String)this.peek()).equals("title");
	}
	
	public boolean textReady(){
		return ((String)this.peek()).equals("text");
	}
	
	public boolean toponymReady(){
		return ((String)this.peek()).equals("phrase");
	}
	
	public boolean toponymIDReady(){
		return ((String)this.peek()).equals("gaztag");
	}
	
	public boolean latReady(){
		return ((String)this.peek()).equals("lat");
	}
	
	public boolean lonReady(){
		return ((String)this.peek()).equals("lon");
	}
	
	public boolean countryReady(){
		return ((String)this.peek()).equals("country");
	}
	
	public boolean admin1Ready(){
		return ((String)this.peek()).equals("admin1");
	}
}
