/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package aengine2;

/**
 *
 * @author joseph
 */
public class visibilityGraph {
    
    public visibilityGraph(polygonMap polymap){
        //Import the polymap nodes
        //Add each of the polymap nodes as a 'visibility node'
        //->use the member function below
    }
    
    public void addStartNode(int x, int y){
        //Use the addNode function
        //but store a private handle 
        //for use in calculating the walkpath
    }
    
    public void addEndNode(int x, int y){
        //Use the addNode function
        //but store a private handle 
        //for use in calculating the walkpath
    }
    
    //private visNode addNode(int x, int y){
        //Create a new visNode (has position and collection of connected neighbors)
        //and return it
    //}
    
    //public ArrayList calculateWalkPath(int startX, int startY, int endX, int endY){
        //start coordinates determine what boundaries are 'voids' and what boundaries are 'objects'
        //then, calculate what other nodes are visible for each visNode based on the polymap
        //finally, do an A* from the start node (addNode(startX, startY)) to the end node (addNode(endX, endY))
        //and use the A* to create an arraylist of Point objects, describing the points in the character object's path
        //to be used by the walk animator 
    //}
    
}
