package xyz.ycgame.rtmpserver.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BootstrapServerTest {

    private BootstrapServer bootstrapServer;

    @BeforeEach
    void setUp() {
        bootstrapServer = BootstrapServer.builder().backLog(128).listenPort(1935).bossGroupThread(12).serverSelectorThreads(4).bizChannelHandler(new PrintMsgHandler()).build();
        Thread thread = new Thread();
        thread.start();
    }

    @Test
    public void testStart() throws InterruptedException {
        bootstrapServer.start();

    }
}