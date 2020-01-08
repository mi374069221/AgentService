package com.qm.run;
/**
 * 服务端的启动类
 * @author: codermi
 * @date: 2020/1/8 12:00
 */

public class SorketServer {
    public static void main(String[] args) {
        SocketServerListenHandler socketServerListenHandler = new SocketServerListenHandler(80);
        socketServerListenHandler.listenClientConnect();
    }

}
