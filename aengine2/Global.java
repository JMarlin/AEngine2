package aengine2;

public class Global {
    public String value;
    private String handle;
    
    public Global(String newHandle){
        this.handle = newHandle;
        this.value = "";
    }
    
    public Global(String newHandle, String newValue){
        this.handle = newHandle;
        this.value = newValue;
    }
    
    public String getValue() {
        return this.value;
    }
    
    public void setValue(String newValue) {
        this.value = newValue;
    }
    
    public String getHandle() {
        return this.handle;
    }
    
}
