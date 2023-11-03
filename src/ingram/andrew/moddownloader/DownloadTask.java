package ingram.andrew.moddownloader;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class DownloadTask implements Runnable{

    public final ArrayList<String> FILE_LIST;
    public final String OUTPUT_FOLDER;

    private final ArrayList<DownloadListener> LISTENERS;

    private long currentFileSize;
    private long totalFileSize;
    private int completedFileCount;

    public DownloadTask(String outputFolder, ArrayList<String> fileList) {

        // uncomment this next line if absolute directory????
        outputFolder = outputFolder.startsWith("/") ? outputFolder : "/" + outputFolder;
        outputFolder = outputFolder.endsWith("/") ? outputFolder : outputFolder + "/";

        this.OUTPUT_FOLDER = outputFolder;
        this.FILE_LIST = fileList;
        this.LISTENERS = new ArrayList<>();
    }

    private boolean fileAlreadyDownloaded(String outputPath) {

        File fileCheck = new File(outputPath);

        boolean exists = fileCheck.exists();

        if (exists) System.out.print(" >> File already exists.");

        return exists;

    }

    private void download(String filePath, String outputPath) {
        startedDownload(filePath);

        if (!fileAlreadyDownloaded(outputPath)) {
            // open url
            URL url;
            try {
                url = new URL(filePath);
            } catch (IOException e) {
                caughtError("Failed to open URL to " + filePath + "!", filePath);
                e.printStackTrace();
                return;
            }
            openedURL();

            // open url connection
            URLConnection urlConnection;
            try {
                urlConnection = url.openConnection();

            } catch (IOException e) {
                caughtError("Failed to open URL connection to " + filePath + "!", filePath);
                e.printStackTrace();
                return;
            }
            openedConnection();

            // get file size
            try {
                if (urlConnection instanceof HttpURLConnection) {
                    ((HttpURLConnection) urlConnection).setRequestMethod("HEAD");
                }

                urlConnection.getInputStream();
                totalFileSize = urlConnection.getContentLengthLong();
            } catch (ProtocolException e) {
                caughtError("Failed to set request method of HttpURLConnection!", filePath);
                e.printStackTrace();
                return;
            } catch (IOException e) {
                caughtError("Failed to open input stream of HttpURLConnection!", filePath);
                e.printStackTrace();
                return;
            }
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConnection).disconnect();
            }
            gotFileSize(totalFileSize);

            // create input stream
            BufferedInputStream inputStream;
            try {
                inputStream = new BufferedInputStream(url.openStream());
            } catch (IOException e) {
                caughtError("Failed to open input stream!", filePath);
                e.printStackTrace();
                return;
            }
            createdInputStream();

            // create output stream
            BufferedOutputStream outputStream;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(outputPath));
            } catch (FileNotFoundException e) {
                caughtError("Failed to open output stream!", filePath);
                e.printStackTrace();
                return;
            }
            createdOutputStream();

            // download data
            currentFileSize = 0;
            downloadProgressChanged();
            try {
                int byteDatum = inputStream.read();
                while (byteDatum != -1) {
                    currentFileSize++;
                    outputStream.write(byteDatum);

                    // send event to listeners that data has been received
                    downloadProgressChanged();

                    byteDatum = inputStream.read();
                }
                inputStream.close();
                outputStream.close();

            } catch (IOException e) {
                caughtError("Failed to download datum from URL!", filePath);
                e.printStackTrace();
                return;
            }
        }
        // complete :)
        completedFileCount++;
        finishedDownload(filePath);
        totalFileSize = 0;
        currentFileSize = 0;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    private void openedConnection() {
        for (DownloadListener listener : LISTENERS) {
            listener.openedConnection();
        }
    }

    private void gotFileSize(long totalFileSize) {
        for (DownloadListener listener : LISTENERS) {
            listener.gotFileSize(totalFileSize);
        }
    }

    private void createdInputStream() {
        for (DownloadListener listener : LISTENERS) {
            listener.createdInputStream();
        }
    }

    private void createdOutputStream() {
        for (DownloadListener listener : LISTENERS) {
            listener.createdOutputStream();
        }
    }

    private void openedURL() {
        for (DownloadListener listener : LISTENERS) {
            listener.openedURL();
        }
    }

    private void caughtError(String errorMessage, String filePath) {
        for (DownloadListener listener : LISTENERS) {
            listener.caughtError(errorMessage, filePath);
        }
    }

    private void finishedAllDownloads() {
        for (DownloadListener listener : LISTENERS) {
            listener.finishedAllDownloads();
        }
    }

    private void finishedDownload(String filePath) {
        for (DownloadListener listener : LISTENERS) {
            listener.finishedDownload(filePath);
        }
    }

    private void startedDownload(String filePath) {
        for (DownloadListener listener : LISTENERS) {
            listener.startedDownload(filePath);
        }
    }

    private void startedAllDownloads() {
        for (DownloadListener listener : LISTENERS) {
            listener.startedAllDownloads();
        }
    }

    private void downloadProgressChanged() {
        for (DownloadListener listener : LISTENERS) {
            listener.downloadProgressChanged(currentFileSize, totalFileSize);
        }
    }

    public void addListener(DownloadListener listener) {
        LISTENERS.add(listener);
    }

    public int getTotalFileCount() {
        return FILE_LIST.size();
    }

    public int getCompletedFileCount() {
        return completedFileCount;
    }

    @Override
    public void run() {
        //
        // make sure output directory exists
        File tempFile = new File(OUTPUT_FOLDER);
        if (!tempFile.exists()) tempFile.mkdirs();

        // get list of file names from url-list
        ArrayList<String> fileNameList = new ArrayList<>(FILE_LIST);
        fileNameList.replaceAll(this::getFileNameFromFileUrl);

        // remove all unnecessary or old mods from folder
        File[] directoryListing = tempFile.listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (!fileNameList.contains(file.getName())) {
                    System.out.println("Removed file: " + file.getName());
                    file.delete();
                }
            }
        }

        startedAllDownloads();
        for (String filePath : FILE_LIST) {
            String fileName = getFileNameFromFileUrl(filePath);
            download(filePath, OUTPUT_FOLDER + fileName);
        }
        finishedAllDownloads();
    }

    private String getFileNameFromFileUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf('/')+1);
    }
}
