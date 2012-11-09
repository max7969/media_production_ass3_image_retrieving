import java.io.File;
import java.io.IOException;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;


public class XMPExtractor {
    private File file;
    private XMPSchemaRegistry registry = XMPMetaFactory.getSchemaRegistry();
    
    public XMPExtractor(File file) {
        this.file = file;
    }
    
    public void extractMetaData() throws ImageReadException, IOException, XMPException {

        String text = Sanselan.getXmpXml(file);
        System.out.println(text);
        XMPMeta meta = XMPMetaFactory.parseFromString(text);
        //registry.registerNamespace("http://purl.org/dc/elements/1.1/", "dc");
        String dc = "http://purl.org/dc/elements/1.1/";
        System.out.println(meta.getPropertyString("dc", "identifier"));
        
    }
    
}