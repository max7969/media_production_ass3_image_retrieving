import java.io.File;
import java.io.IOException;

import org.apache.sanselan.ImageReadException;


public class main {
    public static void main(String[] args) {
        XMPExtractor extractor = new XMPExtractor(new File("mpup_part3_images/0004e7f5-dd1c-444e-918f-a62e05407d7b.jpg"));
        try {
            extractor.extractMetaData();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
