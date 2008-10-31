/**
 * 
 */
package info.sared;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jdesktop.swingworker.SwingWorker;

import com.twmacinta.util.MD5;

/**
 * @author eglen
 * 
 */
public class Main extends JPanel implements ActionListener,
        PropertyChangeListener
{
    /**
     * 
     */
    private static final long   serialVersionUID = -8763164613682401041L;
    static private final String newline          = "\n";
    JButton                     directoryButton, uploadChangesButton;
    JProgressBar                taskProgress, fileProgress;
    JTextArea                   log;
    JFileChooser                fc;
    File                        topLevelDir;
    Hasher                      hasher;
    
    public Main()
    {
        super(new BorderLayout());

        // Setup the log
        log = new JTextArea(5, 50);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        // Create the file chooser
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Add a couple buttons
        directoryButton = new JButton("Choose iWeb Directory");
        directoryButton.addActionListener(this);
        uploadChangesButton = new JButton("Upload changed files");
        uploadChangesButton.addActionListener(this);
        uploadChangesButton.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(directoryButton);
        buttonPanel.add(uploadChangesButton);

        // Setup the progress bar
        taskProgress = new JProgressBar();
        fileProgress = new JProgressBar();

        // Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
        add(taskProgress, BorderLayout.SOUTH);
        // add(fileProgress,BorderLayout.PAGE_END);
    }

    private void hashFileTree(File mainDir) throws IOException
    {
        // this.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // Get an array of all the Files
        

        /*
         * taskProgress = new JProgressBar(0, 100);
         * taskProgress.setIndeterminate(false); taskProgress.setValue(0);
         */
        taskProgress.setStringPainted(true);

        directoryButton.setEnabled(false);

        hasher = new Hasher(mainDir);
        hasher.addPropertyChangeListener(this);
        hasher.execute();

        // Get a map with all the paths
        // HashMap<File,String> fileMap = walkDirectoryTree(file);
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event dispatch thread.
     */
    private static void createAndShowGUI()
    {
        // Create and set up the window.
        JFrame frame = new JFrame("Website Diff Uploader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add content to the window.
        frame.add(new Main());

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals("progress"))
        {
            int progress = (Integer) evt.getNewValue();
            //System.out.println("Progess: " + progress);
            taskProgress.setValue(progress);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        // Handle open button action.
        if (e.getSource() == directoryButton)
        {
            int returnVal = fc.showOpenDialog(Main.this);

            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                log.append("Analyzing: " + file.getName() + "." + newline);
                topLevelDir = file;
                try
                {
                    hashFileTree(file);
                } catch (IOException e1)
                {
                    System.err
                            .println("Fault traversing the file tree. Sorry.");
                    e1.printStackTrace();
                }
            } else
            {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
        } else if (e.getSource() == uploadChangesButton)
        {
            try
            {
                doUpload();
            } catch (InterruptedException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (ExecutionException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException ioe)
            {
                // TODO Auto-generated catch block
                ioe.printStackTrace();
            }
        }
    }



    private HashMap<String, String> loadHashFile(File file)
    {
        String fullText = "";
        HashMap<String, String> loadedFile = new HashMap<String, String>();

        BufferedReader reader;
        try
        {
            reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            while (line != null)
            {
                line.trim();
                //System.out.println("Read line: " + line);
                fullText = fullText.concat(line);
                line = reader.readLine();
            }

            reader.close();
        } catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Parse the text into a hashmap
        String[] entries = fullText.split(",");
        for (int x = 0; x < entries.length; x++)
        {
            String[] entry = entries[x].split("=");
            if (entry.length == 2)
            {
                loadedFile.put(entry[0].trim(), entry[1].trim());
            } else
            {
                System.err
                        .println("Badly formed data file - contact edward@glencomm.com");
                System.err.println("length: " + entry.length );
                for(int i = 0; i < entry.length; i++)
                {
                        System.err.println("Entry: " + entry[i]);
                }
                log
                        .append("Badly formed data file - this shouldn't happen.  Please contact edward@glencomm.com");
            }
        }
        return loadedFile;
    }

    /*private void updateHashFile(HashMap<String,String> completedFilesMap)
    {
        Set<Entry<String,String>> completedFiles = completedFilesMap.entrySet();
        for(Entry<String,String> entry : completedFiles)
        {     
           previousUpload.put(entry.getKey(), entry.getValue());
        }
        String rawString = previousUpload.toString();
        String outputString = rawString.substring(1, rawString.length()-2);
    }*/
    
    private void doUpload() throws InterruptedException, ExecutionException, IOException
    {
        //HashMap<String,String> completedFiles = new HashMap<String,String>();
        //Set<String> keys = changedFiles.keySet();
        
        Set<Entry<String,String>> changedFiles = hasher.get().entrySet();
        for(Entry<String,String> entry : changedFiles)
        {
            boolean uploadSuccessful = copyFile(entry.getKey(),topLevelDir.getCanonicalPath());
            if(uploadSuccessful)
            {
                //completedFiles.put(entry.getKey(), entry.getValue());
                updateHashFile(entry);
            }
        }
        //updateHashFile(completedFiles);
    }
    
    private boolean copyFile(String file, String topLevelDir)
    {
        
        
        //Find the directory
        String directory = "";
        if(file.lastIndexOf("/") != -1)
        {
            directory = file.substring(0, file.lastIndexOf("/"));
        }
        String dircmd = "mkdir -p \"" + "/Users/eglen/Sites/iWebUploader/" + directory + "\"";
        
        
        String cmd = "cp -v \"" + topLevelDir + "/" + file +"\" \"/Users/eglen/Sites/iWebUploader/" + file + "\"";
        
        File outputDir = new File("/Users/eglen/Sites/");
        outputDir.mkdir();
        File outputFile = new File(outputDir.getAbsolutePath() + "/copyscript.sh");
            System.out.println("Output file: " + outputFile.getAbsolutePath());
            BufferedWriter writer;

            try
            {
                writer = new BufferedWriter(new FileWriter(outputFile,true));
                writer.write(dircmd);
                writer.newLine();
                writer.write(cmd);
                writer.newLine();
                writer.close();
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        
        return true;
    }

    private void updateHashFile(Entry<String, String> entry)
    {
        HashMap<String,String> hashes = loadPreviouslyUploadedFiles();
        hashes.put(entry.getKey(), entry.getValue());
        
        writeHashFile(hashes);
    }

    private void writeHashFile(HashMap<String, String> hashes)
    {
        String rawString = hashes.toString();
        //System.out.println("Raw String: " + rawString);
        String outputString = rawString.substring(1, rawString.length()-1);
        //System.out.println("Writing to file: " + outputString);
        
        File outputDir = new File(topLevelDir.getAbsolutePath()
                + "/.iWebUploaderConfig");
        outputDir.mkdir();
            File outputFile = new File(outputDir.getAbsolutePath() + "/lastUpload.dat");
            outputFile.delete();
            //System.out.println("Output file: " + outputFile.getAbsolutePath());
            BufferedWriter writer;

            try
            {
                writer = new BufferedWriter(new FileWriter(outputFile,false));
                writer.write(outputString);
                writer.newLine();
                writer.close();
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        
    }

    private ArrayList<File> walkDirectoryTree(File directory)
    {
        ArrayList<File> masterFileList = new ArrayList<File>();
        ArrayList<File> files = new ArrayList<File>(Arrays.asList(directory
                .listFiles()));
        for (File file : files)
        {
            if (file.isDirectory())
            {
                masterFileList.addAll(walkDirectoryTree(file));
            } else
            {
                masterFileList.add(file);
            }
        }
        return masterFileList;
    }
    
    private HashMap<String,String> loadPreviouslyUploadedFiles()
    {
        // See if we have any existing lists of data
        HashMap<String,String> previousUpload = new HashMap<String, String>();
        File listDir = new File(topLevelDir.getAbsolutePath()
                + "/.iWebUploaderConfig/");
        if (listDir.exists() && listDir.isDirectory())
        {
            File inputFile = new File(listDir.getAbsolutePath()
                    + "/lastUpload.dat");
            if(inputFile.exists())
            {
                previousUpload = loadHashFile(inputFile);
            }
        }
        return previousUpload;
    }

    public class Hasher extends SwingWorker<HashMap<String, String>, String>
    {
        File                  mainDir;
        HashMap<String, String> changedHashes;
        double                  progress = 0.0;

        public Hasher(File directory)
        {
            this.mainDir = directory;
        }
        
        @Override
        protected HashMap<String,String> doInBackground()
        {
            ArrayList<File> fileList = walkDirectoryTree(mainDir);
            //System.out.println("FileList size: " + fileList.size());
            HashMap<String,String> previousUpload = loadPreviouslyUploadedFiles();
            //System.out.println("Loaded");
            changedHashes = new HashMap<String, String>();
            double step = 100.0 / (double) fileList.size();
            setProgress((int) progress);
            
            // Head through and hash em all
            for (File file : fileList)
            {
                
                String filePath = file.getAbsolutePath();
                String relativePath = filePath.substring(topLevelDir
                        .getAbsolutePath().length() + 1);
                publish(relativePath);
                String hash ="";
                try
                {
                    hash = MD5.asHex(MD5.getHash(file));
                } catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //System.out.println("Has this key been loaded?: " + hash + " : " + previousUpload.containsKey(relativePath));
                if(previousUpload.containsKey(relativePath))
                {
                    //System.out.println("Existing Hash: " + previousUpload.get(relativePath) + " New Hash: " + hash);
                }
                if (!previousUpload.containsKey(relativePath))
                {
                    changedHashes.put(relativePath, hash);
                } else if (!previousUpload.get(relativePath).equals(hash))
                {
                    changedHashes.put(relativePath, hash);
                }
                //changedHashes.put(relativePath, hash);
                
                
                progress += step;
                //System.out.println("Progress end of loop: " + progress);
                setProgress((int) Math.floor(progress));
            }
            return changedHashes;
        }
        
        @Override
        protected void process(List<String> chunks)
        {
            /*System.out.println("Process called");
            for (String path : chunks) {
                System.out.println(path);
            // log.append(path + "\n");
            }*/
        }

        @Override
        protected void done()
        {
            setProgress(100);
            
            log.append("Analysis Complete\n");
            if(changedHashes==null || changedHashes.isEmpty())
            {
                log.append("No changed files\n");
            }
            else
            {
            Set<String> changedKeys = changedHashes.keySet();

            for (String key : changedKeys)
            {
                log.append(key + "\n");
            }
            log.append("----\nThere are " + changedKeys.size() + " changed files\n----\n");
            uploadChangesButton.setText("Upload " + changedKeys.size()
                    + " changed files");
            uploadChangesButton.setEnabled(true);
            directoryButton.setEnabled(true);
            }
        }

    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                // Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }

}
