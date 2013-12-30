package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    polygonMap.java                                                                       *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *   This class represents a collection of polygons each defined as a linked list of 2D points    *
 * and provides a constructor which allows for the creation of such a structure from the content  *
 * of an XML file as well as a method for creating an XML file from the object.                   *
 *    In AEngine2, these maps will be used to define the edges of walkable areas in order to      *
 * create a more robust, user-friendly character walk path solver                                 *
 **************************************************************************************************/

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 import org.w3c.dom.*;

//A list of polygons in a scene
public class polygonMap {
    
    public ArrayList polygons;      //The list of polygons in the map
    public boolean validFile;       //Whether or not the map could be created from XML
    
    //Return the value of the first tag matching sTag within the provided element
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
    
    //Trivial constructor 
    //Creates an empty map
    public polygonMap(){
        polygons = new ArrayList();
    }
    
    //XML constructor
    //Parses a polygonMap XML definition into a polygonMap object
    public polygonMap(File mapFile){
        
        Document doc;                                   //XML source
        int polyCount, nodeCount, i, j, tempx, tempy;   //Number of polygons, number of nodes in current polygon, a couple of general index vars and temporary storage for current node coordinates 
        NodeList polyEntries, nodeEntries;              //DOM collection of polygons, DOM collection of nodes in the current polygon
        Element rootElement;                            //The root of the document
        String tempString;                              //Used to retrieve value of X and Y tags
        polyNode tempNode, headNode, lastNode;          //The current node being built, the first node in the chain and the most recent node created
        
        polygons = new ArrayList();
        
        //Attempt to open the XML, exit with an invalid file indication on failure
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        validFile = true;
        try{
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(mapFile);
        }catch(Exception e){
            validFile = false;
            return;
        }
        
        //Get the root node, whch is the <POLYGONMAP> tag
        try{
            rootElement = (Element)doc.getElementsByTagName("polygonmap").item(0);
        }catch(Exception e){
            validFile = false;
            return;
        }
        if(rootElement == null) return;
        
        //Cycle through all found POLYGON tags and, for each, create a new polygon
        //node for every child VERTEX tag
        //Start by getting all POLYGON tags
        polyEntries = rootElement.getElementsByTagName("polygon");
        polyCount = polyEntries.getLength();
        for(i = 0; i < polyCount; i++){
            
            //Retrieve all children of the current POLYGON
            nodeEntries = polyEntries.item(i).getChildNodes();
            nodeCount = nodeEntries.getLength();
            
            //Clear the construction containers
            headNode = null;    
            tempNode = null;
            lastNode = null;
            
            for(j = 0; j < nodeCount; j++){
                
                    //Add a node if the current child is a VERTEX
                    if(nodeEntries.item(j).getNodeName().equals("vertex")){

                        //Find the value in the VERTEX's X tag
                        tempString = getTagValue("x", (Element)nodeEntries.item(j));
                        if(!tempString.equals(""))
                            tempx = Integer.parseInt(tempString);
                        else
                            tempx = 0;
                        
                        //Find the value in the VERTEX's Y tag
                        tempString = getTagValue("y", (Element)nodeEntries.item(j));
                        if(!tempString.equals(""))
                            tempy = Integer.parseInt(tempString);
                        else
                            tempy = 0;
                        
                        //Create a new polyNode from the collected data and add
                        //it to the chain making up the new polygon
                        lastNode = tempNode;
                        if(tempNode == null){
                            headNode = new polyNode(tempNode, tempx, tempy);
                            tempNode = headNode;
                        }else{
                            tempNode = new polyNode(tempNode, tempx, tempy);
                        }
                        if(lastNode != null)
                            lastNode.rightNeighbor = tempNode;
                            
                    }
                
                
            }
            
            //If there were nodes created, link the starting and ending nodes
            //and insert the reference to the starting node into the polygon list
            if(headNode != null){
                
                headNode.leftNeighbor = tempNode;
                tempNode.rightNeighbor = headNode;
                
                polygons.add(headNode);
                
            }
        }
        
    }
    
    //Convert this object into an XML representation
    public void save(File destFile){
        
        Transformer transformer;                                //Converts a DOM to a stream
        Element tempPolygon, vertNode, xNode, yNode, rootNode;  //Construction elements
        polyNode tempNode, startNode;                           //Working node object and the start of the current polygon
        int i;                                                  //General iterator variable
        boolean exitNext;                                       //Used to break the inner loop when the polygon is finished
        
        //Create a new document builder to make a DOM from the polygons
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try{
            dBuilder = dbFactory.newDocumentBuilder();
        }catch(Exception e){
            return;
        }
        
        //Create the new document and insert the root POLYGONMAP node
        Document doc = dBuilder.newDocument();
        rootNode = doc.createElement("polygonmap");
        doc.appendChild(rootNode);
        
        //Iterate through each polygon and add it to the document
        for(i = 0; i < polygons.size(); i++){
            
            //Get the start node of this polygon and create an element for it
            tempNode = (polyNode)polygons.get(i);
            tempPolygon = doc.createElement("polygon");
            startNode = tempNode;
            
            //Create a VERTEX element for each node in the list until we have
            //circled back around to the first node
            exitNext = false;
            while(true){
                
                //Create the VERTEX and its X and Y
                vertNode = doc.createElement("vertex");
                xNode = doc.createElement("x");
                xNode.setTextContent(String.valueOf(tempNode.position.x));
                yNode = doc.createElement("y");
                yNode.setTextContent(String.valueOf(tempNode.position.y));

                //Assemble the VERTEX tree and add it to the POLYGON
                vertNode.appendChild(xNode);
                vertNode.appendChild(yNode);
                tempPolygon.appendChild(vertNode);
                
                //Quit if this was the last node to create, otherwise 
                //get the node to the current node's right, setting the
                //exit flag if the right neighbor of that new node is the
                //start node
                if(exitNext) break;
                tempNode = tempNode.rightNeighbor;
                if(tempNode == startNode)
                    exitNext = true;
            }
            
            //Add the finished polygon to the document
            rootNode.appendChild(tempPolygon);
            
        }
        
        //Transform the document into a stream and write it to disk
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	try{
            transformer = transformerFactory.newTransformer();
        }catch(Exception e){
            return;
        }
	DOMSource source = new DOMSource(doc);
	StreamResult result = new StreamResult(destFile);
        try{
            transformer.transform(source, result);
        }catch(Exception e){
            return;
        }
        
    }
    
    //This child class represents a node within a polygon
    public class polyNode{
        
        public polyNode leftNeighbor;   //The node to the left of this one
        public polyNode rightNeighbor;  //The node to the right of this one
        public Point position;          //The coordinates of this node
        
        //Create a node to the right of the provided node at the given coordinates
        public polyNode(polyNode parent, int x, int y){
            position = new Point(x, y);
            leftNeighbor = parent;
            rightNeighbor = null;
        }
        
        //Check to see if the linked list of nodes of which this node is a member
        //is complete, ie: that every node has two neighbors
        public boolean isClosed(){
            
            polyNode currentNode;   //The node being processed
            boolean retVal = false; //Assume the list is open
            
            //Start from this node
            currentNode = this;
            
            //Traverse the list clockwise
            while(currentNode != null){
                if(currentNode.rightNeighbor == this){
                    retVal = true;
                    break;
                }
                currentNode = currentNode.rightNeighbor;
            }
            
            return retVal;
            
        }
        
    }
   
    
}
