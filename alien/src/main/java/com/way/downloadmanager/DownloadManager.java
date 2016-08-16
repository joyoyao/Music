package com.way.downloadmanager;

import com.way.downloadmanager.lib.FileUtil;
import com.way.downloadmanager.net.exception.DataErrorEnum;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DownloadManager implements IDownloadManager {
    private static DownloadManager manager = new DownloadManager();
    //正在等待的任务列表请求
    LinkedList<DownloadRequest> downloadWaitingList = new LinkedList<DownloadRequest>();
    //正在下载任务
    LinkedList<DownloadRequest> downloadingList = new LinkedList<DownloadRequest>();
    //暂停任务队列
    LinkedList<DownloadRequest> downloadPausedList = new LinkedList<DownloadRequest>();
    //正在下载的队列请求和任务对应关系
    Map<DownloadRequest, DownloadTask> downloading = new HashMap<DownloadRequest, DownloadTask>();
    int taskNum = 2;
    private HashMap<Object, ArrayList<IDownloadListener>> mListenerMaps =
            new HashMap<Object, ArrayList<IDownloadListener>>();
    private IDownloadControl iDownloadControl = new IDownloadControl() {

        @Override
        public void onStart(DownloadRequest request) {
            ArrayList<IDownloadListener> iDownloadListeners = getCallBackListeners(request);
            if (iDownloadListeners != null && iDownloadListeners.size() > 0) {
                for (IDownloadListener iDownloadListener : iDownloadListeners) {
                    iDownloadListener.downloadStart(request);
                }
            }

        }

        @Override
        public void onProgress(DownloadRequest request, int progress) {
            ArrayList<IDownloadListener> iDownloadListeners = getCallBackListeners(request);
            if (iDownloadListeners != null && iDownloadListeners.size() > 0) {
                for (IDownloadListener iDownloadListener : iDownloadListeners) {
                    iDownloadListener.downloadProgress(request, progress);
                }
            }
        }

        @Override
        public void onFinished(DownloadRequest request) {
            ArrayList<IDownloadListener> iDownloadListeners = getCallBackListeners(request);
            if (iDownloadListeners != null && iDownloadListeners.size() > 0) {
                for (int i = 0; i < iDownloadListeners.size(); i++) {
                    IDownloadListener iDownloadListener = iDownloadListeners.get(i);
                    iDownloadListener.downloadFinish(request);
                }
            }
            clearRequest(request, false);
            fillTasks();
        }

        @Override
        public void onError(DownloadRequest request, DataErrorEnum error) {
            ArrayList<IDownloadListener> iDownloadListeners = getCallBackListeners(request);
            if (iDownloadListeners != null && iDownloadListeners.size() > 0) {
                for (IDownloadListener iDownloadListener : iDownloadListeners) {
                    iDownloadListener.downloadError(request, error);
                }
            }
            clearRequest(request, false);
            fillTasks();
        }

        @Override
        public void onCancel(DownloadRequest request) {
            ArrayList<IDownloadListener> iDownloadListeners = getCallBackListeners(request);
            if (iDownloadListeners != null && iDownloadListeners.size() > 0) {
                for (IDownloadListener iDownloadListener : iDownloadListeners) {
                    iDownloadListener.downloadCancel(request);
                }
            }
            clearRequest(request, false);
            fillTasks();
        }

        @Override
        public void onPause(DownloadRequest request) {
            ArrayList<IDownloadListener> iDownloadListeners = getCallBackListeners(request);
            if (iDownloadListeners != null && iDownloadListeners.size() > 0) {
                for (IDownloadListener iDownloadListener : iDownloadListeners) {
                    iDownloadListener.downloadPause(request);
                }
            }
            clearRequest(request, true);
            fillTasks();
        }


        private void clearRequest(DownloadRequest request, boolean isPause) {
            synchronized (DownloadManager.this) {
                downloading.remove(request);
                downloadingList.remove(request);
                if (isPause) {
                    if (!downloadPausedList.contains(request)) {
                        downloadPausedList.add(request);
                    }
                } else {
                    downloadPausedList.remove(request);
                    removeListener(request.getResId());
                }

            }
        }
    };

    public DownloadManager(int taskNum) {
        this.taskNum = taskNum;
    }

    public DownloadManager() {
    }

    public static DownloadManager instance() {
        return manager;
    }

    public void setTaskNum(int taskNum) {
        this.taskNum = taskNum;
    }

    private ArrayList<IDownloadListener> getCallBackListeners(DownloadRequest request) {
        if (request == null) {
            return null;
        }
        return mListenerMaps.get(request.getResId());
    }

    @Override
    public void start(DownloadRequest downloadRequest) {
        DownloadTask task = null;
        if (!checkRequest(downloadRequest)) {
            return;
        }
        synchronized (this) {
            downloadPausedList.remove(downloadRequest);
            if (this.downloading.size() < this.taskNum) {
                task = new DownloadTask(downloadRequest, iDownloadControl);
                this.downloading.put(downloadRequest, task);
                this.downloadingList.add(downloadRequest);
            } else {
                this.downloadWaitingList.add(downloadRequest);
            }
        }
        if (task != null) {
            task.start();
        }
        ArrayList<IDownloadListener> iDownloadListeners = getCallBackListeners(downloadRequest);
        if (iDownloadListeners != null && iDownloadListeners.size() > 0) {
            for (IDownloadListener iDownloadListener : iDownloadListeners) {
                iDownloadListener.downloadWait(downloadRequest);
            }
        }
    }

    private boolean checkRequest(DownloadRequest downloadRequest) {
        synchronized (this) {
            if (downloadRequest != null) {
                for (DownloadRequest r : downloadWaitingList) {
                    if (r == downloadRequest ||
                            r.getResId().equals(downloadRequest.getResId())) {
                        //throw new RuntimeException("Bad DownloadRequest");
                        return false;
                    }
                }
                for (DownloadRequest r : downloadingList) {
                    if (r == downloadRequest ||
                            r.getResId().equals(downloadRequest.getResId())) {
                        //throw new RuntimeException("Bad DownloadRequest");
                        return false;
                    }
                }

                return true;
            }
            //throw new RuntimeException("downloadRequest == null");
            return false;
        }
    }

    public DownloadRequest getRequestById(Object resId) {
        synchronized (this) {
            for (DownloadRequest r : downloadWaitingList) {
                if (resId.equals(r.getResId())) {
                    return r;
                }
            }
            for (DownloadRequest r : downloadingList) {
                if (resId.equals(r.getResId())) {
                    return r;
                }
            }
            for (DownloadRequest r : downloadPausedList) {
                if (resId.equals(r.getResId())) {
                    return r;
                }
            }
        }
        return null;
    }

    @Override
    public void pause(Object resId) {
        cancel(resId, true);
    }

    @Override
    public void cancel(Object resId) {
        cancel(resId, false);
    }

    private void cancel(Object resId, boolean isPause) {
        DownloadRequest downloadRequest = getRequestById(resId);
        if (downloadRequest == null)
            return;
        synchronized (this) {
            Boolean isDownloading = isDownloading(resId);
            Boolean isDownloadPaused = isDownloadPaused(resId);
            if (isDownloading) {
                DownloadTask task = this.downloading.get(downloadRequest);
                if (isPause) {
                    task.pause();
                    iDownloadControl.onPause(downloadRequest);
                } else {
                    task.cancel();
                }
            } else if (isDownloadPaused) {
                deleteLocalTempFile(downloadRequest);
            } else {
                this.downloadWaitingList.remove(downloadRequest);
                if (isPause) {
                    iDownloadControl.onPause(downloadRequest);
                } else {
                    iDownloadControl.onCancel(downloadRequest);
                }

            }
            if (isDownloading) {
                this.fillTasks();
            }
        }
    }

    private void deleteLocalTempFile(DownloadRequest downloadRequest) {
        String filePath = downloadRequest.getFilePath();
        String fileName = downloadRequest.getFileName();
        File tempFilePath = new File(filePath, fileName + ".tmp");
        FileUtil.delete(tempFilePath);
        iDownloadControl.onCancel(downloadRequest);
    }

    private void fillTasks() {
        synchronized (this) {
            while (this.downloading.size() < this.taskNum) {
                if (this.downloadWaitingList.size() > 0) {
                    DownloadRequest downloadRequest = this.downloadWaitingList.pollFirst();
                    DownloadTask task = new DownloadTask(downloadRequest, iDownloadControl);
                    this.downloading.put(downloadRequest, task);
                    this.downloadingList.add(downloadRequest);
                    task.start();
                } else {
                    return;
                }
            }
        }
    }

    @Override
    public boolean isWaiting(Object resId) {
        return isContainResId(resId, downloadWaitingList);
    }

    @Override
    public boolean isDownloading(Object resId) {
        return isContainResId(resId, downloadingList);
    }

    private boolean isDownloadPaused(Object resId) {
        return isContainResId(resId, downloadPausedList);
    }

    @Override
    public int getDownloadProgress(Object resId) {
        synchronized (resId) {
            for (DownloadRequest r : downloadingList) {
                if (resId.equals(r.getResId())) {
                    return r.getProgress();
                }
            }
            return 0;
        }
    }

    private boolean isContainResId(Object resId, List<DownloadRequest> requests) {
        synchronized (this) {
            for (DownloadRequest r : requests) {
                if (r.getResId().equals(resId)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void registerListener(Object resId,
                                 IDownloadListener downloadListener) {
        synchronized (this) {
            if (downloadListener == null)
                return;

            ArrayList<IDownloadListener> listenerList = mListenerMaps.get(resId);
            if (listenerList != null) {
                for (IDownloadListener l : listenerList) {
                    if (l == downloadListener) {
                        return;
                    }
                }
            } else {
                listenerList = new ArrayList<IDownloadListener>();
            }
            listenerList.add(downloadListener);
            mListenerMaps.put(resId, listenerList);
        }
    }

    @Override
    public void removeListener(Object resId, IDownloadListener listener) {
        synchronized (this) {
            if (listener == null)
                return;
            ArrayList<IDownloadListener> listenerList = mListenerMaps.get(resId);
            if (listenerList != null) {
                for (IDownloadListener l : listenerList) {
                    if (l == listener) {
                        listenerList.remove(l);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void removeListener(IDownloadListener listener) {
        synchronized (this) {
            for (DownloadRequest request : downloading.keySet()) {
                ArrayList<IDownloadListener> listenerList = mListenerMaps.get(request.getResId());
                if (listenerList == null || listenerList.size() <= 0) {
                    return;
                }
                for (int i = listenerList.size() - 1; i >= 0; i--) {
                    IDownloadListener l = listenerList.get(i);
                    if (l == listener) {
                        listenerList.remove(l);
                        continue;
                    }
                }
            }
        }
    }

    @Override
    public void removeListener(Object resId) {
        synchronized (this) {
            mListenerMaps.remove(resId);
        }
    }

    @Override
    public List<DownloadRequest> getDownloadWaitingList() {
        synchronized (this) {
            return (List<DownloadRequest>) this.downloadWaitingList.clone();
        }
    }

    public List<DownloadRequest> getDownloadingList() {
        synchronized (this) {
            return (List<DownloadRequest>) this.downloadingList.clone();
        }
    }

}
