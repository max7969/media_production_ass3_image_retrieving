import java.io.File;
import java.io.IOException;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;


public class XMPExtractor {
    private static File file;
    private static XMPSchemaRegistry registry = XMPMetaFactory.getSchemaRegistry();
    
    public XMPExtractor(File file) {
        this.file = file;
    }
    
    public void extractMetaData() throws ImageReadException, IOException, XMPException {

        String text = Sanselan.getXmpXml(file);
        System.out.println(text);
        XMPMeta meta = XMPMetaFactory.parseFromString(text);
        registry.registerNamespace("ns:meta/", "adobe");
        meta.getPropertyString("adobe:ns:meta/", "dc:identifier");
        
    }
    
}