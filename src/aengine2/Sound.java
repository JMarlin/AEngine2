package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    Sound.java                                                                            *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This class defines a sound which can be used to portray background music, game dialogue     *
 * SFX, etc.                                                                                      *
 *    Supported formats are WAV and MIDI. MIDI is not supported on OSX 10.8+.                     *
 **************************************************************************************************/

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.*;

//Music, Dialogue, SFX
public class Sound {
    
    private String source;              //The file to be loaded
    public Sequence midiSequence;       //An object representing the MIDI data
    public boolean ismidi;              //Identifies this as a MIDI or WAV sound
    public String handle;               //The name by which this sound is referenced
    public boolean loop;                //Whether or not to start the sound over again on completion
    private Sequencer sequencer;        //The engine's MIDI sequencer, passed from Core to InterfaceTree to the sound
    private playbackThread superClip;   //The thread used to stream samples to the soundcard if this is a WAV
    public boolean playing;             //If the audio is stopped or not
    
    //Begin playback of the sound
    public void play() {
        
        //Either reset the engine MIDI sequencer and load the new sequence or
        //start a new thread to stream the WAV samples
        if(this.ismidi){
            sequencer.stop();
            try{
                sequencer.setSequence(this.midiSequence);
                sequencer.start();
                this.playing = true;
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
    
    //Does what it says on the box
    public void stop() {
        if(this.ismidi){
            sequencer.stop();
        }
        this.playing = false;
    }
    
    //Basic constructor 
    //Requires a reference to the engine's MIDI sequencer
    public Sound(Sequencer newSequencer, String newSource, String newHandle) {
        this.handle = newHandle; 
        this.loop = false;
        this.ismidi = true;
        this.playing = false;
        this.sequencer = newSequencer;
        changeSound(newSource);
    }
    
    //Load a new audio file
    public void changeSound(String newSource) {
        
        File tempFile;
                
        this.source = newSource;
        
        //Try to load the file into a MIDI sequence object
        //On failure, create a new runnable to process the data as a WAV
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