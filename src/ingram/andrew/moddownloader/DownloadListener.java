package ingram.andrew.moddownloader;

public interface DownloadListener {
    void downloadProgressChanged(long currentDataSize, long expectedDataSize);
    void startedAllDownloads();
    void startedDownload(String filePath);
    void finishedDownload(String filePath);
    void finishedAllDownloads();
    void caughtError(String errorMessage, String filePath);
    void openedURL();
    void openedConnection();
    void createdInputStream();
    void createdOutputStream();
    void gotFileSize(long fileSize);

}
