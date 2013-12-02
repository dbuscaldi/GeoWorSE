package es.upv.dsic.geoclef.indexing;

import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import es.upv.dsic.geoclef.GeoWorSE;

import java.io.*;

import java.util.Date;



class IndexArtificialAmbiguousToponyms {
  public static int max_errors=0;
  public static int current_errors=0;
  public static double error_percentage=0.0; //from 0.0 to 1.0
  public static boolean PASSAGE_INDEXING_ON = false;

  
  public static void indexDir(String dir, String lang) 	{
  	Date start = new Date();
    try {
      String extension="Err"+(error_percentage*100);
      IndexWriterConfig iwc = new IndexWriterConfig(GeoWorSE.LVERSION, new SnowballAnalyzer(GeoWorSE.LVERSION, lang));
      iwc.setOpenMode(OpenMode.CREATE);
      IndexWriter writer;
      if(!PASSAGE_INDEXING_ON)
      	writer = new IndexWriter(FSDirectory.open(new File("index"+lang+"-"+extension)), iwc);
        else 
      	writer = new IndexWriter(FSDirectory.open(new File("p_index"+lang+"-"+extension)), iwc);

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
    String usage = "java " + IndexArtificialAmbiguousToponyms.class + " <root_directory>";
//    if (args.length == 0) {
//      System.err.println("Usage: " + usage);
//      System.exit(1);
//    }
    
    GeoWorSE.init();
	
    //NOTA: ciclo per creare indici multipli
    for(error_percentage=0.0; error_percentage < 0.7; error_percentage+=0.1){
    	max_errors=(int)(512871*error_percentage);
        
        if(!PASSAGE_INDEXING_ON){
        	indexDir("/home/datasets/geoclef/data", "English");
        }
        else {
        	indexDir("/home/datasets/clef-qa/english/passages", "English");
        }
        System.err.println("# of produced errors: "+current_errors);
    }
    
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
        		ArtificialAmbiguityHandlerSAX hdlr = new ArtificialAmbiguityHandlerSAX(file, error_percentage, max_errors, current_errors);
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
