package aengine2;
/**************************************************************************************************
 * Project: AEngine2                                                                              *
 * File:    playbackThread.java                                                                   *
 * Author:  Joseph Marlin (j.marlin@outlook.com)                                                  *
 * Description:                                                                                   *
 *    This is a thread dedicated to loading a specified WAV file and streaming the samples it     *
 * contains to a Java audio line for output by the soundcard                                      *
 *    This could probably be upgraded in the future to allow for mp3 decoding as the WAV files    *
 * end up making the game resources rather massive                                                *
 **************************************************************************************************/

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;


//Play a WAV from disk
public class playbackThread extends Thread {
        
        private AudioInputStream in;            //The stream of audio data from the source file
        private AudioFormat decodedFormat;      //The format of the playback (2ch, 16bit, 44.1khz)
        private AudioInputStream din;           //The input stream decoded by the playback format
        private AudioFormat baseFormat;         //The format of the input file
        private SourceDataLine line;            //The playback line linked to the soundcard
        private boolean loop;                   //Whether or not the sound is looped
        private BufferedInputStream stream;     //The input file

        //Reload the audio file and get a new output line
        private void reset() {
            try {
                
                //Rewind the input file
                stream.reset();
                
                //Decode the audio and get a new output line
                in = AudioSystem.getAudioInputStream(stream);
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                line = getLine(decodedFormat);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Constructor allowing for specification of loop parameter
        public playbackThread(String filename, boolean loop) {
            this(filename);
            this.loop = loop;
        }

        //Main constructor 
        //Opens the file, turns it into a decoded audio stream and creates 
        //and output line into which that stream can be pumped
        public playbackThread(String filename) {
            
            //One-shot by default
            this.loop = false;
            
            try {
                
                //Attempt to open the file and get its audio stream
                stream = new BufferedInputStream(new FileInputStream(filename));
                in = AudioSystem.getAudioInputStream(stream);
                din = null;
                
                //We don't need to do anything else if we couldn't open the file
                if (in != null) {
                    
                    //Get the format of the WAV and define the playback format
                    //we wish to use for the output line
                    baseFormat = in.getFormat();
                    decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED, baseFormat
                                    .getSampleRate(), 16, baseFormat.getChannels(),
                            baseFormat.getChannels() * 2, baseFormat
                                    .getSampleRate(), false);

                    //Translate the WAV stream into the format our output line 
                    //expects and then create the line itself
                    din = AudioSystem.getAudioInputStream(decodedFormat, in);
                    line = getLine(decodedFormat);
                    
                }
                
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        }
        
        //Open a new output line with the specified playback format
        private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
            
            SourceDataLine res = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            res = (SourceDataLine) AudioSystem.getLine(info);
            res.open(audioFormat);
            return res;
            
        }
        
        //This is the code which this thread will execute when activated
        //It pumps data 4kb at a time from the audio stream to the soundcard
        @Override
        public void run() {
            
            
            byte[] data;                //The playback buffer
            boolean firstTime = true;   //Controls looping of the audio
            int nBytesRead;             //The number of bytes retrieved from the input stream
            
            try {
                
                //Make sure this loop cycles only once unless the audio is set to loop
                while (firstTime || loop) {

                    firstTime = false;
                    
                    //Create the playback buffer
                    data = new byte[4096];
                    
                    //Don't bother trying to play the stream if we couldn't get
                    //a proper output line connected
                    if (line != null) {
                        
                        //Open the line for playback
                        line.start();
                        
                        //Keep reading 4kb chunks of data from the input
                        //stream and placing them into the playback line 
                        //until the end of the stream is found
                        nBytesRead = 0;
                        while (nBytesRead != -1) {
                            nBytesRead = din.read(data, 0, data.length);
                            if (nBytesRead != -1)
                                line.write(data, 0, nBytesRead);
                        }
                        
                        //Clean and close the output line
                        line.drain();
                        line.stop();
                        line.close();

                    }
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
    }
