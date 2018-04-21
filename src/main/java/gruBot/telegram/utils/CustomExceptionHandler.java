package gruBot.telegram.utils;


import gruBot.telegram.logger.Logger;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler exceptionHandler;

    public CustomExceptionHandler() {
        this.exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        //if (throwable instanceof HttpHostConnectException || throwable instanceof ConnectException)
            Logger.log(throwable.getCause().toString(), Logger.Type.ERROR, Logger.Source.ALL);
        //else
        //    exceptionHandler.uncaughtException(thread, throwable);
    }
}
