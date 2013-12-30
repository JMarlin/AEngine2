package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    timeThread.java                                                                       *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *   This thread handles all time-dependent functions of the engine. These include calling the    *
 * Core canvas's paint function 24 times a second to facilitate rendering and animation,          *
 * processing the walk action of the player character (which needs overhauling to utilize the new *
 * polygon-based path solver) and processing Triggers from the InterfaceTree.                     *
 **************************************************************************************************/

import java.util.*;
import javax.script.*;
import javax.swing.*;

//Process timed events
public class timeThread extends Thread {
    
    private ScriptEngine engine;                //The Rhino instance used to process Trigger scripts
    private JPanel canvas;                      //The Core JPanel
    private double lastTime;                    //The previous time at which the timing loop executed
    private double xSpeed;                      //Pixels per frame which the player character moves horizontally
    private double ySpeed;                      //Pixels per frame which the player character moves vertically
    private boolean walking;                    //Wether or not the player character is walking
    private InterfaceTree aengine;              //The interface tree of the engine
    private double feetX, feetY, Ydir, Xdir;    //The bottom center of the player character image and whether the character is moving up, down, left or right
    private String tempScript;                  //The current trigger script to be executed
    public boolean halt;                        //Raised to stop the thread's execution
    
    //The main timing loop
    @Override public void run() {
             
        double rightNow;    //The current system time        
        
            //Process until the thread is interrupted
            while(!this.isInterrupted()) {
                                
                //Ensure that the rest of the engine is running
                if(aengine != null && engine != null){
                    if(aengine.mainCharacter != null) {
                        
                    //Get the system time    
                    rightNow = new Date().getTime();

                    
            //----------------| PLAYER CHARACTER WALKING INIT |----------------------//
                    
                    //Calculate the bottom center of the player character image
                    feetX = aengine.mainCharacter.x+((aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale)/2);
                    feetY = aengine.mainCharacter.y+(aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale);

                    //If a walk event is being requested, initialize the walking system
                    if(aengine.startWalk == true){

                        //Don't indicate a walking cycle unless the intended path is clear
                        if((aengine.destX < feetX && (this.walkmapCollideEast() || ((aengine.mainCharacter.x - (8*aengine.mainCharacter.scale)) <= 0)))   ||
                           (aengine.destX >= feetX && (this.walkmapCollideWest()|| ((aengine.mainCharacter.x + (aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale) + (8*aengine.mainCharacter.scale)) >= 640)))  ||
                           (aengine.destY < feetY && (this.walkmapCollideNorth() || ((aengine.mainCharacter.y - (2*aengine.mainCharacter.scale)) <= 0)))  ||
                           (aengine.destY >= feetY && (this.walkmapCollideSouth() || ((aengine.mainCharacter.y + (aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale) + (2*aengine.mainCharacter.scale)) >= 360)))) {
                            walking = false;
                            aengine.mainCharacter.animationOn = false;
                            aengine.mainCharacter.setFrame(aengine.mainCharacter.getFrameCount());
                        }else{
                            walking = true;
                            aengine.startWalk = false;
                        }                    

                        //Set the horizontal and vertical direction of the
                        //character's motion
                        if(aengine.destX < feetX){
                                aengine.mainCharacter.mirrored = true;
                                Xdir = -1;
                        }else{
                                aengine.mainCharacter.mirrored = false;
                                Xdir = 1;
                        }
                        if(aengine.destY < feetY){
                                Ydir = -1;
                        }else{
                                Ydir = 1;
                        }

                    }

                    //Process a frame every 1/24 of a second
                    if(rightNow >= lastTime + 80) {
                        lastTime = rightNow;

            //----------------| PLAYER CHARACTER WALKING CYCLE |----------------------//
                            if(walking){

                                //Set the horizontal and vertical speeds based on character scale
                                xSpeed = Xdir*8*aengine.mainCharacter.scale;
                                ySpeed = Ydir*2*aengine.mainCharacter.scale;

                                //Increase or decrease the character's x and y if either is not yet at the
                                //value of the destination point
                                if( (xSpeed < 0 && feetX > aengine.destX) || (xSpeed > 0 && feetX < aengine.destX) ) {
                                    aengine.mainCharacter.x += xSpeed;
                                }
                                if( (ySpeed < 0 && feetY > aengine.destY) || (ySpeed > 0 && feetY < aengine.destY) ) {
                                    aengine.mainCharacter.y += ySpeed;
                                }

                                //Stop the walking cycle and execute the walkTo script if the character is 
                                //at the destination point
                                if((((ySpeed < 0 && feetY <= aengine.destY) || (ySpeed > 0 && feetY >= aengine.destY))
                                 &&((xSpeed < 0 && feetX <= aengine.destX) || (xSpeed > 0 && feetX >= aengine.destX)))) {
                                    walking = false;
                                    try{
                                        aengine.mainCharacter.animationOn = false;
                                        aengine.mainCharacter.setFrame(aengine.mainCharacter.getFrameCount());
                                        System.out.println(aengine.cbString);
                                        engine.eval(aengine.cbString);
                                    }catch(ScriptException e){
                                        System.out.println("walkTo callback is malformed.");
                                    }
                                }

                                //If the character collides with something, stop the walking
                                //cycle without executing the walkTo script
                                if((aengine.destX < feetX && (this.walkmapCollideEast() || ((aengine.mainCharacter.x - (8*aengine.mainCharacter.scale)) <= 0)))   ||
                                   (aengine.destX >= feetX && (this.walkmapCollideWest()|| ((aengine.mainCharacter.x + (aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale) + (8*aengine.mainCharacter.scale)) >= 640)))  ||
                                   (aengine.destY < feetY && (this.walkmapCollideNorth() || ((aengine.mainCharacter.y - (2*aengine.mainCharacter.scale)) <= 0)))  ||
                                   (aengine.destY >= feetY && (this.walkmapCollideSouth() || ((aengine.mainCharacter.y + (aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale) + (2*aengine.mainCharacter.scale)) >= 360)))) {
                                        walking = false;
                                        aengine.mainCharacter.animationOn = false;
                                        aengine.mainCharacter.setFrame(aengine.mainCharacter.getFrameCount());
                                }

                                //Scale the character based on how far away it is
                                aengine.mainCharacter.scale = (((aengine.mainCharacter.y+aengine.mainCharacter.imageData.getHeight())-360)/360.0)+1;
                            }

                        }
                    
                    }
                
                    //Draw the frame
                    canvas.repaint();
               
                    //Get and execute the collected scripts of all 
                    //elapsed Triggers from the tree
                    try{
                            tempScript = "";
                            if(aengine.processing == false) tempScript = aengine.processTriggers();                            
                            if(!tempScript.contentEquals("")) {
                                engine.eval(tempScript);
                            }
                    }catch(ScriptException e) {
                        System.out.println(e.getMessage());
                        System.out.printf("Engine encountered a malformed script.\n");
                    }
                
                    
                
                
                }
            }
            return;
        
    }
    
    //Pause/unpause the thread
    public void toggleRun(){
        this.halt = !this.halt;
    }
    
    //Basic constructor
    //Requires references to the other engine components
    public timeThread(InterfaceTree newAengine, ScriptEngine newEngine, JPanel newCanvas) {
        
        aengine = newAengine;
        engine = newEngine;
        canvas = newCanvas;
        lastTime = 0;
        walking = false;
        halt = false;
        
    }
    
    //Check if the character's feet are running into anything above them
    public boolean walkmapCollideNorth() {
        
        if(aengine.walkmap == null) return false;
        
        int startx, starty, endx, endy, x, y;
        
        startx = aengine.mainCharacter.x + 15;
        endx = (int)(aengine.mainCharacter.x + (aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale) -15);
        
        starty = (int)(aengine.mainCharacter.y + (aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale)-15);
        endy = starty + 6;
        
        for(x = startx; x <= endx; x++){
            for(y = starty; y <= endy; y++) {
                if(y <= 0) return true;
                if(((aengine.walkmap.getRGB(x, y) & 0xFFFFFF) == 0)|| (x >= 640) || (y >= 360)) return true;
            }
        }
        
        return false;
        
    }
    
    //Check if the character's feet are running into anything below them
    public boolean walkmapCollideSouth() {
        
        if(aengine.walkmap == null) return false;
        
        int startx, starty, endx, endy, x, y;
        
        startx = aengine.mainCharacter.x + 15;
        endx = (int)(aengine.mainCharacter.x + (aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale) - 15);
        
        starty = (int)(aengine.mainCharacter.y + (aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale)-6);
        endy = starty + 6;
        
        for(x = startx; x <= endx; x++){
            for(y = starty; y <= endy; y++) {
                if(y >= 360) return true;
                if(((aengine.walkmap.getRGB(x, y) & 0xFFFFFF) == 0) ) return true;
            }
        }
        
        return false;
        
    }
    
    //Check if the character's feet are running into anything to the right of them
    public boolean walkmapCollideEast() {
        
        if(aengine.walkmap == null) return false;
        
        int startx, starty, endx, endy, x, y;
        
        startx = aengine.mainCharacter.x+4;
        endx = startx+9;
        
        starty = (int)(aengine.mainCharacter.y + (aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale) - 9);
        endy = starty + 2;
        
        for(x = startx; x <= endx; x++){
            for(y = starty; y <= endy; y++) {
                if(x <= 0) return true;
                if(((aengine.walkmap.getRGB(x, y) & 0xFFFFFF) == 0)|| (x >= 640) || (y >= 360)) return true;
            }
        }
        
        return false;
        
    }
    
    //Check if the character's feet are running into anything to the left of them
    public boolean walkmapCollideWest() {
        
        if(aengine.walkmap == null) return false;
        
        int startx, starty, endx, endy, x, y;
        
        endx = (int)(aengine.mainCharacter.x + (aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale))-4;
        startx = endx - 9;
        
        starty = (int)(aengine.mainCharacter.y + (aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale) - 9);
        endy = starty + 2;
        
        for(x = startx; x <= endx; x++){
            for(y = starty; y <= endy; y++) {
                if(x >= 640) return true;
                if(((aengine.walkmap.getRGB(x, y) & 0xFFFFFF) == 0) || (x >= 640) || (y >= 360)) return true;
            }
        }
        
        return false;
        
    }
    
}
