package ingram.andrew.moddownloader;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class ModDownloader {

    private static ModDownloader instance;

    public static void main(String[] args) {
        ModDownloader.instance = new ModDownloader();
        ModDownloader.getInstance().start();
    }

    public void start() {
        System.out.println("Welcome to Mod Downloader. (2023)");

        // init stuff
        String outputFolder = System.getProperty("user.home") + "/AppData/Roaming/.minecraft/mods/";
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

        System.out.println("Getting mod-list from GitHub . . . ");
        String modListLink = "https://github.com/InsomniaWins/ModDownloader2/raw/master/modlist.txt";

        try {
            URL url = new URL(modListLink);
            Scanner scanner = new Scanner(url.openStream());

            while (scanner.hasNextLine()) {
                fileList.add(scanner.nextLine());
            }

            scanner.close();
        } catch (IOException e) {
            System.out.println("Failed to open/update mod-list, quitting program.");
            System.out.println("(Make sure you have a stable internet connection.)");
            System.exit(0);
            throw new RuntimeException(e);

        }

        System.out.println("Got mod-list successfully!");
    }

    static class CustomDownloadListener implements DownloadListener {
        long lastPrintData = -1;
        private final DownloadTask DOWNLOAD_TASK;

        public CustomDownloadListener(DownloadTask downloadTask) {
            this.DOWNLOAD_TASK = downloadTask;
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
            printTotalDownloadProgress(0, DOWNLOAD_TASK.getTotalFileCount());
        }

        @Override
        public void startedDownload(String filePath) {
            System.out.println("\nDownloading " + filePath + " . . . ");
        }

        @Override
        public void finishedDownload(String filePath) {
            System.out.println("\nFinished download.\n");
            printTotalDownloadProgress(DOWNLOAD_TASK.getCompletedFileCount(), DOWNLOAD_TASK.getTotalFileCount());
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
            if (DOWNLOAD_TASK.getTotalFileSize() != 0L) {
                printDownloadProgress(0L, DOWNLOAD_TASK.getTotalFileSize());
            }
        }

        @Override
        public void gotFileSize(long fileSize) {
            System.out.println("Got file-size of " + fileSize + " bytes.");
        }
    }


}
