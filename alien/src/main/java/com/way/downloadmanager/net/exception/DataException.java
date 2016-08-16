package com.way.downloadmanager.net.exception;

public class DataException extends Exception {

    private static final long serialVersionUID = 5093049925663572599L;

    private DataErrorEnum dataErrorCode;


    public DataException(DataErrorEnum dataErrorCode) {
        super();
        this.dataErrorCode = dataErrorCode;
    }

    public DataException(String message, DataErrorEnum dataErrorCode) {
        super(message);
        this.dataErrorCode = dataErrorCode;
    }

    public DataException(Throwable cause, DataErrorEnum dataErrorCode) {
        super(cause);
        this.dataErrorCode = dataErrorCode;
    }

    public DataException(String message, Throwable cause,
                         DataErrorEnum dataErrorCode) {
        super(message, cause);
        this.dataErrorCode = dataErrorCode;
    }

    public DataErrorEnum getNetErrorCode() {
        return dataErrorCode;
    }

}
