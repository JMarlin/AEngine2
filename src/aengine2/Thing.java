package aengine2;

import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.*;

public class Thing {
        
    public String handle;
    private String image;
    public int x;
    public int y;
    public int z;
    public boolean animationOn;
    private int frame;
    public double scale;
    public boolean hidden;
    public String onClick;
    public String onHover;
    public BufferedImage imageData;
    private ArrayList frames;
    private boolean animated;
    public boolean mirrored;
    public boolean looped;

    public Thing(String startImage, String startHandle) {
        this.handle = startHandle;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.animationOn = false;
        this.frame = 0;
        this.scale = 1.0;
        this.hidden = false;
        this.onClick = "";
        this.onHover = "";
        this.mirrored = false;
        this.looped = false;
        this.frames = new ArrayList();
        this.changeImage(startImage);
    }

    public int getFrameCount(){
        return frames.size();
    }
    
    public boolean isAnimated() {
        return animated;
    }
    
    public int getFrame() {
        return this.frame;
    }
    
    public void setFrame(int newFrame) {
        if(newFrame > this.frames.size() - 1 || newFrame < 0) {
            this.frame = this.frames.size() - 1;
        }else{
            this.frame = newFrame;
        }
        
        this.imageData = (BufferedImage)this.frames.get(this.frame);
    }
    
    
    public String getImagePath() {
        return this.image;
    }
    
    public void changeImage(String newImage) {
        
        File tempFile;
        int aIndex = 0;
        
        this.frames.clear();
        
        this.image = newImage; 
        try {
            this.frames.add(ImageIO.read(new File(this.image)));
            this.animated = true;
        }catch (IOException e) {
            try{
               tempFile = new File(this.image);
               if(tempFile.isDirectory()) {
                   this.animated = true;
                   while(true){
                       tempFile = new File(this.image + "/" + aIndex + ".png");
                       if(tempFile.exists()) {
                           this.frames.add(ImageIO.read(tempFile));
                       }else{
                           break;
                       }
                       aIndex++;
                   }
               }else{ 
                   System.out.printf("There was en error loading %s.\n", this.image);
               }
            }catch(IOException e2){
                System.out.printf("There was en error loading %s.\n", this.image);
            }
        }
        
        this.frame = 0;
        
        if(this.frames.size() > 0) {
            this.imageData = (BufferedImage)this.frames.get(0);
        }else{
            this.imageData = null;
        }
        
    }
        
}
