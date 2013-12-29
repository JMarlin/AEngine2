package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    Global.java                                                                           *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This file represents a simple key-value hash type for use in defining and setting and       *
 * retrieving parameters of the global game state. As such a collection these objects is stored   *
 * in the InterfaceTree and is the only collection not cleared when the tree is reset for a level *
 * change. In this way a player could, for example, open a door in one room then leave and return *
 * to the room with the door still open thanks to the use of a Global.                            *
 **************************************************************************************************/

public class Global {
    
    //Class property definitions
    public String value;        //The value stored by the Global
    private String handle;      //The key/name of the Global
    
    //First type of constructor taking only the name of the new Global
    //and returning a Global with an empty value string
    public Global(String newHandle){
        this.handle = newHandle;
        this.value = "";
    }
    
    //Second type of constructor taking a name and initial value
    public Global(String newHandle, String newValue){
        this.handle = newHandle;
        this.value = newValue;
    }
    
    //Value getter
    public String getValue() {
        return this.value;
    }
    
    //Value setter
    public void setValue(String newValue) {
        this.value = newValue;
    }
    
    //Handle getter (no setter as we don't want the name changed after creation)
    public String getHandle() {
        return this.handle;
    }
    
}
