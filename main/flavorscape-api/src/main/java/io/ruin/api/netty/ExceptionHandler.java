package io.ruin.api.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.ruin.api.utils.ServerWrapper;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

public class ExceptionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(!isDisconnect(cause))
            ServerWrapper.logError("Netty issue", cause);
        ctx.close();
    }

    static boolean isDisconnect(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof ClosedChannelException)
                return true;
            if (current instanceof IOException && current.getMessage() != null) {
                String message = current.getMessage().toLowerCase(java.util.Locale.ROOT);
                if (message.contains("connection reset") || message.contains("broken pipe")
                        || message.contains("forcibly closed")
                        || message.contains("an established connection was aborted by the software in your host machine"))
                    return true;
            }
        }
        return false;
    }

}
