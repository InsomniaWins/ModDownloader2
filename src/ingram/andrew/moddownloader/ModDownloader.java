package ingram.andrew.moddownloader;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;

public class ModDownloader {

    private static ModDownloader instance;

    public static void main(String[] args) {
        ModDownloader.instance = new ModDownloader();
        ModDownloader.getInstance().start();
    }

    public void start() {
        System.out.println("Welcome to Mod Downloader. (2023)");

        // init stuff
        String outputFolder = "Downloaded Mods/";
        ArrayList<String> fileList = new ArrayList<>();

        // populate file list
        populateFileList(fileList);

        // create download thread
        DownloadTask downloadTask = new DownloadTask(outputFolder, fileList);
        Thread downloadThread = new Thread(downloadTask, "downloadThread");

        // create download listener to listen to data flow events
        DownloadListener downloadListener = new CustomDownloadListener(downloadTask);
        downloadTask.addListener(downloadListener);

        // begin downloading
        downloadThread.start();
    }

    public static ModDownloader getInstance() {
        return ModDownloader.instance;
    }

    private void populateFileList(ArrayList<String> fileList) {
        String fileName = "modlist.txt";
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Failed to open modlist.txt, quitting program.");
            System.exit(0);
        }

        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to open file reader for modlist.txt, quitting program.");
            System.exit(0);
        }
        BufferedReader reader = new BufferedReader(fileReader);

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                fileList.add(line);
            }
        } catch (IOException e) {
            System.out.println("Failed to read modlist.txt into fileList ArrayList.");
            throw new RuntimeException(e);
        }
    }

    static class CustomDownloadListener implements DownloadListener {
        long lastPrintData = -1;
        private DownloadTask downloadTask;

        public CustomDownloadListener(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
        }

        // must not have a print statement until download bar is finished or else the \r code might mess things up
        private void printDownloadProgress(long currentDataSize, long expectedDataSize) {
            double progressNumerator = (double) currentDataSize;
            double progressDenominator = (double) expectedDataSize;
            double progressRatio = progressNumerator / progressDenominator;

            int downloadProgress = (int) (100.0 * progressRatio);
            System.out.print("|");
            for (int i = 0; i < 20; i++) {
                if (i*5 >= downloadProgress) {
                    System.out.print("_");
                } else {
                    System.out.print("#");
                }
            }
            System.out.print("|  " + downloadProgress + "%\r");
        }

        private void printTotalDownloadProgress(int completedDownloads, int totalDownloads) {
            double progressRatio = completedDownloads / (double) totalDownloads;

            int downloadProgress = (int) (100.0 * progressRatio);
            System.out.print("Total Progress: |");
            for (int i = 0; i < 50; i++) {
                if (i*2 >= downloadProgress) {
                    System.out.print("_");
                } else {
                    System.out.print("#");
                }
            }
            System.out.println("|  " + downloadProgress + "%");
        }

        @Override
        public void downloadProgressChanged(long currentDataSize, long expectedDataSize) {
            double printInterval = expectedDataSize / 20.0;
            if (expectedDataSize != 0 && (lastPrintData == -1 || currentDataSize - lastPrintData > printInterval) || currentDataSize == expectedDataSize) {
                printDownloadProgress(currentDataSize, expectedDataSize);
                lastPrintData = currentDataSize;
            }
        }

        @Override
        public void startedAllDownloads() {
            System.out.println("Starting to download mods . . . ");
            printTotalDownloadProgress(0, downloadTask.getTotalFileCount());
        }

        @Override
        public void startedDownload(String filePath) {
            System.out.println("\nDownloading " + filePath + " . . . ");
        }

        @Override
        public void finishedDownload(String filePath) {
            System.out.println("\nFinished download.\n");
            printTotalDownloadProgress(downloadTask.getCompletedFileCount(), downloadTask.getTotalFileCount());
        }

        @Override
        public void finishedAllDownloads() {
            System.out.println("Finished downloading all mods!");
            System.out.println("Have a nice day. :)");
        }
        @Override
        public void caughtError(String errorMessage, String filePath) {
            System.out.println("!!(ERROR)!! While Downloading " + filePath + ": " + errorMessage);
        }

        @Override
        public void openedURL() {
            System.out.println("Opened URL.");
        }

        @Override
        public void openedConnection() {
            System.out.println("Opened Connection.");
        }

        @Override
        public void createdInputStream() {
            System.out.println("Created Input Stream.");
        }

        @Override
        public void createdOutputStream() {
            System.out.println("Created Output Stream.");
            if (downloadTask.getTotalFileSize() != 0L) {
                printDownloadProgress(0L, downloadTask.getTotalFileSize());
            }
        }

        @Override
        public void gotFileSize(long fileSize) {
            System.out.println("Got file-size of " + fileSize + " bytes.");
        }
    }


}
