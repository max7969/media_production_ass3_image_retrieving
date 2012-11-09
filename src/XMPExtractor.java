import java.io.File;
import java.io.IOException;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;


public class XMPExtractor {
    private static File file;
    private static JpegImageMetadata metadata;
    
    public XMPExtractor(File file) {
        this.file = file;
    }
    
    public void extractMetaData() throws ImageReadException, IOException {
        IImageMetadata temp = Sanselan.getMetadata(this.file);
        if(temp instanceof JpegImageMetadata)
        {
            this.metadata = (JpegImageMetadata)temp;
            this.metadata.getRawImageData().getImageData();
        }
        System.out.println(Sanselan.getXmpXml(file));
        
    }
    
}