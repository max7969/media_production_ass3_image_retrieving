import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.DocumentBuilderFactory;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;

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
    
    private static String m_imagePath = "./mpup_part3_images";
    private static String m_indexPath = m_imagePath+"/lire_index";
    
    private static String COLOR_LAYOUT = "COLOR_LAYOUT";
    private static String CEDD = "CEDD";
    private static String FCTHI = "FCTHI";
    private static String JPEGC = "JPEGC";
    
    public static void imageSearch(String imageToSearch, String indexPath, String builderType) {
        IndexReader ir = null;
        ImageSearcher imgSearcher = null;
        BufferedImage img = null;
        
        boolean imageOk = false;
        int numberOfImages = 10;
        
        try
        {
            ir = IndexReader.open(FSDirectory.open(new File(indexPath)));
            if (builderType.equals(COLOR_LAYOUT)) {
            	imgSearcher = ImageSearcherFactory.createColorLayoutImageSearcher(numberOfImages);
            } else if (builderType.equals(CEDD)) {
            	imgSearcher = ImageSearcherFactory.createCEDDImageSearcher(numberOfImages);
            } else if(builderType.equals(FCTHI)) {
            	imgSearcher = ImageSearcherFactory.createFCTHImageSearcher(numberOfImages);
            } else if(builderType.equals(JPEGC)) {
            	imgSearcher = ImageSearcherFactory.createJpegCoefficientHistogramImageSearcher(numberOfImages);
            }
            img = ImageIO.read(new File(imageToSearch));
            imageOk = true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        if(imageOk)
        {
            ImageSearchHits hits = null;
            try
            {
                hits = imgSearcher.search(img, ir);
                for(int i = 0; i < hits.length(); i++)
                {
                    String fileName = hits.doc(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
                    System.out.println(hits.score(i)+": \t"+fileName);
                }
            }
            catch(Exception e)
            {
                
            }
        }
    }
    
	public static void imageIndexer(String folderPath, String builderType) throws IOException, ImageReadException, XMPException {
        
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

        	if(builderType.equals(COLOR_LAYOUT)) {
        		builder.addBuilder(DocumentBuilderFactory.getColorLayoutBuilder());
        	} else if (builderType.equals(CEDD)) {
        		builder.addBuilder(DocumentBuilderFactory.getCEDDDocumentBuilder());
        	} else if (builderType.equals(FCTHI)) {
        		builder.addBuilder(DocumentBuilderFactory.getFCTHDocumentBuilder());
        	} else if (builderType.equals(JPEGC)) {
        		builder.addBuilder(DocumentBuilderFactory.getJpegCoefficientHistogramDocumentBuilder());
        	}
            // Adda MPEG-7 Color Layout descritor -based builder
            lire_indexFolder.mkdirs();
            IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, new WhitespaceAnalyzer(Version.LUCENE_36));
            IndexWriter iw  = new IndexWriter(FSDirectory.open(lire_indexFolder), conf);
            
            // Loop image files
            for (File imageFile : imageFiles) 
            {
                System.out.println("Processing image "+imageFile.getAbsolutePath());
                String id = extractXMPMetadata(imageFile);
                boolean processImage = false;
                
                if(id != null && id.length() > 0)
                {
                    
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
    
    private static String extractXMPMetadata(File imageFile) throws ImageReadException, IOException, XMPException {
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
            e.printStackTrace();
        }
        return null;
    }
    
    
    public static void main(String[] args) {
        try 
        {
            ImageIndexer.imageIndexer(m_imagePath, COLOR_LAYOUT);
            ImageIndexer.imageSearch(m_imagePath+"/ff57944d-6c56-4e44-8258-57e0526de687.jpg", m_indexPath, COLOR_LAYOUT);
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
