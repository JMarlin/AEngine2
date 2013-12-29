
package aengine2;

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

public class playbackThread extends Thread {
        
        private AudioInputStream in;
        private AudioFormat decodedFormat;
        private AudioInputStream din;
        private AudioFormat baseFormat;
        private SourceDataLine line;
        private boolean loop;
        private BufferedInputStream stream;

        
        private void reset() {
            try {
                stream.reset();
                in = AudioSystem.getAudioInputStream(stream);
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                line = getLine(decodedFormat);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public playbackThread(String filename, boolean loop) {
            this(filename);
            this.loop = loop;
        }

        public playbackThread(String filename) {
            this.loop = false;
            try {
                stream = new BufferedInputStream(new FileInputStream(filename));

                in = AudioSystem.getAudioInputStream(stream);
                din = null;

                if (in != null) {
                    baseFormat = in.getFormat();

                    decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED, baseFormat
                                    .getSampleRate(), 16, baseFormat.getChannels(),
                            baseFormat.getChannels() * 2, baseFormat
                                    .getSampleRate(), false);

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

        private SourceDataLine getLine(AudioFormat audioFormat)
                throws LineUnavailableException {
            SourceDataLine res = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                    audioFormat);
            res = (SourceDataLine) AudioSystem.getLine(info);
            res.open(audioFormat);
            return res;
        }
        
        @Override
        public void run() {

            try {
                boolean firstTime = true;
                while (firstTime || loop) {

                    firstTime = false;
                    byte[] data = new byte[4096];

                    if (line != null) {

                        line.start();
                        int nBytesRead = 0;

                        while (nBytesRead != -1) {
                            nBytesRead = din.read(data, 0, data.length);
                            if (nBytesRead != -1)
                                line.write(data, 0, nBytesRead);
                        }

                        line.drain();
                        line.stop();
                        line.close();

                        //reset();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
