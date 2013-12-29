
package aengine2;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.*;

public class Sound {
    
    private String source;
    public Sequence midiSequence;
    public boolean ismidi;
    public String handle;
    public boolean loop;
    private Sequencer sequencer;
    private playbackThread superClip;
        
    public void play() {
        if(this.ismidi){
            sequencer.stop();
            try{
                sequencer.setSequence(this.midiSequence);
                sequencer.start();
            }catch(InvalidMidiDataException e){
                System.out.printf("Could not begin midi playback.\n");
            }   
        }else{
            if(!superClip.isAlive()){
                this.changeSound(this.source);
                this.superClip.start();
            }
        }
    }
    
    public void stop() {
        if(this.ismidi){
            sequencer.stop();
        }
    }
    
    public Sound(Sequencer newSequencer, String newSource, String newHandle) {
        this.handle = newHandle; 
        this.loop = false;
        this.ismidi = true;
        this.sequencer = newSequencer;
        changeSound(newSource);
    }
    
    public void changeSound(String newSource) {
        
        File tempFile;
                
        this.source = newSource;
        
        try{
            tempFile = new File(this.source);

            try{
                this.midiSequence = MidiSystem.getSequence(tempFile);
                this.ismidi = true;
                this.superClip = null;
            }catch(InvalidMidiDataException e){
                        this.superClip = new playbackThread(this.source, this.loop);
                        this.ismidi = false;
                        this.midiSequence = null;
            }
        }catch(IOException e2){
            this.midiSequence = null;
            this.superClip = null;
            System.out.printf("Could not load the file %s\n", this.source);
        }
        
    }
    
    
    
}