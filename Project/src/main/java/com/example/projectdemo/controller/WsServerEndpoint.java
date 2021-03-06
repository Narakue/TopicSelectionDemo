package com.example.projectdemo.controller;

import com.example.projectdemo.config.HttpSessionConfigurator;
import com.example.projectdemo.service.SubjectService;
import com.example.projectdemo.util.SpringContextUtil;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Zero
 */
@Component
@ServerEndpoint(value = "/selectSubject", configurator = HttpSessionConfigurator.class)
public class WsServerEndpoint {
    private SubjectService subjectService;
    private WsServer wsServer;
    private Gson gson = new Gson();

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
     */
    private static CopyOnWriteArraySet<WsServerEndpoint> webSocketSet = new CopyOnWriteArraySet<WsServerEndpoint>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    public Session getSession() {
        return session;
    }

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        //加入set中
        webSocketSet.add(this);
        //在线数加1
        addOnlineCount();
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        //从set中删除
        webSocketSet.remove(this);
        //在线数减1
        subOnlineCount();
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message) {
        System.out.println("来自客户端的消息:" + message);
        //群发消息
        if (this.subjectService == null) {
            this.subjectService = SpringContextUtil.getBean("subjectService");
        }
        if (this.wsServer == null) {
            this.wsServer = SpringContextUtil.getBean("wsServer");
        }
        wsServer.onMessage(message);
        String str = gson.toJson(subjectService.subjectDisplay());
        System.out.println("发送给客户端消息:" + str);
        for (WsServerEndpoint item : webSocketSet) {
            try {
                item.sendMessage(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发生错误时调用
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WsServerEndpoint.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WsServerEndpoint.onlineCount--;
    }
}