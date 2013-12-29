package aengine2;

import java.util.*;
import javax.sound.midi.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class InterfaceTree {
    
    public boolean loadingText, readingText;
    public ArrayList things; 
    public ArrayList sounds;
    public ArrayList items;
    public ArrayList globals;
    public ArrayList textLines;
    public boolean interactive;
    public List triggers;
    private Sequencer sequencer;
    public Thing mainCharacter;
    public boolean startWalk;
    public String cbString;
    public int destX;
    public int destY;
    public BufferedImage walkmap;
    //public polygonMap walkmap;
    public Item activeItem;
    private String activeTool;
    public String level;
    public boolean processing;
    public int mousex, mousey;
    public int linePosition;
    
    
    public void setCharacter(Thing charThing){
        mainCharacter = charThing; 
        mainCharacter.looped = true;
        mainCharacter.setFrame(mainCharacter.getFrameCount()-1);
    }
    
    public void walkTo(int x, int y, String callback) {
        if(mainCharacter != null){
            mainCharacter.animationOn = true;
            destX = x;
            destY = y;
            cbString = callback;
            startWalk = true;
        }
    }
    
    public void printLine(String newText) {
        
        while(this.readingText == true);
        
        this.loadingText = true;
        System.out.println("Printing '".concat(newText).concat("'"));
        final int CHARS_PER_LINE = 42;
        int newLines;
        int i;
        
        String tempString;
        newLines = (int)Math.ceil(new Double(newText.length()) / CHARS_PER_LINE);
        
        
        for(i = 0; i < newLines; i++){
            if(i == newLines - 1){
                tempString = newText.substring(i*CHARS_PER_LINE);
            }else{
                tempString = newText.substring(i*CHARS_PER_LINE, (i+1)*CHARS_PER_LINE);
            }
            this.textLines.add(tempString);
        }
        
        this.linePosition = this.textLines.size() - newLines + 1;
        this.loadingText = false;
        
    }
    
    public void resetText() {
        this.textLines.clear();
        this.linePosition = 0;
    }
    
    public Thing makeThing(String newImage, String newHandle) {
        return new Thing(newImage, newHandle);
    }
    
    public void appendThing(Thing newThing) {
        this.things.add(newThing);
    }
    
    public Item makeItem(String newImage, String newHandle) {
        return new Item(newImage, newHandle);
    }
    
    public void appendItem(Item newItem) {
        this.items.add(newItem);
    }
    
    
    public void deleteThing(Thing delThing) {
        this.things.remove(delThing);
    }    
    
    public void deleteItem(Item delItem) {
        this.items.remove(delItem);
    }
    
    public void deleteSound(Sound delSound) {
        this.sounds.remove(delSound);
    }
    
    public Sound makeSound(String newSource, String newHandle) {
        return new Sound(this.sequencer, newSource, newHandle);
    }
    
    public void appendSound(Sound newSound) {
        this.sounds.add(newSound);
    }
    
    public void resetChildren(){
        this.triggers.clear();
        this.mainCharacter = null;
        this.sounds.clear();
        this.things.clear();
        this.walkmap = null;
        this.loadingText = false;
        this.resetText();
    }
    
    public InterfaceTree(Sequencer newSequencer) {
        this.things = new ArrayList();
        this.sounds = new ArrayList();
        this.items = new ArrayList();
        this.globals = new ArrayList();
        this.triggers = new ArrayList();
        this.textLines = new ArrayList();
        this.sequencer = newSequencer;
        this.walkmap = null;
        this.activeTool = "walk";
        this.activeItem = null;
        this.level = "";
        this.processing = false;
        this.mousex = 0;
        this.mousey = 0;
        this.linePosition = 0;
        this.loadingText = false;
        this.readingText = false;
        
    }
    
    public long now() {
        return new Date().getTime();
    }
    
    public void addTrigger(long time, String callback){
        triggers.add(new Trigger(time, callback));
    }
    
    public void loadWalkmap(String file) {
        try{    
            walkmap = ImageIO.read(new File(file));
        }catch(IOException e){
            walkmap = null;
        }
        //walkmap = new polygonMap(new File(file));
    }
    
    public String getTool() {
        return this.activeTool;
    }
    
    public Thing getThing(String name) {
        Iterator thingIterator = this.things.iterator();
        Thing tempThing;
        
        while(thingIterator.hasNext()){
            tempThing = (Thing)thingIterator.next();
            if(tempThing.handle.matches(name)) return tempThing;
        }
        
        return null;
        
    }    
    
    public Item getItem(String name){
        Iterator itemIterator = this.items.iterator();
        Item tempItem;
        
        while(itemIterator.hasNext()){
            tempItem = (Item)itemIterator.next();
            if(tempItem.handle.matches(name)) return tempItem;
        }
        
        return null;
                
    }
    
    public Sound getSound(String name){
        Iterator soundIterator = this.sounds.iterator();
        Sound tempSound;
        
        while(soundIterator.hasNext()){
            tempSound = (Sound)soundIterator.next();
            if(tempSound.handle.matches(name)) return tempSound;
        }
        
        return null;
        
    }
    
    public void setTool(Item newTool){
        
        this.activeItem = newTool;
        this.activeTool = this.activeItem.handle;
        
    }
    
    public void setTool(String newToolName){
        
        if(newToolName.matches("walk") || newToolName.matches("look") || newToolName.matches("use")) {
            this.activeTool = newToolName;
        }
        
    }
    
    public String processTriggers() {
        
        this.processing = true;
        
        Trigger tempTrigger;
        String returnString = "";
        Iterator triggerIterator = this.triggers.iterator();
                
                while(triggerIterator.hasNext()){
                    tempTrigger = (Trigger)triggerIterator.next();
                    if(this.now() >= ((Trigger)tempTrigger).time) {
                            triggerIterator.remove();
                            returnString = returnString.concat(tempTrigger.callback);
                   }
                }
       
       this.processing = false;
                
                return returnString;
              
                
    }
        
    public Global findGlobal(String seekHandle){
        Iterator globalIterator = this.globals.iterator();
        Global tempGlobal;
        
        while(globalIterator.hasNext()){
            tempGlobal = (Global)globalIterator.next();
            if(tempGlobal.getHandle().contentEquals(seekHandle)) 
                return tempGlobal;
        }
        
        return null;
    }
    
    public void makeGlobal(String newHandle) {
        Global tempGlobal;
        
        tempGlobal = this.findGlobal(newHandle);
        
        if(tempGlobal == null){
            tempGlobal = new Global(newHandle);
            this.globals.add(tempGlobal);
        }
        
    }
    
    public void makeGlobal(String newHandle, String newValue) {
        Global tempGlobal;
        
        tempGlobal = findGlobal(newHandle);
        
        if(tempGlobal == null){
            tempGlobal = new Global(newHandle, newValue);
            this.globals.add(tempGlobal);
        }else{
            tempGlobal.setValue(newValue);
        }
            
    }
    
    
    public String getGlobal(String seekHandle){
        
        Global tempGlobal = this.findGlobal(seekHandle);
        
        if(tempGlobal == null)
            return tempGlobal.value;
        else
            return "";
        
    }
    
    public void setGlobal(String seekHandle, String newValue){
        Global tempGlobal = this.findGlobal(seekHandle);
        
        if(tempGlobal != null){
            tempGlobal.setValue(newValue);
        }
        
    }
    
    public void setLevel(){
        
    }
    
}

