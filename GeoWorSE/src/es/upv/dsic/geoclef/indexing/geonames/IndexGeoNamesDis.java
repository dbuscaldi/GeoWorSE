package es.upv.dsic.geoclef.indexing.geonames;

import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import es.upv.dsic.GeoNames.GeoNamesDisambiguator;
import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.indexing.DocumentVector;

import java.io.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;
import java.util.Properties;


class IndexGeoNamesDis {
  
  public static void indexDir(String dir, String lang) 	{
  	Date start = new Date();
    try {
      GeoNamesDisambiguator.init();
      
      String head_fn="gn_index";
      IndexWriterConfig iwc = new IndexWriterConfig(GeoWorSE.LVERSION, new SnowballAnalyzer(GeoWorSE.LVERSION, lang));
      iwc.setOpenMode(OpenMode.CREATE);
      IndexWriter writer = new IndexWriter(FSDirectory.open(new File(head_fn+lang)), iwc);
      
      indexDocs(writer, new File(dir));

      writer.close();

      Date end = new Date();

      System.out.print(end.getTime() - start.getTime());
      System.out.println(" total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }
	
  public static void main(String[] args) throws IOException {
    String usage = "java " + IndexGeoNamesDis.class + " <root_directory>";
//    if (args.length == 0) {
//      System.err.println("Usage: " + usage);
//      System.exit(1);
//    }
    
    GeoWorSE.init();
    GeoNamesDisambiguator.init();
	
    indexDir("/home/datasets/geoclef-GNtagged", "English");
    
    GeoWorSE.close();
  }

  public static void indexDocs(IndexWriter writer, File file)
    throws IOException {
  	// do not try to index files that cannot be read
  	if (file.canRead()) {
      if (file.isDirectory()) {
      	String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file);
        try {
        		GNDisSAXHandler hdlr = new GNDisSAXHandler(file);
            	DocumentVector docs=hdlr.getDocuments();
            	for(int i=0; i< docs.size(); i++){
            		writer.addDocument(docs.getDocumentAt(i));
            	}
        }
        catch (Exception e) {
        	e.printStackTrace();
        } 
      }
    }
  }
}
