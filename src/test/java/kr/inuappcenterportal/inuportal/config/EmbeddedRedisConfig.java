package kr.inuappcenterportal.inuportal.config;

import io.netty.util.internal.SocketUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

@Profile("test")
@Configuration
public class EmbeddedRedisConfig {
    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        if (isRedisRunning(6379)) {
            System.setProperty("spring.data.redis.port", "6379");
            return;
        }
        int port = findAvailablePort();
        System.setProperty("spring.data.redis.port", String.valueOf(port));
        redisServer = new RedisServer(port);
        redisServer.start();
    }

    private boolean isRedisRunning(int port) {
        try (java.net.Socket socket = new java.net.Socket("localhost", port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
    private int findAvailablePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
}
