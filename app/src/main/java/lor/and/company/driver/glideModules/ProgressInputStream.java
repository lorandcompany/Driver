package lor.and.company.driver.glideModules;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class ProgressInputStream extends InputStream {
    private final InputStream wrappedInputStream;
    private final long size;
    private Object tag;
    private long counter;
    private long lastPercent;
    private OnProgressListener listener;

    public ProgressInputStream(InputStream in, long size) {
        wrappedInputStream = in;
        this.size = size;
    }

    public void setOnProgressListener(OnProgressListener listener) { this.listener = listener; }

    public Object getTag() { return tag; }

    @Override
    public int read() throws IOException {
        counter += 1;
        check();
        return wrappedInputStream.read();
    }

    int checkCount = 0;

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        int retVal = wrappedInputStream.read(b, offset, length);
        counter += retVal;
        if (checkCount != 500) {
            checkCount += 1;
        } else {
            check();
            checkCount = 0;
            Log.d("count", "read: " + checkCount);
        }
        return retVal;
    }

    private void check() {
        int percent = (int) ( counter * 100 / size );
        if (percent - lastPercent >= 10) {
            lastPercent = percent;
            if (listener != null)
                listener.onProgress(percent, tag);
        }
        if (percent == 100) {
            this.listener = null;
        }
    }

    @Override
    public void close() throws IOException { wrappedInputStream.close(); }
    @Override
    public int available() throws IOException { return wrappedInputStream.available(); }
    @Override
    public void mark(int readlimit) { wrappedInputStream.mark(readlimit); }
    @Override
    public synchronized void reset() throws IOException { wrappedInputStream.reset(); }
    @Override
    public boolean markSupported() { return wrappedInputStream.markSupported(); }
    @Override
    public long skip(long n) throws IOException { return wrappedInputStream.skip(n); }

    /**
     * Interface for classes that want to monitor this input stream
     */
    public interface OnProgressListener {
        void onProgress(int percentage, Object tag);
    }
}
