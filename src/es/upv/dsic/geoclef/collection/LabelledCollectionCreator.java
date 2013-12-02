package es.upv.dsic.geoclef.collection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import es.upv.dsic.GeoNames.GeoNamesDisambiguator;
import es.upv.dsic.geoclef.GeoWorSE;

public class LabelledCollectionCreator {
	//creates a collection with geonames-labelled places
	//converting the original GeoCLEF documents
	//XML tags appended to the files (LGL-style)
	private static String rootDir="/home/datasets/geoclef/labelled/";
	
	public static void parseDir(String dir) 	{
	  	Date start = new Date();
	    try {

	      parseDocs(new File(dir));

	      Date end = new Date();

	      System.out.print(end.getTime() - start.getTime());
	      System.out.println(" total milliseconds");

	    } catch (IOException e) {
	      System.out.println(" caught a " + e.getClass() +
	       "\n with message: " + e.getMessage());
	    }
	  }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GeoWorSE.init();
	    GeoNamesDisambiguator.init();
		
	    parseDir("/home/datasets/geoclef/data");
	    
	    GeoWorSE.close();

	}
	
	public static void parseDocs(File file)
    throws IOException {
  	// do not try to index files that cannot be read
	  	if (file.canRead()) {
	      if (file.isDirectory()) {
	      	String[] files = file.list();
	        // an IO error could occur
	        if (files != null) {
	          for (int i = 0; i < files.length; i++) {
	            parseDocs(new File(file, files[i]));
	          }
	        }
	      } else {
	        System.out.println("parsing " + file);
	        try {
	        		XMLDocTaggerSAXHandler hdlr = new XMLDocTaggerSAXHandler(file);
	            	String fileContent=hdlr.getDocuments();
	            	String fileName=rootDir+file.getName();
	            	
	            	System.out.println("writing content to file "+fileName+" ... ");
	            	BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
	            	writer.write(fileContent);
	            	writer.flush();
	            	writer.close();
	            	//System.err.println("content:\n\n"+fileContent+"\n\n--------------------\n\n");
	            	
	        } catch (Exception e) {
	        	e.printStackTrace();
	        } 
	      }
	  	}
	}

}
