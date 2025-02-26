package cn.promptness.rpt.server.handler;

import cn.promptness.rpt.base.utils.Config;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.regex.Pattern;

public class RedirectHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Pattern PATTERN = Pattern.compile(":");

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);
        HttpHeaders headers = response.headers();
        headers.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        String host = PATTERN.split(msg.headers().get(HttpHeaderNames.HOST))[0] + ":" + Config.getServerConfig().getHttpsPort();
        headers.set(HttpHeaderNames.LOCATION, HttpScheme.HTTPS + "://" + host + msg.uri());        ChannelFuture future = ctx.writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(msg)) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
