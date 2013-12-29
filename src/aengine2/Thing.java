package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    Thing.java                                                                            *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This class defines a sprite which may be rendered onto the playfield and may be associated  *
 * with a script which will be executed by the engine when the sprite is clicked upon.            *
 * Animations can be spawned by specifying a folder of numbered (eg 0.png, 1.png, ..., n.png)     *
 * images instead of specifying a single image file.                                              *
 **************************************************************************************************/

import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.*;

//A visible object in the scene
public class Thing {
        
    public String handle;               //The name by which the object will be referred
    private String image;               //The path to the image to display
    public int x;                       //X-component of the top left corner of this Thing
    public int y;                       //Y-component of the top left corner of this Thing
    public int z;                       //Order in the stack of Thing objects on screen, 0 = furthest away
    public boolean animationOn;         //Whether animation is enabled
    private int frame;                  //The current frame to display
    public double scale;                //Scaling factor to apply before rendering
    public boolean hidden;              //Whether or not to display the Thing at all
    public String onClick;              //The script to be executed when the Thing is clicked 
    public BufferedImage imageData;     //The bitmap in memory of this sprite
    private ArrayList frames;           //Collection of bitmaps in memory comprising the animation
    private boolean animated;           //Whether or not this sprite can be animated
    public boolean mirrored;            //Reflects the drawn image about its vertical axis
    public boolean looped;              //Whether or not to restart the animation on its completion

    //Basic constructor
    //Nulls object properties and loads the specified image(s)
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
        this.mirrored = false;
        this.looped = false;
        this.frames = new ArrayList();
        this.changeImage(startImage);
    }

    //Return the number of frames in this animation
    public int getFrameCount(){
        return this.frames.size();
    }
    
    //Getter for the animated property
    public boolean isAnimated() {
        return this.animated;
    }
    
    //Getter for the displayed frame number
    public int getFrame() {
        return this.frame;
    }
    
    //Set the current frame of animation to display
    public void setFrame(int newFrame) {
        
        //Ignore invalid frame numbers
        if(newFrame > this.frames.size() - 1 || newFrame < 0) {
            this.frame = this.frames.size() - 1;
        }else{
            this.frame = newFrame;
        }
        
        this.imageData = (BufferedImage)this.frames.get(this.frame);
    }
    
    //Getter for the image path
    public String getImagePath() {
        return this.image;
    }
    
    //Load the specified image(s) into memory
    public void changeImage(String newImage) {
        
        File tempFile;      //Used to reference each frame at a time
        int aIndex = 0;     //The frame numbers found in the provided folder
        
        //Remove all loaded image data
        this.frames.clear();
        this.image = newImage;
        
        //Try to open the provided path as an image file
        //If that fails, look for numbered frames and load them in sequence
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
        
        //Rewind the animation
        this.frame = 0;
        
        //Return a blank reference if nothing was loaded
        if(this.frames.size() > 0) {
            this.imageData = (BufferedImage)this.frames.get(0);
        }else{
            this.imageData = null;
        }
        
    }
        
}
