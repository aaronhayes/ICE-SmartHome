package csse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import Ice.Current;
import csse4004._EMMDisp;
import datastore.MusicData;

/**
 * Electronic Media Manager for Smart Home
 *
 */
public class EMM extends Ice.Application {
    @SuppressWarnings("serial")
    class EMMI extends _EMMDisp {
        private ArrayList<MusicData> musicData;

        /**
         * Constructor of EMMI
         * 
         * @param dataFile
         *            Predefined data file
         */
        public EMMI(String dataFile) {
            musicData = new ArrayList<MusicData>();
            try {
                FileReader fileReader = new FileReader(dataFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                parseDataFile(bufferedReader);
            } catch (FileNotFoundException e) {
                System.err.println("File Not Found: " + dataFile);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Cannot parse file: " + dataFile);
                System.exit(1);
            }
        }

        /**
         * Parse Music Data File
         * 
         * @param br
         *            Buffered Reader of Stdin
         * @throws IOException
         */
        private void parseDataFile(BufferedReader br) throws IOException {
            String line = null; 
            String fileName = null; 
            String trackTitle = null;
            String disc = null;
            String track = null;
            
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                String[] content = line.split(":");
                switch (content[0].trim()) {
                case "filename":
                    fileName = content[1].trim();
                    break;
                case "title":
                    trackTitle = content[1].trim();
                    break;
                case "disc":
                    disc = content[1].trim();
                    break;
                case "track":
                    track = content[1].trim();

                    // End of music file, add to list.
                    MusicData data = new MusicData(fileName, trackTitle, disc,
                            track);
                    this.musicData.add(data);
                    break;
                }
            }
        }

        /**
         * Get Track Title
         * 
         * @param fileName
         *            String of file name
         * @return String of Track Title or null if file name not found.
         */
        @Override
        public String getTitle(String fileName, Current __current) {
            Iterator<MusicData> it = this.musicData.iterator();
            MusicData data;
            while (it.hasNext()) {
                data = it.next();
                if (data.getFileName().equals(fileName)) {
                    return data.getTitle();
                }
            }
            return null;
        }

        /**
         * Get Disc
         * 
         * @param fileName
         *            String of file Name
         * @return String of Disc or null if file name not found.
         */
        @Override
        public String getDisc(String fileName, Current __current) {
            Iterator<MusicData> it = this.musicData.iterator();
            MusicData data;
            while (it.hasNext()) {
                data = it.next();
                if (data.getFileName().equals(fileName)) {
                    return data.getDisc();
                }
            }
            return null;
        }

        /**
         * Get Music Tracks
         * 
         * @param disc
         *            String of Disc for Tracks
         * @return tracks on disc, in order by track number.
         */
        @Override
        public String getTracks(String disc, Current __current) {
            TreeMap<Integer, String> tracks = new TreeMap<Integer, String>();
            Iterator<MusicData> it = this.musicData.iterator();
            MusicData data;
            while (it.hasNext()) {
                data = it.next();
                if (data.getDisc().equals(disc)) {
                    tracks.put(data.getTrack(), data.getTitle());
                }
            }

            return treeMapToString(tracks);
        }

        /**
         * get Music Files
         * 
         * @return All Music Files Ordered Alphabetically
         */
        @Override
        public String getFiles(Current __current) {
            TreeMap<String, String> files = new TreeMap<String, String>();
            Iterator<MusicData> it = this.musicData.iterator();
            MusicData data;
            while (it.hasNext()) {
                data = it.next();
                files.put(data.getFileName(), data.getFileName());
            }

            return treeMapToString(files);
        }

        /**
         * Shutdown EMM server
         */
        @Override
        public void shutdown(Current __current) {
            communicator().shutdown();
        }

        /**
         * Convert a TreeMap into a String
         * 
         * @param treeMap
         *            <?, String> TreeMap
         * @return String of TreeMap
         */
        private String treeMapToString(TreeMap<?, String> treeMap) {
            if (treeMap.isEmpty()) {
                return null;
            }

            StringBuilder stringBuilder = new StringBuilder();
            Iterator<String> it = treeMap.values().iterator();

            while (it.hasNext()) {
                stringBuilder.append(it.next());
                if (it.hasNext()) {
                    stringBuilder.append(",");
                }
            }

            return stringBuilder.toString();
        }

    }

    /**
     * Entry point to the program
     * 
     * @param args
     *            Command Line arguments (Should be 1).
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("USAGE: EMM.java predefined-data-file");
            System.exit(1);
        }
        EMM musicManager = new EMM();
        int status = musicManager.main("EMM", args);
        System.exit(status);
    }

    /**
     * ICE main function
     */
    @Override
    public int run(String[] args) {
        EMMI emmi = new EMMI(args[0]);
        Ice.ObjectAdapter adapter = communicator()
                .createObjectAdapterWithEndpoints("EMM",
                        "tcp -h 127.0.0.1 -p 12002");
        
        adapter.add(emmi, communicator().stringToIdentity("emm"));
        adapter.activate();
        shutdownOnInterrupt();
        communicator().waitForShutdown();

        communicator().destroy();
        return 0;
    }

}