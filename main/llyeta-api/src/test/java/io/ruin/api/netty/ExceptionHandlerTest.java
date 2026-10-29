package io.ruin.api.netty;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExceptionHandlerTest {
    @Test public void expectedDisconnectsDoNotDependOnJdkStackFrames() {
        assertTrue(ExceptionHandler.isDisconnect(new SocketException("Connection reset")));
        IOException aborted = new IOException("An established connection was aborted by the software in your host machine");
        assertTrue(ExceptionHandler.isDisconnect(aborted));
        assertTrue(ExceptionHandler.isDisconnect(new RuntimeException(aborted)));
        assertTrue(ExceptionHandler.isDisconnect(new RuntimeException(new ClosedChannelException())));
        RuntimeException unexpected = new RuntimeException("decoder defect");
        unexpected.setStackTrace(new StackTraceElement[0]);
        assertFalse(ExceptionHandler.isDisconnect(unexpected));
        assertFalse(ExceptionHandler.isDisconnect(new SocketException("unexpected network error")));
        assertFalse(ExceptionHandler.isDisconnect(new IOException("unexpected I/O error")));
        assertFalse(ExceptionHandler.isDisconnect(new IOException()));
        assertFalse(ExceptionHandler.isDisconnect(new IllegalStateException(aborted.getMessage())));
    }
}
