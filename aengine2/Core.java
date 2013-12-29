package aengine2;

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


/**
 *
 * @author Joe
 */
public class Core extends JPanel {

   private InterfaceTree aengine;  
   private RhinoScriptEngine engine;
   private BufferedImage offscreen;
   private Graphics2D g2d;
   private Sequencer midiEngine;
   private BufferedImage uiImage, walkButton, lookButton, useButton, itemHighlight, itemScrollDown, itemScrollUp;  
   private BufferedImage walkTool, lookTool, useTool, toolStub;
   private Point mousePt;
   private Point panelPt;
   private int mousex, mousey;
   private boolean animToggle;
   private BufferedImage tempImg;
   private AffineTransform at;
   private AffineTransformOp scaleOp;
   private int scroll, maxScroll, txtScroll, maxTxtScroll;
   private double lastTime;
   private String lastLevel;
   private timeThread theTimer;
   ScriptEngineManager manager;
   
   
   @Override public void paint(Graphics g) {
       
            if(aengine.level != lastLevel)
                this.initTree(aengine.level);
       
            Iterator itemIterator = aengine.items.iterator();
            int itemCounter = 0;
            int itemX = 519;
            int itemY = 377;
            int i;
            Item tempItem;
            boolean drawBorder = false;
            mousePt = MouseInfo.getPointerInfo().getLocation();
            panelPt = this.getLocationOnScreen();
            mousex = mousePt.x-panelPt.x;
            mousey = mousePt.y-panelPt.y;
            
            aengine.mousex = mousex;
            aengine.mousey = mousey;
            
            Collections.sort(aengine.things, new Comparator<Thing>(){
                @Override
                public int compare(Thing o1, Thing o2){
                    if(o1.z == o2.z)
                        return 0;
                    return o1.z > o2.z ? 1 : -1;
                }
            });
            
            Graphics2D gScreen = (Graphics2D) g;
            Iterator thingIterator = aengine.things.listIterator();
            Thing tempThing; 
            int frame;
           
            if(aengine.now() >= lastTime + 80){
                lastTime = aengine.now();
                
                while(thingIterator.hasNext()){
                   tempThing = (Thing)thingIterator.next();  
                   
                   if(tempThing.equals(aengine.mainCharacter) && tempThing.animationOn){
                        frame = tempThing.getFrame()+1;
                        if(frame > tempThing.getFrameCount() - 2) {
                            frame = 0;
                        }
                        tempThing.setFrame(frame);
                    }
                   
                   if(tempThing.mirrored){
                        at = AffineTransform.getScaleInstance(-1, 1);
                        at.translate(-tempThing.imageData.getWidth(null), 0);
                   }else{
                        at = new AffineTransform();
                   }
                   at.scale(tempThing.scale, tempThing.scale);
                   scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                   tempImg = scaleOp.createCompatibleDestImage(tempThing.imageData, null);
                   scaleOp.filter(tempThing.imageData, tempImg);
             
                   if(!tempThing.hidden)
                      this.g2d.drawImage(tempImg, tempThing.x, tempThing.y, null); 
                   
                   if(!tempThing.equals(aengine.mainCharacter) && tempThing.animationOn){
                        frame = tempThing.getFrame()+1;
                        if(frame > tempThing.getFrameCount() - 1) {
                            frame = 0;
                        }
                        tempThing.setFrame(frame);
                    }
                   
                }
                
                this.g2d.drawImage(this.uiImage, 0, 360, null);
                
                maxScroll = (int)Math.ceil((aengine.items.size()-1) / 2) - 1;
                
                for(i = 0; i < (2*scroll) && itemIterator.hasNext(); i++)
                    tempItem = (Item)itemIterator.next();
                
                for( ; itemIterator.hasNext() && i < (2*scroll) + 4; i++ ){
                    tempItem = (Item)itemIterator.next();
                    this.g2d.drawImage(tempItem.imageData, itemX, itemY, null);
                    
                    itemX += 46;
                    if(itemX == 611){
                        itemX = 519;
                        itemY += 46;
                    }
                }
                
                
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
                
                if(aengine.loadingText == false) {
                    aengine.readingText = true;
                    Iterator textIterator = aengine.textLines.listIterator();
                    String tempString;

                    for(i = 0; i < aengine.linePosition-1 && textIterator.hasNext(); i++){
                         textIterator.next();
                    }

                    this.g2d.setColor(new Color(255,0,0));
                    this.g2d.setFont(new Font("Monospaced", Font.PLAIN, 15));

                    for( ; textIterator.hasNext() && i < aengine.linePosition + 5; i++ ){

                        tempString = (String)textIterator.next();
                        this.g2d.drawString(tempString, 106, 400+((i-aengine.linePosition)*g2d.getFontMetrics().getHeight()));

                    }
                    aengine.readingText = false;
                }
                
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
                
                if(mousex <= 505 && mousex >= 489){
                    if(mousey >= 372 && mousey <= 383 && aengine.linePosition > 1)
                        this.g2d.drawImage(this.itemScrollUp, 489, 372, null);
                    
                    if(mousey >= 457 && mousey <= 468 && aengine.linePosition + 5 < aengine.textLines.size())
                        this.g2d.drawImage(this.itemScrollDown, 489, 457, null);
                }
                
                if(mousex <= 625 && mousex >= 609){
                    if(mousey >= 372 && mousey <= 383 && scroll > 0)
                        this.g2d.drawImage(this.itemScrollUp, 609, 372, null);
                    
                    if(mousey >= 457 && mousey <= 468 && scroll < maxScroll) 
                        this.g2d.drawImage(this.itemScrollDown, 609, 457, null);
                }
                
                if(aengine.getTool().matches("walk")) {
                    this.g2d.drawImage(this.walkTool, mousex, mousey, null);
                }else if(aengine.getTool().matches("look")){
                    this.g2d.drawImage(this.lookTool, mousex, mousey, null);
                }else if(aengine.getTool().matches("use")) {
                    this.g2d.drawImage(this.useTool, mousex, mousey, null);
                }else{
                    this.g2d.drawImage(aengine.activeItem.imageData, mousex + 4, mousey + 4, null);
                    this.g2d.drawImage(this.toolStub, mousex, mousey, null);
                }
                
            }
                
            animToggle = !animToggle;
                       
            gScreen.drawImage(this.offscreen, 0, 0, null);      
   } 
       
   public Core() {
                
                animToggle = false;
                scroll = 0;
                maxScroll = 0;
                lastTime = 0;
       
                Image img = createImage(new MemoryImageSource(1, 1, new int[2], 0, 0));
                Toolkit kit = Toolkit.getDefaultToolkit();
                Cursor cursor = kit.createCustomCursor(img, new java.awt.Point(0,0), "Transparent");
                setCursor(cursor);
                
                try{
                    this.midiEngine = MidiSystem.getSequencer();
                    midiEngine.open();
                }catch(MidiUnavailableException e){
                    System.out.printf("Midi is unsupported on this system!");
                }
               
                this.addMouseListener(new thisMouseListener());
                
                midiEngine.addMetaEventListener(new MetaEventListener() {
                    public void meta(MetaMessage msg) {
                        if (msg.getType() == 0x2F) { // End of track
                           // Restart the song
                          midiEngine.setTickPosition(0);
                          midiEngine.start();
                        }
                    }
                });
                
                
                
                theTimer = new timeThread(aengine, engine, this, "");
                theTimer.start();
               
                
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
                    
                this.setPreferredSize( new Dimension( 640, 480 ) );
                
                this.offscreen = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
                this.g2d = (Graphics2D)this.offscreen.createGraphics();
                
                this.aengine = new InterfaceTree(this.midiEngine);
                
                manager = new ScriptEngineManager();
                
                                
                this.initTree("game/start.xml");
                
	}
   
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
        
        private void initTree(String level){
                
                System.out.println("Resetting the tree.");
            
                if(theTimer != null) {
                    aengine.processing = true;
                    theTimer.interrupt();
                    while(theTimer.isAlive()) {}
                    theTimer = null;
                }
            
                NodeList tempList;
                Node tempNode;
                int i;
                Thing tempThing;
                Sound tempSound;
                String tempString, handleString, imageString;
                Element eElement;
                
                aengine.resetChildren();              
                                
                try {
                
                    File xmlFile = new File(level);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(xmlFile);
                    Node docBody = doc.getElementsByTagName("level").item(0);
                                        
                    doc.getDocumentElement().normalize();
                    
                    	
                    eElement = doc.getDocumentElement();
                    
                    tempList = eElement.getElementsByTagName("thing");
                    for(i = 0; i < tempList.getLength(); i++) {
                        imageString = getTagValue("image", (Element)tempList.item(i));
                        if(imageString != null) {
                            handleString = getTagValue("handle", (Element)tempList.item(i));
                            if(handleString != null) {
                                tempThing = aengine.makeThing(imageString, handleString);
                                
                                tempString = getTagValue("x", (Element)tempList.item(i));
                                if(tempString != null) tempThing.x = Integer.parseInt(tempString);
                                
                                tempString = getTagValue("y", (Element)tempList.item(i));
                                if(tempString != null) tempThing.y = Integer.parseInt(tempString);
                                
                                tempString = getTagValue("z", (Element)tempList.item(i));
                                if(tempString != null) tempThing.z = Integer.parseInt(tempString);
                                
                                tempString = getTagValue("hidden", (Element)tempList.item(i));
                                if(tempString != null){System.out.println(tempString); tempThing.hidden = Boolean.parseBoolean(tempString);}
                                
                                tempString = getTagValue("playing", (Element)tempList.item(i));
                                if(tempString != null) tempThing.animationOn = Boolean.parseBoolean(tempString);
                                
                                tempString = getTagValue("looped", (Element)tempList.item(i));
                                if(tempString != null) tempThing.looped = Boolean.parseBoolean(tempString);
                                
                                tempString = getTagValue("mirrored", (Element)tempList.item(i));
                                if(tempString != null) tempThing.mirrored = Boolean.parseBoolean(tempString);
                                
                                tempString = getTagValue("scale", (Element)tempList.item(i));
                                if(tempString != null) tempThing.scale = Double.parseDouble(tempString);
                                
                                tempString = getTagValue("onclick", (Element)tempList.item(i));
                                if(tempString != null) tempThing.onClick = tempString;
                                
                                aengine.appendThing(tempThing);
                            } 
                        }
                        
                    }
                                        
                    tempList = eElement.getElementsByTagName("sound");
                    for(i = 0; i < tempList.getLength(); i++) {
                        imageString = getTagValue("file", (Element)tempList.item(i));
                        if(imageString != null) {
                            handleString = getTagValue("handle", (Element)tempList.item(i));
                            if(handleString != null) {
                                tempSound = aengine.makeSound(imageString, handleString);  
                                aengine.appendSound(tempSound);
                            } 
                        }
                        
                    }
                    
                    tempString = getTagValue("walkmap", eElement);
                    if(tempString != null) aengine.loadWalkmap(tempString);
                    
                    this.lastLevel = aengine.level;
                    
                    if(theTimer != null)
                        System.out.println(theTimer.isAlive());
                    
                    engine = null;
                    engine = (RhinoScriptEngine)manager.getEngineByName("JavaScript");
                    engine.put("AEngine2", this.aengine);
                    engine.eval(getTagValue("script", eElement));
                    
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
                
                
                aengine.processing = false;
        }
        
        private class thisMouseListener implements MouseListener {
            
                    ArrayList underMouse;
                    Iterator thingIterator; 
                    ListIterator underIterator;
                    Item tempTool;
                    Thing tempThing;
                    boolean itemExists = false;
            
                    @Override public void mouseClicked(MouseEvent event) {
                        if(mousey < 360) {
                           thingIterator = aengine.things.iterator();
                           underMouse = new ArrayList();
                           while(thingIterator.hasNext()){
                               tempThing = (Thing)thingIterator.next();
                               if((mousey >= tempThing.y) && (mousey <= (tempThing.imageData.getHeight() + tempThing.y)) && (mousex >= tempThing.x) && (mousex <= (tempThing.imageData.getWidth() + tempThing.x))){
                                   underMouse.add(tempThing);
                               }
                           }
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
                            
                           if(mousex <= 625 && mousex >= 609){
                                if(mousey >= 372 && mousey <= 383 && scroll > 0)
                                    scroll--;

                                if(mousey >= 457 && mousey <= 468 && scroll < maxScroll){
                                    scroll++;
                                }
                           }
                           
                           if(mousex <= 505 && mousex >= 489){
                                if(mousey >= 372 && mousey <= 383 && aengine.linePosition > 1)
                                    aengine.linePosition--;

                                if(mousey >= 457 && mousey <= 468 && aengine.linePosition + 5 < aengine.textLines.size())
                                    aengine.linePosition++;
                           }
                            
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
