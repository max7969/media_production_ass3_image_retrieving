import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.DocumentBuilderFactory;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;

import javax.xml.parsers.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

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
    
    private static String COLOR_LAYOUT = "COLOR_LAYOUT";
    private static String CEDD = "CEDD";
    private static String FCTH = "FCTH";
    private static String TAMURA = "TAMURA";
    

    public static ArrayList<String> imageSearch(String imageToSearch, String indexPath, String builderType) throws ImageReadException, IOException, XMPException {
        IndexReader ir = null;
        ImageSearcher imgSearcher = null;
        BufferedImage img = null;
        ArrayList<String> matches = new ArrayList<String>();
        
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
    				matches.add(hit.getName());
    				System.out.println(hit.getName());
    			}
    			System.out.println("\nFound matching dc:identifier(s). Skipping the index search...\n");
    			return matches; // no need to search the indexed images
    		}
    		System.out.println("Did not find any matching dc:identifiers. Continuing with the index search...");
    	}
    	
		try
		{
			ir = IndexReader.open(FSDirectory.open(new File(indexPath)));
			if (builderType.equals(COLOR_LAYOUT)) {
				imgSearcher = ImageSearcherFactory.createColorLayoutImageSearcher(numberOfImages);
			} else if (builderType.equals(CEDD)) {
				imgSearcher = ImageSearcherFactory.createCEDDImageSearcher(numberOfImages);
			} else if(builderType.equals(FCTH)) {
				imgSearcher = ImageSearcherFactory.createFCTHImageSearcher(numberOfImages);
			} else if(builderType.equals(TAMURA)) {
				imgSearcher = ImageSearcherFactory.createTamuraImageSearcher(numberOfImages);
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
			System.out.println("Matches found by Index search are :");
			ImageSearchHits hits = null;
			hits = imgSearcher.search(img, ir);
			for(int i = 0; i < hits.length(); i++)
			{
				String fileName = hits.doc(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
				File f = new File(fileName);
				if (hits.score(i) > 0.5) {
					matches.add(f.getName());
				}
				System.out.println(String.format("%1.8f:\t%s",hits.score(i), f.getName()));
			}
			System.out.println("");
		}
    	
    	return matches;
    }
    
    public static String imageSearchById(String idToSearch) {
    	for(File imgFile : M_DCIDENTIFIERS.keySet())
		{
			// check if dc_id matches the dc:identifier of any other (metadata enhanced) image (imgName)
			if(M_DCIDENTIFIERS.get(imgFile).equals(idToSearch))
			{
				return imgFile.getPath();
			}
		}
    	
    	return null;
    }
    
	public static void imageIndexer(String folderPath, String builderType, boolean skipIndex) throws IOException, ImageReadException, XMPException {
        
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
        if (!skipIndex) {
        	ChainedDocumentBuilder builder = null;
        	File lire_indexFolder = new File(M_IMAGEPATH, "lire_index");
        	
        	// if index doesn't exists...
        	if(!IndexReader.indexExists(FSDirectory.open(lire_indexFolder))) 
        	{
        		builder = new ChainedDocumentBuilder();
        		
        		if(builderType.equals(COLOR_LAYOUT)) {
        			builder.addBuilder(DocumentBuilderFactory.getColorLayoutBuilder());
        		} else if (builderType.equals(CEDD)) {
        			builder.addBuilder(DocumentBuilderFactory.getCEDDDocumentBuilder());
        		} else if (builderType.equals(FCTH)) {
        			builder.addBuilder(DocumentBuilderFactory.getFCTHDocumentBuilder());
        		} else if (builderType.equals(TAMURA)) {
        			builder.addBuilder(DocumentBuilderFactory.getTamuraDocumentBuilder());
        		}
        		// Adda MPEG-7 Color Layout descritor -based builder
        		lire_indexFolder.mkdirs();
        		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, new WhitespaceAnalyzer(Version.LUCENE_36));
        		IndexWriter iw  = new IndexWriter(FSDirectory.open(lire_indexFolder), conf);
        		
        		// Loop image files
        		for (File imageFile : imageFiles) 
        		{
        			System.out.println("Processing image "+imageFile.getName());
        			try {
        				BufferedImage img = ImageIO.read(imageFile);
        				Document document = builder.createDocument(img, imageFile.getAbsolutePath());
        				iw.addDocument(document);
        			} catch(Exception e) {
        				e.printStackTrace();
        			}
        		}
        		iw.close();
        	}
        }
    }
    
    public static String getDcIdentifier(String imageFile) throws ImageReadException, IOException, XMPException {
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
        ArrayList<String> newsItems;
        ArrayList<String> fileItems = new ArrayList<String>();
        Scanner scanner = new Scanner(System.in);
        String packageName = "";
        int indexerType = 0;
        int fullIndexerInt = 0;
        String indexer = COLOR_LAYOUT;
        boolean indexerOk = false;
        boolean fullIndexer = true;
        boolean fullIndexerOk = false;
        
        System.out.println("IMAGE INDEXER V1 -- This image indexer permits to retrieve photos relative to newsitems which are stored in a package");
        System.out.println("Put your package file in the XML format in the image folder:" + M_IMAGEPATH);
        System.out.println("What is the name of your package ? (Example: topic.xml)");
        packageName = scanner.nextLine();
        System.out.println("Which type of indexer do you want to create ?");
        System.out.println("1) CEDD");
        System.out.println("2) COLOR LAYOUT");
        System.out.println("3) FCTH");
        System.out.println("4) TAMURA");
        
        
        while (!indexerOk) {
        	indexerType = scanner.nextInt();
        	if(indexerType > 0 && indexerType < 5) {
        		indexerOk = true;
			} else {
				System.out.println("This option doesn't exist");
				scanner.nextLine();
			}
        }
        
        System.out.println("Do you want to create a full indexer ?");
        System.out.println("1) YES");
        System.out.println("2) NO");
        
        while (!fullIndexerOk) {
        	fullIndexerInt = scanner.nextInt();
        	if(fullIndexerInt > 0 && fullIndexerInt < 3) {
        		fullIndexerOk = true;
			} else {
				System.out.println("This option doesn't exist");
				scanner.nextLine();
			}
        }
        
        switch (indexerType) {
		    case 1:
		    	indexer = CEDD;
		    	break;
		    case 2:
		    	indexer = COLOR_LAYOUT;
		    	break;
		    case 3:
		    	indexer = FCTH;
		    	break;
		    case 4:
		    	indexer = TAMURA;
		    	break;
        }
        
        if (fullIndexerInt == 1) {
        	fullIndexer = false;
        } else {
        	fullIndexer = true;
        }
        
        try 
        {
        	System.out.println("LOADING NEWSITEMS FROM PACKAGE ....");
        	newsItems = PackageReader.getNewsItemFromPackage(M_IMAGEPATH+"/" + packageName);
        	System.out.println("INDEXING IMAGES ....");
            ImageIndexer.imageIndexer(M_IMAGEPATH, indexer, fullIndexer);
            
            System.out.println("RETRIEVING IMAGES ....");
            for(String newsItem : newsItems) {
            	String filePath = ImageIndexer.imageSearchById(newsItem);
            	if (filePath != null) {
            		fileItems.add(filePath);
            	}
            }
            
            System.out.println(fileItems);
            
            System.out.println("CREATING NEWSITEMS ....");
            for(String fileItem : fileItems) {
            	System.out.println("=========================================================");
            	ArrayList<String> filePaths = ImageIndexer.imageSearch(fileItem, M_INDEXPATH, indexer);
            	filePaths.add(fileItem.substring(fileItem.lastIndexOf("./mpup_part3_images/")+20));
            	
            	System.out.println("MATCHES FOUND FOR " + fileItem);
            	String id = getDcIdentifier(fileItem);
            	
        		for(String filePath : filePaths) {
        			NewsPhotoWriter.writePhotoToFile(id, filePath);
        			System.out.println("- " + filePath);
            	}
            	System.out.println("=========================================================");
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
