/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package aengine2;

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

/**
 *
 * @author joseph
 */
public class polygonMap {
    
    public ArrayList polygons;
    public boolean validFile; 
    
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
    
    public polygonMap(){
        polygons = new ArrayList();
    }
    
    public polygonMap(File mapFile){
        
        Document doc;
        int polyCount, nodeCount, i, j, tempx, tempy;
        NodeList polyEntries, nodeEntries;
        Element rootElement;
        String tempString, nodeVal;
        polyNode tempNode, headNode, lastNode;
        
        polygons = new ArrayList();
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        
        validFile = true;
        try{
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(mapFile);
        }catch(Exception e){
            validFile = false;
            return;
        }
        
        try{
            rootElement = (Element)doc.getElementsByTagName("polygonmap").item(0);
        }catch(Exception e){
            validFile = false;
            return;
        }

        if(rootElement == null) return;
        polyEntries = rootElement.getElementsByTagName("polygon");
        polyCount = polyEntries.getLength();
        for(i = 0; i < polyCount; i++){
            nodeEntries = polyEntries.item(i).getChildNodes();
            nodeCount = nodeEntries.getLength();
            headNode = null;
            tempNode = null;
            lastNode = null;
            for(j = 0; j < nodeCount; j++){
                    if(nodeEntries.item(j).getNodeName().equals("vertex")){

                        tempString = getTagValue("x", (Element)nodeEntries.item(j));
                        if(!tempString.equals(""))
                            tempx = Integer.parseInt(tempString);
                        else
                            tempx = 0;

                        tempString = getTagValue("y", (Element)nodeEntries.item(j));
                        if(!tempString.equals(""))
                            tempy = Integer.parseInt(tempString);
                        else
                            tempy = 0;
                        
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
            
            if(headNode != null){
                
                headNode.leftNeighbor = tempNode;
                tempNode.rightNeighbor = headNode;
                
                polygons.add(headNode);
                
            }
        }
        
        //Rationalize the polygons (make sure each polygon is not self-intersecting and that all polygons have three or more nodes )
        
    }
    
    public void save(File destFile){
        
        Transformer transformer;
        Element tempPolygon, vertNode, xNode, yNode, rootNode;
        polyNode tempNode, startNode;
        int i;
        boolean exitNext;
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try{
            dBuilder = dbFactory.newDocumentBuilder();
        }catch(Exception e){
            return;
        }
        
        Document doc = dBuilder.newDocument();
        rootNode = doc.createElement("polygonmap");
        doc.appendChild(rootNode);
        
        for(i = 0; i < polygons.size(); i++){
            tempNode = (polyNode)polygons.get(i);
            
            tempPolygon = doc.createElement("polygon");
            
            startNode = tempNode;
            
            exitNext = false;
            while(true){
                
                vertNode = doc.createElement("vertex");
                
                xNode = doc.createElement("x");
                xNode.setTextContent(String.valueOf(tempNode.position.x));

                yNode = doc.createElement("y");
                yNode.setTextContent(String.valueOf(tempNode.position.y));


                vertNode.appendChild(xNode);
                vertNode.appendChild(yNode);
                tempPolygon.appendChild(vertNode);
                
                if(exitNext) break;
                tempNode = tempNode.rightNeighbor;
                if(tempNode == startNode)
                    exitNext = true;
            }
            rootNode.appendChild(tempPolygon);
            
        }
        
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
    
    public class polyNode{
        public polyNode leftNeighbor;
        public polyNode rightNeighbor;
        public Point position;
        
        public polyNode(polyNode parent, int x, int y){
            position = new Point(x, y);
            leftNeighbor = parent;
            rightNeighbor = null;
        }
        
        public boolean isClosed(){
            polyNode currentNode;
            boolean retVal = false; 
            
            currentNode = this;
            
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
