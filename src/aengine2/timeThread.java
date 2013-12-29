package aengine2;

import java.util.*;
import javax.script.*;
import javax.swing.*;

/**
 *
 * @author Joe
 */
public class timeThread extends Thread {
    
    private ScriptEngine engine;
    private java.util.List trig;
    private JPanel canvas;
    private double lastTime;
    private double xSpeed;
    private double ySpeed;
    private boolean walking;
    private InterfaceTree aengine;
    private boolean animToggle;
    private double feetX, feetY, Ydir, Xdir;
    private String tempScript;
    public boolean halt;
    private String iniCode;
    
    
    @Override public void run() {
             
        double rightNow;
        Thing tempThing; 
        int frame;
        
        
            while(!this.isInterrupted()) {
                if(aengine != null && engine != null){
                    if(aengine.mainCharacter != null) {
                    rightNow = new Date().getTime();

                    feetX = aengine.mainCharacter.x+((aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale)/2);
                    feetY = aengine.mainCharacter.y+(aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale);

                    if(aengine.startWalk == true){

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


                    if(rightNow >= lastTime + 80) {
                        lastTime = rightNow;

                            if(walking){

                                xSpeed = Xdir*8*aengine.mainCharacter.scale;
                                ySpeed = Ydir*2*aengine.mainCharacter.scale;


                                if( (xSpeed < 0 && feetX > aengine.destX) || (xSpeed > 0 && feetX < aengine.destX) ) {
                                    aengine.mainCharacter.x += xSpeed;
                                }

                                if( (ySpeed < 0 && feetY > aengine.destY) || (ySpeed > 0 && feetY < aengine.destY) ) {
                                    aengine.mainCharacter.y += ySpeed;
                                }

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

                                if((aengine.destX < feetX && (this.walkmapCollideEast() || ((aengine.mainCharacter.x - (8*aengine.mainCharacter.scale)) <= 0)))   ||
                                   (aengine.destX >= feetX && (this.walkmapCollideWest()|| ((aengine.mainCharacter.x + (aengine.mainCharacter.imageData.getWidth()*aengine.mainCharacter.scale) + (8*aengine.mainCharacter.scale)) >= 640)))  ||
                                   (aengine.destY < feetY && (this.walkmapCollideNorth() || ((aengine.mainCharacter.y - (2*aengine.mainCharacter.scale)) <= 0)))  ||
                                   (aengine.destY >= feetY && (this.walkmapCollideSouth() || ((aengine.mainCharacter.y + (aengine.mainCharacter.imageData.getHeight()*aengine.mainCharacter.scale) + (2*aengine.mainCharacter.scale)) >= 360)))) {
                                        walking = false;
                                        aengine.mainCharacter.animationOn = false;
                                        aengine.mainCharacter.setFrame(aengine.mainCharacter.getFrameCount());
                                }

                                aengine.mainCharacter.scale = (((aengine.mainCharacter.y+aengine.mainCharacter.imageData.getHeight())-360)/360.0)+1;
                            }

                        }
                    
                    }
                
                    canvas.repaint();
               
              
                    
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
    
    public void toggleRun(){
        this.halt = !this.halt;
    }
    
    public timeThread(InterfaceTree newAengine, ScriptEngine newEngine, JPanel newCanvas, String initCode) {
        
        aengine = newAengine;
        engine = newEngine;
        canvas = newCanvas;
        lastTime = 0;
        walking = false;
        animToggle = false;
        halt = false;
        iniCode = initCode;
        
    }
    
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
