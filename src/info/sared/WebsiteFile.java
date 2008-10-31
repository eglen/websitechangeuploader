package info.sared;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

import com.twmacinta.util.MD5;

public class WebsiteFile extends File
{

    private String hash;
    private Date uploadDate;
    
    private static final long serialVersionUID = -1677511625258944283L;

    public WebsiteFile(String pathname)
    {
        super(pathname);
        init();
    }

    public WebsiteFile(URI uri)
    {
        super(uri);
        init();
    }

    public WebsiteFile(String parent, String child)
    {
        super(parent, child);
        init();
    }

    public WebsiteFile(File parent, String child)
    {
        super(parent, child);
        init();
    }
    
    private void init()
    {
        try
        {
            this.hash = MD5.asHex(MD5.getHash(this));
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
