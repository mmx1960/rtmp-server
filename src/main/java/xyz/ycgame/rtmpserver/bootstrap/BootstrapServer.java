package xyz.ycgame.rtmpserver.bootstrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ycgame.rtmpserver.protocol.handler.RtmpCommandHandler;
import xyz.ycgame.rtmpserver.protocol.handler.RtmpDecoderHandler;
import xyz.ycgame.rtmpserver.protocol.handler.RtmpEncoderHandler;
import xyz.ycgame.rtmpserver.protocol.handler.ServerHandshakeHandler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class BootstrapServer {
    // region netty config params
    private final Integer backLog;

    private final Integer listenPort;

    private final Integer bossGroupThread;

    private final Integer serverSelectorThreads;

    private final ChannelHandler bizChannelHandler;

    // endregion

    //region netty members
    private ServerBootstrap bootstrap;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Channel channel;
    //endregion

    //region server state
    private boolean running = false;
    //endregion

    public static BootstrapServerBuilder builder() {
        return new BootstrapServerBuilder();
    }

    @Data
    public static class BootstrapServerBuilder {
        private Integer backLog;

        private Integer listenPort;

        private Integer bossGroupThread;

        private Integer serverSelectorThreads;

        private ChannelHandler bizChannelHandler;

        public BootstrapServerBuilder backLog(Integer backLog) {
            this.backLog = backLog;
            return this;
        }

        public BootstrapServerBuilder listenPort(Integer listenPort) {
            this.listenPort = listenPort;
            return this;
        }

        public BootstrapServerBuilder bossGroupThread(Integer bossGroupThread) {
            this.bossGroupThread = bossGroupThread;
            return this;
        }

        public BootstrapServerBuilder serverSelectorThreads(Integer serverSelectorThreads) {
            this.serverSelectorThreads = serverSelectorThreads;
            return this;
        }

        public BootstrapServerBuilder bizChannelHandler(ChannelHandler bizChannelHandler) {
            this.bizChannelHandler = bizChannelHandler;
            return this;
        }

        public BootstrapServer build() {
            return new BootstrapServer(backLog, listenPort, bossGroupThread, serverSelectorThreads, bizChannelHandler);
        }
    }

    /**
     * 开启服务
     */
    public void start() {
        if (isRunning()) {
            return;
        }
        running = true;
        bossGroup = new NioEventLoopGroup(bossGroupThread, new ThreadFactory() {

            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyBoss_%d", this.threadIndex.incrementAndGet()));
            }
        });
        if (useEpoll()) {
            workerGroup = new EpollEventLoopGroup(serverSelectorThreads, new ThreadFactory() {

                private final AtomicInteger threadIndex = new AtomicInteger(0);

                private final int threadTotal = serverSelectorThreads;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r,
                            String.format("NettyServerEPOLLSelector_%d_%d",
                                    threadTotal,
                                    this.threadIndex.incrementAndGet()));
                }
            });
        } else {
            workerGroup = new NioEventLoopGroup(serverSelectorThreads, new ThreadFactory() {

                private final AtomicInteger threadIndex = new AtomicInteger(0);

                private final int threadTotal = serverSelectorThreads;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r,
                            String.format("NettyServerNIOSelector_%d_%d",
                                    threadTotal,
                                    this.threadIndex.incrementAndGet()));
                }
            });
        }
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new ServerHandshakeHandler(), new RtmpDecoderHandler(), new RtmpEncoderHandler(),
                                new RtmpCommandHandler(), bizChannelHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, backLog);
        ChannelFuture channelFuture = bootstrap.bind(listenPort);
        channel = channelFuture.channel();
        channelFuture.addListener(channelFuture1 -> running = channelFuture1.isSuccess());
        channelFuture.syncUninterruptibly();
    }

    /**
     * 关闭服务
     */
    public void stop() {
        if (!isRunning()) {
            return;
        }
        running = false;
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    private boolean useEpoll() {
        return Epoll.isAvailable();
    }

}
