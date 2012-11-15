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
        
        // get the dc:identifier of the image provided as the search keyword
        String dc_id = ImageIndexer.getDcIdentifier(imageToSearch);
        
        // matching dc:identifier
        if(dc_id != null)
        {
            Set<File> hits = new HashSet<File>(); 
            for(File imgName : M_DCIDENTIFIERS.keySet())
            {
                // check if dc_id matches the dc:identifier of any other (metadata enhanced) image
                if(M_DCIDENTIFIERS.get(imgName).equals(dc_id) && !imgName.equals(imageToSearch))
                {
                    hits.add(imgName);
                }
            }
            if(hits.size() > 0)
            {
                for(File hit : hits)
                {
                    System.out.println("Matching dc:identifier value:\t"+hit.getAbsolutePath());
                }
                return; // no need to search the indexed images
            }
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
                System.out.println(hits.score(i)+": \t"+fileName);
            }
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
        
        // Create a LIRE DocumentBuilder
        ChainedDocumentBuilder builder = null;
        File lire_indexFolder = new File(folderPath, "lire_index");
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
                System.out.println("Processing image "+imageFile.getAbsolutePath());
                String id = getDcIdentifier(imageFile);
                boolean processImage = false;
                
                if(id != null && id.length() > 0)
                {
                    M_DCIDENTIFIERS.put(imageFile, id);
                }
                else // if metadata is not available...
                {
                    try 
                    {
                        BufferedImage img = ImageIO.read(imageFile);
                        Document document = builder.createDocument(img, imageFile.getAbsolutePath());
                        iw.addDocument(document);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }

                }
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
        catch(Exception e) 
        {
            System.out.println("Unable to read the XMP metadata for dc:identifier");
        }
        return null;
    }
    
    
    public static void main(String[] args) {
        
        try 
        {
            ImageIndexer.imageIndexer(M_IMAGEPATH);
            ImageIndexer.imageSearch(M_IMAGEPATH+"/ff57944d-6c56-4e44-8258-57e0526de687.jpg", M_INDEXPATH);
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
