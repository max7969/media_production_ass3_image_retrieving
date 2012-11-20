import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class NewsPhotoWriter {
	private static String M_NEWSITEMFOLDER = "./newsItems";
	private static String M_IMAGEFOLDER = "./mpup_part3_images";
	
	public static void writePhotoToFile(String guid, String path) {
		
		try {
            /////////////////////////////
            //Creating an empty XML Document

            //We need a Document
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document xmlNewsItem = docBuilder.newDocument();
            File f = new File(M_IMAGEFOLDER + "/" + path);
            BufferedImage img = ImageIO.read(f);
            ////////////////////////
            //Creating the XML tree

            //create the root element (newsItem in our case)
            Element root = xmlNewsItem.createElement("newsItem");
            
            //add all attributes
            root.setAttribute("guid", guid);
            root.setAttribute("version", "1");
            root.setAttribute("xmlns", "http://iptc.org/std/nar/2006-10-01/");
            root.setAttribute("xsi:schemaLocation", "http://iptc.org/std/nar/2006-10-01/../specification/NewsML-G2_2.9-spec-All-Power.xsd ");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("standard", "NewsML-G2");
            root.setAttribute("standardversion", "2.9");
            root.setAttribute("conformance", "power");
            root.setAttribute("xml:lang", "en-US");
            xmlNewsItem.appendChild(root);
            
            //CREATING GENERAL INFORMATION
            Element catalogRef = xmlNewsItem.createElement("catalogRef");
            catalogRef.setAttribute("href","http://www.iptc.org/std/catalog/catalog.IPTC-G2-standards_16.xml");
            root.appendChild(catalogRef);
            
            Element rightsInfo = xmlNewsItem.createElement("rightsInfo");
            Element copyrightHolder = xmlNewsItem.createElement("copyrightHolder");
            Element nameCopyrightHolder = xmlNewsItem.createElement("name");
            Element copyrightNotice = xmlNewsItem.createElement("copyrightNotice");
            Element usageTerms = xmlNewsItem.createElement("usageTerms");
            
            copyrightHolder.appendChild(nameCopyrightHolder);
            rightsInfo.appendChild(copyrightHolder);
            rightsInfo.appendChild(copyrightNotice);
            rightsInfo.appendChild(usageTerms);
            root.appendChild(rightsInfo);
            
            //CREATING ITEM META
            Element itemMeta = xmlNewsItem.createElement("itemMeta");
            Element itemClass = xmlNewsItem.createElement("copyrigitemClasshtHolder");
            Element provider = xmlNewsItem.createElement("provider");
            Element providerName = xmlNewsItem.createElement("name");
            Element versionCreated = xmlNewsItem.createElement("versionCreated");
            Element firstCreated = xmlNewsItem.createElement("firstCreated");
            Element pubStatus = xmlNewsItem.createElement("pubStatus");
            Element title = xmlNewsItem.createElement("title");
            
            itemClass.setAttribute("qcode", "ninat:picture");
            pubStatus.setAttribute("qcode", "stat:usable");
            
            provider.appendChild(providerName);
            itemMeta.appendChild(itemClass);
            itemMeta.appendChild(provider);
            itemMeta.appendChild(versionCreated);
            itemMeta.appendChild(firstCreated);
            itemMeta.appendChild(pubStatus);
            itemMeta.appendChild(title);
            root.appendChild(itemMeta);
            
            //CREATING CONTENT META
            Element contentMeta = xmlNewsItem.createElement("contentMeta");
            Element urgency = xmlNewsItem.createElement("urgency");
            Element contentCreated = xmlNewsItem.createElement("contentCreated");
            Element creator = xmlNewsItem.createElement("creator");
            Element contributor = xmlNewsItem.createElement("contributor");
            Element creditline = xmlNewsItem.createElement("creditline");
            Element keyword = xmlNewsItem.createElement("keyword");
            Element headline = xmlNewsItem.createElement("headline");
            Element dateline = xmlNewsItem.createElement("dateline");
            Element description = xmlNewsItem.createElement("description");
            
            contentCreated.setTextContent(new Date().toString());
            creator.setAttribute("role", "");
            contributor.setAttribute("role", "");
            description.setAttribute("role", "drol:caption");
            keyword.setAttribute("role", "krole:index");
            
            contentMeta.appendChild(urgency);
            contentMeta.appendChild(contentCreated);
            contentMeta.appendChild(creator);
            contentMeta.appendChild(contributor);
            contentMeta.appendChild(creditline);
            contentMeta.appendChild(keyword);
            contentMeta.appendChild(headline);
            contentMeta.appendChild(dateline);
            contentMeta.appendChild(description);
            root.appendChild(contentMeta);
            
            //CREATING CONTENT SET
            Element contentSet = xmlNewsItem.createElement("contentSet");
            Element remoteContent = xmlNewsItem.createElement("remoteContent");
            Element altId = xmlNewsItem.createElement("altId");
            
            altId.setAttribute("type", "gyiid:masterID");
            altId.setTextContent(f.getName());
            remoteContent.setAttribute("href", M_IMAGEFOLDER + "/" + path);
            remoteContent.setAttribute("version", "1");
            remoteContent.setAttribute("size", "" + f.length());
            remoteContent.setAttribute("contenttype", "image/jpeg");
            remoteContent.setAttribute("width", ""+img.getWidth());
            remoteContent.setAttribute("height", ""+img.getHeight());
            remoteContent.setAttribute("colourspace", "colsp:sRGB");
            remoteContent.setAttribute("orientation", "1");
            remoteContent.setAttribute("resolution","");
            
            remoteContent.appendChild(altId);
            contentSet.appendChild(remoteContent);
            root.appendChild(contentSet);
            
            //Output the XML

            //set up a transformer
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            //create string from xml tree
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(xmlNewsItem);
            trans.transform(source, result);
            String xmlString = sw.toString();
            
            String fileName = f.getName().substring(0, f.getName().lastIndexOf(".jpg"));
            //print xml
            try{
                PrintWriter out  = new PrintWriter(new FileWriter(M_NEWSITEMFOLDER + "/" + fileName +".xml"));
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlString);
                out.close();
            } catch(Exception e){
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println(e);
        }
	}
}
