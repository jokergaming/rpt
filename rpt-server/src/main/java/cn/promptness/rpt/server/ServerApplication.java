package cn.promptness.rpt.server;

import cn.promptness.rpt.base.coder.MessageDecoder;
import cn.promptness.rpt.base.coder.MessageEncoder;
import cn.promptness.rpt.base.config.ServerConfig;
import cn.promptness.rpt.base.handler.IdleCheckHandler;
import cn.promptness.rpt.base.utils.Config;
import cn.promptness.rpt.server.cache.ServerChannelCache;
import cn.promptness.rpt.server.handler.RequestHandler;
import cn.promptness.rpt.server.handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.InputStream;

public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) throws SSLException {

        ServerConfig serverConfig = Config.getServerConfig();
        InputStream certChainFile = ClassLoader.getSystemResourceAsStream("server.crt");
        InputStream keyFile = ClassLoader.getSystemResourceAsStream("pkcs8_server.key");
        InputStream rootFile = ClassLoader.getSystemResourceAsStream("ca.crt");
        SslContext sslContext = SslContextBuilder.forServer(certChainFile, keyFile).trustManager(rootFile).clientAuth(ClientAuth.REQUIRE).sslProvider(SslProvider.OPENSSL).build();

        NioEventLoopGroup serverBossGroup = new NioEventLoopGroup();
        NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                // 固定帧长解码器
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                ch.pipeline().addLast(new LengthFieldPrepender(4));
                ch.pipeline().addLast(new ChunkedWriteHandler());
                // 自定义协议解码器
                ch.pipeline().addLast(new MessageDecoder());
                // 自定义协议编码器
                ch.pipeline().addLast(new MessageEncoder());
                ch.pipeline().addLast(new IdleCheckHandler(60, 40, 0));
                // 代理客户端连接代理服务器处理器
                ch.pipeline().addLast(new ServerHandler());
            }
        });
        bootstrap.bind(serverConfig.getServerIp(), serverConfig.getServerPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.info("服务端启动成功,本机绑定IP:{},服务端口:{}", serverConfig.getServerIp(), serverConfig.getServerPort());
                startHttp(serverBossGroup, serverWorkerGroup);
            } else {
                logger.info("服务端启动失败,本机绑定IP:{},服务端口:{},原因:{}", serverConfig.getServerIp(), serverConfig.getServerPort(), future.cause().getMessage());
                serverBossGroup.shutdownGracefully();
                serverWorkerGroup.shutdownGracefully();
            }
        });
    }

    private static void startHttp(NioEventLoopGroup serverBossGroup, NioEventLoopGroup serverWorkerGroup) {
        ServerConfig serverConfig = Config.getServerConfig();
        ServerBootstrap httpBootstrap = new ServerBootstrap();
        httpBootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childOption(ChannelOption.SO_KEEPALIVE, true).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new HttpServerCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(8 * 1024 * 1024));
                ch.pipeline().addLast(new ChunkedWriteHandler());
                ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                    }
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
                        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);
                        HttpHeaders headers = response.headers();
                        headers.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                        headers.set(HttpHeaderNames.LOCATION, HttpScheme.HTTPS + "://" + msg.headers().get(HttpHeaderNames.HOST) + msg.uri());
                        ch.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    }
                });
            }
        });
        httpBootstrap.bind(serverConfig.getServerIp(), serverConfig.getHttpPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.info("服务端启动成功,本机绑定IP:{},Http端口:{}", serverConfig.getServerIp(), serverConfig.getHttpPort());
                startHttps(serverBossGroup, serverWorkerGroup);
            } else {
                logger.info("服务端启动失败,本机绑定IP:{},Http端口:{},原因:{}", serverConfig.getServerIp(), serverConfig.getHttpPort(), future.cause().getMessage());
                serverBossGroup.shutdownGracefully();
                serverWorkerGroup.shutdownGracefully();
            }
        });
    }

    private static void startHttps(NioEventLoopGroup serverBossGroup, NioEventLoopGroup serverWorkerGroup) throws SSLException {
        ServerConfig serverConfig = Config.getServerConfig();
        InputStream certChainFile = ClassLoader.getSystemResourceAsStream(serverConfig.getDomainCert());
        InputStream keyFile = ClassLoader.getSystemResourceAsStream(serverConfig.getDomainKey());
        SslContext sslContext = SslContextBuilder.forServer(certChainFile, keyFile).clientAuth(ClientAuth.NONE).sslProvider(SslProvider.OPENSSL).build();

        ServerBootstrap httpsBootstrap = new ServerBootstrap();
        httpsBootstrap.group(serverBossGroup, serverWorkerGroup).channel(NioServerSocketChannel.class).childOption(ChannelOption.SO_KEEPALIVE, true).childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                ch.pipeline().addLast(new HttpServerCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(8 * 1024 * 1024));
                ch.pipeline().addLast(new ChunkedWriteHandler());
                ch.pipeline().addLast(new RequestHandler());
                ServerChannelCache.getServerHttpChannelMap().put(ch.id().asLongText(), ch);
            }
        });
        httpsBootstrap.bind(serverConfig.getServerIp(), serverConfig.getHttpsPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.info("服务端启动成功,本机绑定IP:{},Https端口:{}", serverConfig.getServerIp(), serverConfig.getHttpsPort());
            } else {
                logger.info("服务端启动失败,本机绑定IP:{},Https端口:{},原因:{}", serverConfig.getServerIp(), serverConfig.getHttpsPort(), future.cause().getMessage());
                serverBossGroup.shutdownGracefully();
                serverWorkerGroup.shutdownGracefully();
            }
        });
    }
}
