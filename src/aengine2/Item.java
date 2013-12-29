package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    Item.java                                                                             *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This class defines an inventory item which the player can use to solve puzzles in the game. *
 * It has an image, which will be displayed in the inventory box or as the user's cursor when it  *
 * is set as the current tool.                                                                    *
 **************************************************************************************************/

import java.awt.image.*;
import javax.imageio.*;
import java.io.*;

//A inventory item
public class Item {
    
    private String image;               //The path to the image file to display
    public BufferedImage imageData;     //The bitmap in memory of the image
    public String handle;               //The name by which the Item is referred
    public int qty;                     //This allows for multiple items of the same type, eg. currency
                                        //qty is not yet implemented by Core.java
    
    //Basic constructor
    //Sets the name and bitmap of the item
    public Item(String newImage, String newHandle) {
        this.changeImage(newImage);
        this.handle = newHandle; 
    }
    
    //Set the bitmap used to represent the item
    private void changeImage(String newImage) {
        
        this.image = newImage;
        
        try{
            this.imageData = ImageIO.read(new File(this.image));
        }catch(IOException e){
            System.out.printf("Couldn't load image '%s'\n", this.image);
        }
        
    }
    
}
