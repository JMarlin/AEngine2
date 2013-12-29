
package aengine2;

import java.awt.image.*;
import javax.imageio.*;
import java.io.*;

public class Item {
    
    private String image;
    public BufferedImage imageData;
    public String handle;
    public int qty;
    
    public Item(String newImage, String newHandle) {
        this.changeImage(newImage);
        this.handle = newHandle; 
    }
    
    private void changeImage(String newImage) {
        this.image = newImage;
        
        try{
            this.imageData = ImageIO.read(new File(this.image));
        }catch(IOException e){
            System.out.printf("Couldn't load image '%s'\n", this.image);
        }
    }
    
}
