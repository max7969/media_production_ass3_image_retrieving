import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.DocumentBuilderFactory;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;
import net.semanticmetadata.lire.impl.ColorLayoutDocumentBuilder;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;

public class ImageIndexer {
    
    private static String M_IMAGEPATH = "./mpup_part3_images";
    private static String M_INDEXPATH = M_IMAGEPATH+"/lire_index";
    private static Map<File, String> M_DCIDENTIFIERS = new HashMap<File, String>(); // stores the dc:identifier of an image (if it is available)
    
    public static void imageSearch(String imageToSearch, String indexPath) throws ImageReadException, IOException, XMPException {
        IndexReader ir = null;
        ImageSearcher imgSearcher = null;
        BufferedImage img = null;
        
        boolean imageOk = false;
        int numberOfImages = 10;
        
        // get the dc:identifier of the image provided as the search keyword (returns null if not found)
        String dc_id = ImageIndexer.getDcIdentifier(imageToSearch);
        
        // if the image has dc:identifier, lets loop through other images for matching dc:identifiers
        if(dc_id != null)
        {
            Set<File> hits = new HashSet<File>(); 
            for(File imgFile : M_DCIDENTIFIERS.keySet())
            {
                // check if dc_id matches the dc:identifier of any other (metadata enhanced) image (imgName)
                if(M_DCIDENTIFIERS.get(imgFile).equals(dc_id) && !imageToSearch.contains(imgFile.getName()))
                {
                    hits.add(imgFile);
                }
            }
            if(hits.size() > 0)
            {
                System.out.println("Matching dc:identifier value(s) for "+new File(imageToSearch).getName());
                for(File hit : hits)
                {
                    System.out.println(hit.getName());
                }
                System.out.println("\nFound matching dc:identifier(s). Skipping the index search...\n");
                return; // no need to search the indexed images
            }
            System.out.println("Did not find any matching dc:identifiers. Continuing with the index search...");
        }
        
        ir = IndexReader.open(FSDirectory.open(new File(indexPath)));
        imgSearcher = ImageSearcherFactory.createColorLayoutImageSearcher(numberOfImages);
        img = ImageIO.read(new File(imageToSearch));
        imageOk = true;
        
        if(imageOk)
        {
            ImageSearchHits hits = null;
            hits = imgSearcher.search(img, ir);
            for(int i = 0; i < hits.length(); i++)
            {
                String fileName = hits.doc(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
                System.out.println(String.format("%1.8f:\t%s",hits.score(i), new File(fileName).getName()));
            }
            System.out.println("");
        }
    }
    
    public static void imageIndexer(String folderPath) throws IOException, ImageReadException, XMPException {
        
        File[] imageFiles = new File(folderPath).listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File file) {
                if (file.isFile() && file.getName().endsWith(".jpg")) return true;
                return false;
            } 
        });
        
        // get available dc:identifiers and store them to M_DCIDENTIFIERS
        System.out.println("Getting dc:identifiers...");
        for (File imageFile : imageFiles) 
        {
            String id = getDcIdentifier(imageFile);
            if(id != null && id.length() > 0)
            {
                M_DCIDENTIFIERS.put(imageFile, id); // storing found dc:identifiers
            }
        }
        
        // Create a LIRE DocumentBuilder
        ChainedDocumentBuilder builder = null;
        File lire_indexFolder = new File(folderPath, "lire_index");
        
        // if index doesn't exists...
        if(!IndexReader.indexExists(FSDirectory.open(lire_indexFolder))) 
        {
            builder = new ChainedDocumentBuilder();
            // Add a MPEG-7 Color Layout descriptor -based builder
            //builder.addBuilder(new ColorLayoutDocumentBuilder());
            
            builder.addBuilder(DocumentBuilderFactory.getColorLayoutBuilder());
            lire_indexFolder.mkdirs();
            IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, new WhitespaceAnalyzer(Version.LUCENE_36));
            IndexWriter iw  = new IndexWriter(FSDirectory.open(lire_indexFolder), conf);
            
            // Loop image files
            for (File imageFile : imageFiles) 
            {
                System.out.println("Processing image "+imageFile.getName());

                BufferedImage img = ImageIO.read(imageFile);
                Document document = builder.createDocument(img, imageFile.getAbsolutePath());
                iw.addDocument(document);
            }
            iw.close();
        }
    }
    
    private static String getDcIdentifier(String imageFile) throws ImageReadException, IOException, XMPException {
        return getDcIdentifier(new File(imageFile));
    }
    
    private static String getDcIdentifier(File imageFile) throws ImageReadException, IOException, XMPException {
        String xmpString = null;
        XMPMeta xmp = null;
        
        try 
        {
            xmpString = Sanselan.getXmpXml(imageFile);
            if(xmpString != null) 
            {
                xmp = XMPMetaFactory.parseFromString(xmpString);
                String dc = "http://purl.org/dc/elements/1.1/";
                //registry.registerNamespace("http://purl.org/dc/elements/1.1/", "dc");
                return xmp.getPropertyString(dc, "identifier");
            }
        } 
        catch(Exception e) {}
        return null;
    }
    
    
    public static void main(String[] args) {
        
        try 
        {
            ImageIndexer.imageIndexer(M_IMAGEPATH);
            // image that has no metadata (=> search the index)
            ImageIndexer.imageSearch(M_IMAGEPATH+"/0b5090e2-aadd-4fd4-ac74-16dd97bd08fc.jpg", M_INDEXPATH);
            // image that has metadata (=> search for identical dc:indentifiers)
            ImageIndexer.imageSearch(M_IMAGEPATH+"/1acd4fc4-aff9-4c02-a273-acf36ff26742.jpg", M_INDEXPATH);
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
