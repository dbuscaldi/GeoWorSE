package es.upv.dsic.wordnet.mappings;

import java.io.*;
import java.util.Hashtable;

import es.upv.dsic.geoclef.GeoWorSE;

public class Mapping {
	public final static int WN_15=15;
	public final static int WN_16=16;
	public final static int WN_17=17;
	public final static int WN_171=171;
	public final static int WN_20=20;
	public final static int WN_21=21;
	public final static int WN_30=30;
	
	public Hashtable<String, String> map;
	public Hashtable<String, String> reversemap;
	
	private void loadMap(String sourcefile, String POS){
		File nf=new File(sourcefile);
		try {
			BufferedReader nr = new BufferedReader(new InputStreamReader(new FileInputStream(nf)));
			String line;
			while((line = nr.readLine()) != null){
				String [] tokens = line.split(" ");
				String source = tokens[0];
				String target= new String();
				double weight=0.0;
				for(int i=1; i< tokens.length-1; i=i+2){
					if(Double.parseDouble(tokens[i+1]) > weight){
						target=tokens[i];
						weight=Double.parseDouble(tokens[i+1]);
					}
				}
				map.put(source+"-"+POS, target);
				reversemap.put(target+"-"+POS, source); //TODO: verificare se non sia meglio usare il file inverso dei mappings. A naso dovrebbe uscire lo stesso
			}
			nr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Mapping(int v_from, int v_to){
		String rootdir=GeoWorSE.WN_MAPPINGS_HOME+"mapping-"+v_from+"-"+v_to+"/";
		String nounfile=rootdir+"wn"+v_from+"-"+v_to+".noun";
		String verbfile=rootdir+"wn"+v_from+"-"+v_to+".verb";
		String adjfile=rootdir+"wn"+v_from+"-"+v_to+".adj";
		String advfile=rootdir+"wn"+v_from+"-"+v_to+".adv";
		
		map=new Hashtable<String, String>();
		reversemap=new Hashtable<String, String>();
		
		loadMap(nounfile, "n");
		loadMap(verbfile, "n");
		loadMap(adjfile, "n");
		loadMap(advfile, "n");
		
	}
}
