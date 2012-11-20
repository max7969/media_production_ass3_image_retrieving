import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


public class PackageReader {
	private final static String ITEM_XPATH = "packageItem/groupSet/group/itemRef/@residred"; 
	
    public static ArrayList<String> getNewsItemFromPackage(String path) {
    	ArrayList<String> newsItem = new ArrayList<String>();
    	
    	DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		if(documentBuilder == null ) { System.out.println("assadsad"); }
		// Reads all the XML documents listed
		Document xmlDocument = null;
		XPath xpath = XPathFactory.newInstance().newXPath();
		File packageFile = new File(path);
		
		try {
			xmlDocument = documentBuilder.parse(packageFile);
			System.out.println(xmlDocument.getTextContent());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
            e.printStackTrace();
        }
		
		XPathExpression expr;
		NodeList nodes;
		
		//Get NewsItem categories
		try {
			expr = xpath.compile(ITEM_XPATH);
			nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				newsItem.add(nodes.item(i).getTextContent());
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		
		return newsItem;
    }
}
