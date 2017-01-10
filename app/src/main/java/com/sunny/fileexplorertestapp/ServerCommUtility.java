package com.sunny.fileexplorertestapp;

import java.io.Serializable;

public class ServerCommUtility {

    // since an arraylist (which is already serializable in itself) of this object is transferred,
    // no implement serializable
    public static class FileExpItem implements Serializable{
        private String name;
        private boolean directory;
        private String fileInfo;

        public FileExpItem(String name, boolean directory,String fileInfo) {
            this.directory = directory;
            this.name = name;
            this.fileInfo = fileInfo;
        }
        public String getName() {
            return name;
        }
        public boolean isDirectory() {
            return directory;
        }
        public String getFileInfo() {
            return fileInfo;
        }
    }

    // since it is static inner class , then it can be serialized. Never serialize nested class that are not static
    public static class RequestMessage implements Serializable {
        public static final String EXEC_FILE = "execute-file-in-directory";
        public static final String DISPLAY_DIRECTORY = "display-list-files-in-directory";
        public static final String NAVIGATE_PARENT = "navigate-to-parent-directory";
        public static final String LIST_ROOTS = "request-root-directories";

        private String currentDir;
        private String fileName;
        private String request;

        public RequestMessage() {
            currentDir = null;
            fileName = null;
            request = null;
        }
        public RequestMessage(String request){
            this();
            this.request = request;
        }
        public RequestMessage(String request, String currentDir){
            this(request);
            this.currentDir = currentDir;
        }
        public RequestMessage(String request, String currentDir, String fileName){
            this(request, currentDir);
            this.fileName = fileName;
        }

        public String getCurrentDir() {
            return currentDir;
        }

        public String getFileName() {
            return fileName;
        }

        public String getRequest() {
            return request;
        }
    }
}
