package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    Core.java                                                                             *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This file represents the panel onto which the game playfield will be rendered and which     *
 * will intercept and process user input. The constructor initializes an InterfaceTree object,    *
 * which encapsulates the game state and is exposed to Rhino to allow for affecting game state    *
 * via the scripting interface. The constructor then accesses the root XML document containing    *
 * scene definition and scripting for the initial game level, populates the empty tree with the   *
 * objects defined within and finally hands the content of the first SCRIPT block found to Rhino  *
 * for execution. Also found in the constructor is construction and execution of a timeThread     *
 * runnable object which handles the accurate timing of animations and player motion tweening.    *
 *    There is an overridden paint method which contains the rendering loop of the engine,        *
 * displaying the Item and Thing objects found in the InterfaceTree.                              *
 *    There is also a child MouseListener class which is bound to the Core JPanel upon            *
 * construction in order to provide mouse state and location to the rendering and timing loops    *
 * and detect and trigger events caused by user mouse clicks.                                     *
 **************************************************************************************************/

//The core requires Rhino for script execution, DOM and XML libs for parsing the
//level files, AWT/Swing/ImageIO for rendering, MIDI libs for creating the global
//background music sequencer and IO and Util for general file access and such
import javax.script.*;
import com.sun.script.javascript.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.awt.*;
import java.io.*;
import java.awt.image.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.sound.midi.*;
import java.awt.event.*;
import java.awt.geom.*;

//The Core class, containing engine initialization and level parsing, creation of the InterfaceTree
//object, tracking and reaction to user input, and rendering of the game state to the Core JPanel itself
public class Core extends JPanel {

   private InterfaceTree aengine;       //The object which contains the game state and is exposed to scripting
   private RhinoScriptEngine engine;    //The Rhino Javascript engine which is used to interpret and execute the level scripts
   private BufferedImage offscreen;     //The working, non-displayed bitmap in the double-buffer scheme
   private Graphics2D g2d;              //Used to perform Graphics2D operations on the paint() loop's Graphics object
   private Sequencer midiEngine;        //Plays the cheezy background music loaded by the InterfaceTree
   private BufferedImage uiImage, walkButton, lookButton, useButton;    //Bitmaps for the UI elements (background + buttons)
   private BufferedImage itemHighlight, itemScrollDown, itemScrollUp;   //More UI bitmaps (item select rectangle, scroll arrows)
   private BufferedImage walkTool, lookTool, useTool, toolStub;         //Bitmaps for cursor images, built-in and Item-defined
   private Point mousePt;               //The location of the mouse as populated by the MouseListener
   private Point panelPt;               //Playfield top-left in desktop coordinates, subtracted from mousePt to get in-window coordinates
   private int mousex, mousey;          //mousePt less panelPt used in rendering and interface calculations
   private boolean animToggle;          //Limits animation updates to every other frame. Makes mouse look smoother while keeping 12fps animations
   private BufferedImage tempImg;       //Used to transform Thing and Item bitmaps and render them to the backbuffer
   private AffineTransform at;          //For performing scaling on the player character
   private AffineTransformOp scaleOp;   //For performing scaling on the player character
   private int scroll, maxScroll, txtScroll, maxTxtScroll;      //Parameters defining the state of the UI textbox
   private double lastTime;             //Timecode captured at the start of last frame, used to frame-limit render loop to 24fps
   private String lastLevel;            //Level file path captured from the InterfaceTree on previous render, used to trigger new level load
   private timeThread theTimer;         //The thread which handles global timing, triggering of render loop and player character path solving
   ScriptEngineManager manager;         //The object which spawns instances of Rhino
   
   //The render loop which draws the UI elements and the scene defined by the
   //InterfaceTree onto the Core JPanel
   @Override public void paint(Graphics g) {
       
            //Define a general-purpose indexing variable for use in the various loops below
            int i;                                              
       
            //Check the level file path stored in the interface tree (the aengine object) 
            //and trigger a new level load if it doesn't match the path of the currently
            //loaded level
            if(aengine.level != lastLevel)
                this.initTree(aengine.level);
            
    //----------| CURSOR POSITION CAPTURE AND CALCULATION |----------//
            
            //Capture or ignore the mouse position based on the interactivity
            //toggle set in the InterfaceTree
            if(aengine.interactive) {
                mousePt = MouseInfo.getPointerInfo().getLocation();
            }else{
                mousePt.x = 0;
                mousePt.y = 0;
            }
            
            //Get the desktop coordinates of the Core JPanel
            panelPt = this.getLocationOnScreen();
            
            //Convert the mouse location to window coordinates and push the
            //calculated values to the InterfaceTree
            mousex = mousePt.x-panelPt.x;
            mousey = mousePt.y-panelPt.y;
            aengine.mousex = mousex;
            aengine.mousey = mousey;
            
    //----------| RENDERING OF Thing OBJECTS TO PLAYFIELD |----------//
            
            //Initialize local variables used to draw 
            Graphics2D gScreen = (Graphics2D) g;                        //Cast the Graphics object to Graphics2D for the expanded capabilities
            Iterator thingIterator = aengine.things.listIterator();     //Used to cycle through the Thing objects in the InterfaceTree
            Thing tempThing;                                            //Used to cast the objects in the InterfaceTree's ArrayList
            int frame;                                                  //The current frame of the Thing being rendered
            
            //Sort the list of Things in the interface tree by their z-index
            //so that we can simply iterate through the Thing objects during
            //render and guarantee that they are drawn in the correct order
            //as to occlude each other as the scene defines it
            Collections.sort(aengine.things, new Comparator<Thing>(){
                @Override
                public int compare(Thing o1, Thing o2){
                    if(o1.z == o2.z)
                        return 0;
                    return o1.z > o2.z ? 1 : -1;
                }
            });
                      
            //Start the fresh Thing render only if it has been 1/24 of a second since last render
            if(aengine.now() >= lastTime + 80){
                
                //Update the most recent render time
                lastTime = aengine.now();
                
                //Cycle through every Thing currently in the InterfaceTree
                while(thingIterator.hasNext()){
                    
                   //Cast the next Thing in the list to a working reference
                   tempThing = (Thing)thingIterator.next();  
                   
                   //Don't bother with any processing on the Thing if we can't see it
                   if(!tempThing.hidden){
                   
                       //If the current Thing is the player character and it's 
                       //in the middle of walking we should auto-increment the
                       //frame number
                       //We do this in a block seperate from the standard Thing
                       //animation due to the special properties of the player
                       //character -- notably that it defaults to its idle frame
                       //when not in motion and that the idle frame is defined to 
                       //be the last frame. Therefore, we make sure to increment
                       //before we render so that if the walk was just initialized
                       //that the Thing is out of idle by the time it is drawn and 
                       //we cycle back to frame 0 one frame early so that we don't
                       //pass through the idle frame
                       if(tempThing.equals(aengine.mainCharacter) && tempThing.animationOn){
                            frame = tempThing.getFrame()+1;
                            if(frame > tempThing.getFrameCount() - 2) {
                                frame = 0;
                            }
                            tempThing.setFrame(frame);
                        }

                       //If the current Thing is mirrored, create a new AffineTransform
                       //set to mirror it. Otherwise, just create a blank AffineTransform
                       if(tempThing.mirrored){
                            at = AffineTransform.getScaleInstance(-1, 1);
                            at.translate(-tempThing.imageData.getWidth(null), 0);
                       }else{
                            at = new AffineTransform();
                       }

                       //Use the new AffineTransform to apply the Things scale ratio to
                       //the bitmap to be rendered
                       at.scale(tempThing.scale, tempThing.scale);
                       scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                       tempImg = scaleOp.createCompatibleDestImage(tempThing.imageData, null);
                       scaleOp.filter(tempThing.imageData, tempImg);

                       //Finally, copy the modified bitmap to the screen
                       this.g2d.drawImage(tempImg, tempThing.x, tempThing.y, null); 

                       //And progress the frame of the thing if it's animated
                       //(and, importantly, *not* the player character)
                       if(!tempThing.equals(aengine.mainCharacter) && tempThing.animationOn){
                            frame = tempThing.getFrame()+1;
                            if(frame > tempThing.getFrameCount() - 1) {
                                
                                //Make sure we kill the animation if we hit the
                                //end and the thing isn't looped
                                if(tempThing.looped){
                                    frame = 0;
                                }else{
                                    frame = tempThing.getFrameCount() - 1;
                                    tempThing.animationOn = false;
                                }
                            }
                            tempThing.setFrame(frame);
                        }
                   }
                   
                }
                
    //----------| RENDERING OF THE UI OVER THE FINISHED PLAYFIELD |----------//
                
                //Slap the main UI 'drawer' image to the bottom of the screen
                this.g2d.drawImage(this.uiImage, 0, 360, null);
 
    //----------| RENDERING OF THE Item OBJECTS INTO THE INVENTORY BOX |----------//
                
                //Initialization of local variables used in the rendering of the 
                //UI's inventory box
                Iterator itemIterator = aengine.items.iterator();   //Used to cycle through the InterfaceTree's item list
                int itemCounter = 0;                                //Tracks the index of the item being processed
                int itemX = 519;                                    //Initial point in the window to begin placing the items
                int itemY = 377;                                    //Initial point in the window to begin placing the items
                Item tempItem;                                      //Used to cast items in the InterfaceTree ArrayList to Item objects
                boolean drawBorder = false;                         //The mouse is or is not over one of the items in the item box.
                
                //Calculate the number of rows the user should be able
                //to scroll down in the inventory based on the number
                //of Item objects in the InterfaceTree
                maxScroll = (int)Math.ceil((aengine.items.size()-1) / 2) - 1;
                
                //Cycle through and ignore all of the Item objects
                //preceeding the first item in the current topmost 
                //visible row of the inventory box
                for(i = 0; i < (2*scroll) && itemIterator.hasNext(); i++)
                    tempItem = (Item)itemIterator.next();
                
                //Get up to the next four Item objects, if available,
                //and draw their bitmaps to the item box, increasing
                //the working placement coordinates accordingly
                for( ; itemIterator.hasNext() && i < (2*scroll) + 4; i++ ){
                    tempItem = (Item)itemIterator.next();
                    this.g2d.drawImage(tempItem.imageData, itemX, itemY, null);
                    
                    itemX += 46;
                    if(itemX == 611){
                        itemX = 519;
                        itemY += 46;
                    }
                }
                
                //The following mass of conditionals simply checks to see if the
                //mouse coordinates fall into the bounds of each of the items
                //displayed in the inventory box in turn and, if they do, if
                //that slot is occupied. If both of these conditions is met
                //the highlight image is rendered over that position
                if(mousex >= 517 && mousex <= 562){
                    if(mousey >= 375 && mousey <= 419){
                        try{
                            aengine.items.get(scroll*2);
                            drawBorder = true;
                        }catch(IndexOutOfBoundsException e){
                            drawBorder = false;
                        }
                        if(drawBorder)
                            this.g2d.drawImage(this.itemHighlight, 517, 375, null);
                    }
                    if(mousey >= 421 && mousey <= 467) {
                        try{
                            aengine.items.get((scroll+1)*2);
                            drawBorder = true;
                        }catch(IndexOutOfBoundsException e){
                            drawBorder = false;
                        }
                        if(drawBorder)
                            this.g2d.drawImage(this.itemHighlight, 517, 421, null);
                    }
                }
                if(mousex >= 563 && mousex <= 608){
                    if(mousey >= 375 && mousey <= 419){
                        try{
                            aengine.items.get((scroll*2)+1);
                            drawBorder = true;
                        }catch(IndexOutOfBoundsException e){
                            drawBorder = false;
                        }
                        if(drawBorder)    
                            this.g2d.drawImage(this.itemHighlight, 563, 375, null);
                    }
                    if(mousey >= 421 && mousey <= 467) {
                        try{
                            aengine.items.get(((1+scroll)*2)+1);
                            drawBorder = true;
                        }catch(IndexOutOfBoundsException e){
                            drawBorder = false;
                        }
                        if(drawBorder)
                            this.g2d.drawImage(this.itemHighlight, 563, 421, null);
                    }
                }
                
    //----------| RENDERING OF THE DIALOG TEXT INTO THE UI TEXT BOX |----------//
                
    //NOTE: This area requires fixes as the properties of the monospaced font 
    //varies from platform to platform causing lines to be spaced oddly and/or
    //fall outside of the text box. 
    //Current proposal is to replace the Graphics2D text routines with a simple
    //custom renderer based on a custom bitmap font
                
                //This is a simple lock to prevent simultaneous access 
                //to the collection of text lines in the InterfaceTree
                //by the rendering routine here and the text setter called
                //by the current script
                if(aengine.loadingText == false) {
                    
                    //We gained access, lock out others while we work
                    aengine.readingText = true;
                    
                    //Set up the local variables we'll use in rendering the text
                    Iterator textIterator = aengine.textLines.listIterator();   //Used to cycle through the lines of text in the InterfaceTree
                    String tempString;                                          //The string value of the line being operated upon

                    //Skip through all of the lines preceeding the topmost line
                    //visible in the textbox
                    for(i = 0; i < aengine.linePosition-1 && textIterator.hasNext(); i++){
                         textIterator.next();
                    }

                    //Set up the font, red and monospaced
                    this.g2d.setColor(new Color(255,0,0));
                    this.g2d.setFont(new Font("Monospaced", Font.PLAIN, 15));

                    //Get the next five lines, if availible, and print them to
                    //the textbox with increasing y-index
                    for( ; textIterator.hasNext() && i < aengine.linePosition + 5; i++ ){
                        tempString = (String)textIterator.next();
                        this.g2d.drawString(tempString, 106, 400+((i-aengine.linePosition)*g2d.getFontMetrics().getHeight()));
                    }
                    
                    //We're done accesing the list of lines, so we'll release our lock
                    aengine.readingText = false;
                }
                
    //----------| RENDERING OF HIGHLIGHTED UI BUTTONS |----------//
                
                //Check to see if the mouse coordinates overlap any of
                //the action buttons and, if so, superimpose the
                //highlighted button image over the base UI image
                if(mousex <= 91 && mousex >= 6) {
                    if(mousey >= 366 && mousey <= 398) {
                        this.g2d.drawImage(this.walkButton, 6, 366, null);
                    }
                    if(mousey >= 404 && mousey <= 438) {
                        this.g2d.drawImage(this.lookButton, 6, 404, null);
                    }
                    if(mousey >= 440 && mousey <= 475) {
                        this.g2d.drawImage(this.useButton, 6, 440, null);
                    }
                }
                
                //Check to see if the mouse coordinates overlap either of
                //the text scroll buttons and if there are lines to view in
                //that direction
                //If so, superimpose the highlighted button image over the
                //base UI image
                if(mousex <= 505 && mousex >= 489){
                    if(mousey >= 372 && mousey <= 383 && aengine.linePosition > 1)
                        this.g2d.drawImage(this.itemScrollUp, 489, 372, null);
                    
                    if(mousey >= 457 && mousey <= 468 && aengine.linePosition + 5 < aengine.textLines.size())
                        this.g2d.drawImage(this.itemScrollDown, 489, 457, null);
                }
                
                //Check to see if the mouse coordinates overlap either of
                //the item scroll buttons and if there are items to view in
                //that direction
                //If so, superimpose the highlighted button image over the
                //base UI image
                if(mousex <= 625 && mousex >= 609){
                    if(mousey >= 372 && mousey <= 383 && scroll > 0)
                        this.g2d.drawImage(this.itemScrollUp, 609, 372, null);
                    
                    if(mousey >= 457 && mousey <= 468 && scroll < maxScroll) 
                        this.g2d.drawImage(this.itemScrollDown, 609, 457, null);
                }
                
    //----------| RENDERING OF THE CURSOR |----------//
                
                //If the engine is set to non-interactive mode (eg: in the middle
                //of a cutscene) then don't render the cursor to the screen at all
                if(aengine.interactive) {
                    
                    //Render the cursor image appropriate to the currently selected tool
                    if(aengine.getTool().matches("walk")) {
                        this.g2d.drawImage(this.walkTool, mousex, mousey, null);
                    }else if(aengine.getTool().matches("look")){
                        this.g2d.drawImage(this.lookTool, mousex, mousey, null);
                    }else if(aengine.getTool().matches("use")) {
                        this.g2d.drawImage(this.useTool, mousex, mousey, null);
                    }else{
                        //If the selected tool is an item, render the pointer and then the
                        //item image below it
                        this.g2d.drawImage(aengine.activeItem.imageData, mousex + 4, mousey + 4, null);
                        this.g2d.drawImage(this.toolStub, mousex, mousey, null);
                    }
                    
                }
                
            }
               
            //Indicate, for the sake of 12fps animations, that the next
            //frame will or will not be an 'update' frame
            animToggle = !animToggle;
                       
            //Put the finished backbuffer on the JFrame
            gScreen.drawImage(this.offscreen, 0, 0, null);      
   } 
   
   
   //This is the constructor of the Core object which initializes the
   //state of the engine, installs the MIDI sequencer, Mouse Listener 
   //and timing thread, and finally loads the root level file located
   //at 'game/start.xml'
   public Core() {
                
                //Initialize the UI and render state
                animToggle = false;
                scroll = 0;
                maxScroll = 0;
                lastTime = 0;
       
                //Create a 1x1px invisible image and apply it to the
                //application cursor in order to hide it when it is
                //over the game canvas
                Image img = createImage(new MemoryImageSource(1, 1, new int[2], 0, 0));
                Toolkit kit = Toolkit.getDefaultToolkit();
                Cursor cursor = kit.createCustomCursor(img, new java.awt.Point(0,0), "Transparent");
                setCursor(cursor);
                
                //Initialize the MIDI sequencer
                //NOTE: Apparently they removed the software synth from
                //CoreAudio in Mavericks, so no more cheezy MIDI on OSX
                //unless we go to the touble of writing our own sampler!
                try{
                    this.midiEngine = MidiSystem.getSequencer();
                    midiEngine.open();
                }catch(MidiUnavailableException e){
                    System.out.printf("Midi is unsupported on this system!");
                }
               
                //Add an event listener to the MIDI sequencer to either unload
                //the track or restart it, if it is set to loop, when it completes
                midiEngine.addMetaEventListener(new MetaEventListener() {
                    public void meta(MetaMessage msg) {
                        if (msg.getType() == 0x2F) { // End of track
                           // Restart the song
                          Iterator soundIterator = aengine.sounds.iterator();
                          Sound tempSound = null;
                          boolean looping = false;
                          while(soundIterator.hasNext()){
                              tempSound = (Sound)soundIterator.next();
                              if(tempSound.playing){
                                  looping = tempSound.loop;
                                  break;
                              }
                                  
                          }
                          if(looping){
                                midiEngine.setTickPosition(0);
                                midiEngine.start();
                          }
                        }
                    }
                });
                
                //Install the mouse handler
                this.addMouseListener(new thisMouseListener());
                
                //Start the global timing thread               
                theTimer = new timeThread(aengine, engine, this, "");
                theTimer.start();
               
                //Load the UI skin from the game folder
                try{
                    this.uiImage = ImageIO.read(new File("game/global/ui/ui.png"));
                    this.walkTool = ImageIO.read(new File("game/global/ui/walktool.png"));
                    this.lookTool = ImageIO.read(new File("game/global/ui/looktool.png"));
                    this.useTool = ImageIO.read(new File("game/global/ui/usetool.png"));
                    this.toolStub = ImageIO.read(new File("game/global/ui/toolstub.png"));
                    this.walkButton = ImageIO.read(new File("game/global/ui/walkbutton.png"));
                    this.lookButton = ImageIO.read(new File("game/global/ui/lookbutton.png"));
                    this.useButton = ImageIO.read(new File("game/global/ui/usebutton.png"));
                    this.itemHighlight = ImageIO.read(new File("game/global/ui/itemhighlight.png"));
                    this.itemScrollUp = ImageIO.read(new File("game/global/ui/scrollup.png"));
                    this.itemScrollDown = ImageIO.read(new File("game/global/ui/scrolldown.png"));
                }catch(IOException e){
                    System.out.println("Could not open ui images.");
                }
                
                //Set the size of the canvas
                this.setPreferredSize( new Dimension( 640, 480 ) );
                
                //Create the front and back buffers
                this.offscreen = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
                this.g2d = (Graphics2D)this.offscreen.createGraphics();
                
                //Create the InterfaceTree which will hold the game state
                //and be exposed to the scripting engine 
                this.aengine = new InterfaceTree(this.midiEngine);
                
                //This is the object from which our scripting engine will be spawned
                manager = new ScriptEngineManager();
                
                //Load the initial level               
                this.initTree("game/start.xml");
                
	}
   
        //This simple method gets the value of the first tag found in the
        //provided element which has the name passed in sTag
	private static String getTagValue(String sTag, Element eElement) {
            if(eElement.getElementsByTagName(sTag).getLength() > 0){
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = (Node) nlList.item(0);
		if(nValue != null){
                    return nValue.getNodeValue();
                }else{
                    return null;
                }
            }else{
                return null;
            }
	}
        
        //Open the XML file at the location stored in the level string,
        //create the objects defined within, appending them to the 
        //InterfaceTree. Finally, find and execute the code in the first SCRIPT tag
        private void initTree(String level){
                
                System.out.println("Resetting the tree.");
            
                //If the timing thread is active, message it to quit and 
                //wait until it has stopped executing
                if(theTimer != null) {
                    aengine.processing = true;
                    theTimer.interrupt();
                    while(theTimer.isAlive()) {}
                    theTimer = null;
                }
            
                //Define the local working variables
                NodeList tempList;      //Used to store and iterate through a collection of similar tags
                Node tempNode;          //Used to process the content of the current object block
                int i;                  //General purpose index variable
                Thing tempThing;        //A container in which to build a new Thing object
                Sound tempSound;        //A container in which to build a new Sound object
                String tempString, handleString, imageString;       //Used to get the value of the tags in the current object block
                Element eElement;       //The current attribute tag from the working object block
                
                //Clear the state of the InterfaceTree, excepting Global objects
                aengine.resetChildren();              
                                
                try {
                    
                    //Parse the XML file into a DOM tree and get the data stored in the LEVEL block
                    File xmlFile = new File(level);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(xmlFile);
                    Node docBody = doc.getElementsByTagName("level").item(0);
                    doc.getDocumentElement().normalize();
                    eElement = doc.getDocumentElement();
                    
                    //Find and process all THING object blocks
                    tempList = eElement.getElementsByTagName("thing");
                    for(i = 0; i < tempList.getLength(); i++) {
                        
                        //Get the image and handle with which to create the object
                        imageString = getTagValue("image", (Element)tempList.item(i));
                        if(imageString != null) {
                            handleString = getTagValue("handle", (Element)tempList.item(i));
                            if(handleString != null) {
                                
                                //Create the initial object
                                tempThing = aengine.makeThing(imageString, handleString);
                                
                                //Try to get the value of the X tag
                                tempString = getTagValue("x", (Element)tempList.item(i));
                                if(tempString != null) tempThing.x = Integer.parseInt(tempString);
                                
                                //Try to get the value of the Y tag
                                tempString = getTagValue("y", (Element)tempList.item(i));
                                if(tempString != null) tempThing.y = Integer.parseInt(tempString);
                                
                                //Try to get the value of the Z tag
                                tempString = getTagValue("z", (Element)tempList.item(i));
                                if(tempString != null) tempThing.z = Integer.parseInt(tempString);
                                
                                //Try to get the value of the HIDDEN tag
                                tempString = getTagValue("hidden", (Element)tempList.item(i));
                                if(tempString != null){System.out.println(tempString); tempThing.hidden = Boolean.parseBoolean(tempString);}
                                
                                //Try to get the value of the PLAYING tag
                                tempString = getTagValue("playing", (Element)tempList.item(i));
                                if(tempString != null) tempThing.animationOn = Boolean.parseBoolean(tempString);
                                
                                //Try to get the value of the LOOPED tag
                                tempString = getTagValue("looped", (Element)tempList.item(i));
                                if(tempString != null) tempThing.looped = Boolean.parseBoolean(tempString);
                                
                                ////Try to get the value of the MIRRORED tag
                                tempString = getTagValue("mirrored", (Element)tempList.item(i));
                                if(tempString != null) tempThing.mirrored = Boolean.parseBoolean(tempString);
                                
                                //Try to get the value of the SCALE tag
                                tempString = getTagValue("scale", (Element)tempList.item(i));
                                if(tempString != null) tempThing.scale = Double.parseDouble(tempString);
                                
                                //Try to get the value of the ONCLICK tag
                                tempString = getTagValue("onclick", (Element)tempList.item(i));
                                if(tempString != null) tempThing.onClick = tempString;
                                
                                //Add the completed Thing object to the InterfaceTree
                                aengine.appendThing(tempThing);
                            } 
                        }
                    }
                                
                    //Find and process all SOUND object blocks
                    tempList = eElement.getElementsByTagName("sound");
                    for(i = 0; i < tempList.getLength(); i++) {
                        
                        //Get the file path and handle with which to create the new object
                        imageString = getTagValue("file", (Element)tempList.item(i));
                        if(imageString != null) {
                            handleString = getTagValue("handle", (Element)tempList.item(i));
                            if(handleString != null) {
                                
                                //Create the Sound object, attempt to get its LOOPED tag value
                                //and finally add the new Sound object to the InterfaceTree
                                tempSound = aengine.makeSound(imageString, handleString);
                                tempString = getTagValue("looped", (Element)tempList.item(i));
                                if(tempString != null) tempSound.loop = Boolean.parseBoolean(tempString);
                                aengine.appendSound(tempSound);
                                
                            }
                        }
                    }
                    
                    //Attempt to find a WALKMAP tag and load the associated file
                    tempString = getTagValue("walkmap", eElement);
                    if(tempString != null) aengine.loadWalkmap(tempString);
                    
                    //Save the path to the level that was just loaded
                    this.lastLevel = aengine.level;
                    
                    //Create a new Rhino instance, expose the InterfaceTree to it
                    //and finally find and execute the first SCRIPT block in the document
                    engine = null;
                    engine = (RhinoScriptEngine)manager.getEngineByName("JavaScript");
                    engine.put("AEngine2", this.aengine);
                    engine.eval(getTagValue("script", eElement));
                    
                    //Restart the timing thread
                    theTimer = new timeThread(aengine, engine, this, "");
                    theTimer.start();
                                        
                }catch(ParserConfigurationException e) {
                    System.out.printf("There was an error setting up the JavaScript parser.\n");
                }catch(SAXException e) {
                    System.out.printf("Engine encountered a malformed sax.\n");
                }catch(ScriptException e){
                    System.out.printf("Engine encountered a malformed script.\n");
                }catch(IOException e) {
                    System.out.printf("IOError.\n");
                }
                
                //Indicate that the level is finished loading
                aengine.processing = false;
        }
        
        
        //This is the mouse event listener which is attached to the Core JPanel
        //in order to detect and handle user mouse clicks
        private class thisMouseListener implements MouseListener {
                
                    //Set up the local variables used to process the mouse input
                    ArrayList underMouse;           //Collection of Thing objects beneath the user's cursor
                    Iterator thingIterator;         //Used to cycle through all Thing objects in the scene
                    ListIterator underIterator;     //Used to cycle through underMouse
                    Item tempTool;                  //Used to get the item the user selected from the inventory
                    Thing tempThing;                //The Thing object the user has clicked upon
                    boolean itemExists = false;     //Whether or not the item slot the user selected is populated
            
                    //We track the mouse's position in the rendering loop, so we just need to 
                    //handle mouse clicks in the mouse handler
                    @Override public void mouseClicked(MouseEvent event) {
                        
                        //Don't pay attention to mouse clicks at all if we're non-interactive 
                        if(aengine.interactive) {
                            
                            //Check if the moues is in the UI or the playfield
                            if(mousey < 360) {
                               
                               //Check the boundary of every Thing object and, if the mouse 
                               //coordinates lie within them, add it to an ArrayList
                               thingIterator = aengine.things.iterator();
                               underMouse = new ArrayList();
                               while(thingIterator.hasNext()){
                                   tempThing = (Thing)thingIterator.next();
                                   if((mousey >= tempThing.y) && (mousey <= (tempThing.imageData.getHeight() + tempThing.y)) && (mousex >= tempThing.x) && (mousex <= (tempThing.imageData.getWidth() + tempThing.x))){
                                       underMouse.add(tempThing);
                                   }
                               }
                               
                               //Iterate backwards through the list we just created and retrieve the
                               //first object encountered which does not have a transparent pixel at
                               //the mouse cursor coordiates. Then we use the Rhino instance to execute
                               //the script contained in the object's onClick string
                               //We iterate backwards through the list because it inherits the back to
                               //front of screen organization in which the parent list was placed by the
                               //rendering loop and we want to instead find the frontmost, which is
                               //therefore at the end of the list
                               thingIterator = null;
                               underIterator = underMouse.listIterator(underMouse.size());
                               while(underIterator.hasPrevious()){
                                   tempThing = (Thing)underIterator.previous();
                                   if(!tempThing.hidden){
                                        if((tempThing.imageData.getRGB(mousex - tempThing.x, mousey - tempThing.y) & 0xFF000000) != 0){
                                            if(!tempThing.onClick.contentEquals("")){
                                                try{
                                                    engine.eval(tempThing.onClick);
                                                }catch(ScriptException e){
                                                    System.out.println("Encountered a malformed script.");
                                                }
                                                break;
                                            }
                                        }
                                   }
                               }
                            }else{

                               //The mouse is below the playfield, so we start checking
                               //to see if it overlaps with any UI elements
                               //NOTE: Most of this overlap detection code replicates
                               //the same code used to highlight the UI elements in the
                               //rendering loop, which is not exactly DRY and could be
                               //perhaps replaced with a bool set in the rendering loop
                               //if the mouse was over a given button. However, these 
                               //simple conditionals do not pose an especially grevious
                               //time penalty, so it's not a primary concern
                                
                               //First we look at the inventory box and, if the mouse
                               //overlaps one of the item slots, we try to set the 
                               //current tool to the item in that slot if there is one
                               if(mousex >= 517 && mousex <= 562){
                                    if(mousey >= 375 && mousey <= 419){
                                        try{
                                            tempTool = (Item)aengine.items.get(scroll*2);
                                            itemExists = true;
                                        }catch(IndexOutOfBoundsException e){
                                            itemExists = false;
                                        }
                                        if(itemExists)
                                            aengine.setTool(tempTool);
                                    }
                                    if(mousey >= 421 && mousey <= 467) {
                                        try{
                                            tempTool = (Item)aengine.items.get((scroll+1)*2);
                                            itemExists = true;
                                        }catch(IndexOutOfBoundsException e){
                                            itemExists = false;
                                        }
                                        if(itemExists)
                                            aengine.setTool(tempTool);
                                    }
                                }
                                if(mousex >= 563 && mousex <= 608){
                                    if(mousey >= 375 && mousey <= 419){
                                        try{
                                            tempTool = (Item)aengine.items.get((scroll*2)+1);
                                            itemExists = true;
                                        }catch(IndexOutOfBoundsException e){
                                            itemExists = false;
                                        }
                                        if(itemExists)    
                                            aengine.setTool(tempTool);
                                    }
                                    if(mousey >= 421 && mousey <= 467) {
                                        try{
                                            tempTool = (Item)aengine.items.get(((1+scroll)*2)+1);
                                            itemExists = true;
                                        }catch(IndexOutOfBoundsException e){
                                            itemExists = false;
                                        }
                                        if(itemExists)
                                            aengine.setTool(tempTool);
                                    }
                                } 

                               //Check to see if the mouse was within the bounds of the
                               //inventory scrollbars and, if there are more items to be
                               //displayed in that direction, increase or decrease the 
                               //scrolled line number accordingly
                               if(mousex <= 625 && mousex >= 609){
                                    if(mousey >= 372 && mousey <= 383 && scroll > 0)
                                        scroll--;

                                    if(mousey >= 457 && mousey <= 468 && scroll < maxScroll){
                                        scroll++;
                                    }
                               }

                               //Check to see if the mouse was within the bounds of the
                               //textbox scrollbars and, if there are more lines to be
                               //displayed in that direction, increase or decrease the 
                               //scrolled line number accordingly
                               if(mousex <= 505 && mousex >= 489){
                                    if(mousey >= 372 && mousey <= 383 && aengine.linePosition > 1)
                                        aengine.linePosition--;

                                    if(mousey >= 457 && mousey <= 468 && aengine.linePosition + 5 < aengine.textLines.size())
                                        aengine.linePosition++;
                               }

                               //Check to see if the mouse lies within the boundary of any of the 
                               //action buttons and, if so, set the appropriate tool
                               if(mousex <= 91 && mousex >= 6) {
                                    if(mousey >= 366 && mousey <= 398) {
                                        aengine.setTool("walk");
                                    }
                                    if(mousey >= 404 && mousey <= 438) {
                                        aengine.setTool("look");
                                    }
                                    if(mousey >= 440 && mousey <= 475) {
                                        aengine.setTool("use");
                                    }
                               }
                               
                            }
                        }
                    } 
                    
                    //The below overrides are required only to keep NetBeans happy
                    //and are not strictly required
                    
                    @Override public void mousePressed(MouseEvent event) {
                    }
                    
                    @Override public void mouseReleased(MouseEvent event) {
                    }
                    
                    @Override public void mouseEntered(MouseEvent event) {
                    }
                    
                    @Override public void mouseExited(MouseEvent event) {
                    } 
        }
        
        
}
