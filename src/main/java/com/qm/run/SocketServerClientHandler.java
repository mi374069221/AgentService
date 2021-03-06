package com.qm.run;

import com.qm.Utils.PropertyUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Socket;

public class SocketServerClientHandler extends Thread {
    //每个消息通过Socket进行传输
    private Socket clientConnectSocket;
    public SocketServerClientHandler(Socket clientConnectSocket){
        this.clientConnectSocket = clientConnectSocket;
    }

    @Override
    public void run(){
        try {
            InputStream inputStream = clientConnectSocket.getInputStream();
            String[] clientAddressPort = clientConnectSocket.getRemoteSocketAddress().toString().split(":");
            String clientAddress = clientAddressPort[0].substring(1);
            String savePath = PropertyUtil.getProperty("sys.data.path")+File.separator+clientAddress;//要保存的路径
            System.out.println(savePath);
            while (true) {

                //String savePath = "C:"+File.separator+"Users"+File.separator+"Administrator"+File.separator+"Desktop"+File.separator+"olclog";
                int size = 10 * 1024;
                byte[] buf = new byte[size];
                int len = -1;
                byte[] bs = new byte[size];
                while ((len = inputStream.read(buf, 0, size)) != -1) {
                    writeData(buf, 0, len, savePath, bs);

                }
                System.out.println("accept file data success");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static FileOutputStream out=null;
    static long count=0;//计算是否完成数据的读取，开始下一条命令
    static int cmd=-1;

    static int bsl=0;
    private static void writeData(byte[] buf,int off, int len,String savePath,byte[] bs) throws Exception{
        if(len-off==0)return;
        System.out.println("偏移量："+off+"命令"+cmd+"数量："+count);
        int i=off;
        if(count==0l){//如果一条命令的数据已经读完就开始读取下一条命令
            cmd=buf[i++];
            System.out.println("获取命令："+cmd);
            count=-1l;
            if(len-i==0)return;
            writeData(buf,i,len,savePath,bs);
        }else if(count==-1l){//读取文件（夹）名称的长度或文件的大小
            System.out.println("获取长度");
            switch (cmd){
                case 0:
                    if(len-i+bsl<8){
                        System.arraycopy(buf, i, bs, bsl, len-i);
                        System.out.println("读取长度1："+(len-i)+"  未读取完");
                        bsl=len-i;
                        i=0;
                        return;
                    }
                    System.arraycopy(buf, i, bs, bsl, 8-bsl);
                    System.out.println("读取长度1："+(8-bsl)+"  读取完");
                    count=bytesToLong(bs, 0);
                    i+=8-bsl;
                    bsl=0;
                    writeData(buf,i,len,savePath,bs);
                    break;
                case 1:
                case 2:
                    if(len-i+bsl<4){
                        System.arraycopy(buf, i, bs, bsl, len-i);
                        System.out.println("读取长度2："+(len-i)+"  未读取完");
                        bsl=len-i;
                        i=0;
                        return;
                    }
                    System.arraycopy(buf, i, bs, bsl, 4-bsl);
                    System.out.println("读取长度2："+(4-bsl)+"  读取完");
                    count=bytesToInt(bs, 0);
                    i+=4-bsl;
                    bsl=0;
                    writeData(buf,i,len,savePath,bs);
                    break;
            }
        }else{//写入文件或创建文件夹、创建文件输出流
            System.out.println("3");
            switch (cmd){
                case 0:
                    System.out.println("写入文件");
                    if(len-i-count>0){
                        try{
                            System.out.println("写入文件      长度："+count+"文件写入完成");
                            out.write(buf, i, (int)count);
                            i+=count;
                            count=0;
                            out.flush();
                        }finally{
                            if(out!=null)out.close();
                        }
                        writeData(buf,i,len,savePath,bs);
                    }else{
                        System.out.println("写入文件      长度："+(len-i)+"文件写入没有完成");
                        out.write(buf,i,len-i);
                        count-=len-i;
                        i=0;
                    }break;
                case 1:
                    if(len-i-count<0){
                        System.out.println("获取文件名字："+(len-i)+"写入没有完成    剩余长度"+count);
                        System.arraycopy(buf, i, bs, bsl, len-i);
                        bsl+=len-i;
                        count-=bsl;
                        i=0;
                        return;
                    }else{
                        System.out.println("获取文件名字："+(count-bsl)+"写入完成    剩余长度");
                        System.arraycopy(buf, i, bs, bsl, (int)count);
                        String name=new String(bs,0,(int)count+bsl);
                        System.out.println("文件："+savePath+File.separator+name);
                        out=new FileOutputStream(savePath+File.separator+name);
                        bsl=0;
                        i+=count;
                        count=0;
                        writeData(buf,i,len,savePath,bs);
                    }
                    break;
                case 2:
                    if(len-i-count<0){
                        System.out.println("获取文件夹名字："+(len-i)+"写入没有完成    剩余长度"+count);
                        System.arraycopy(buf, i, bs, bsl, len-i);
                        bsl+=len-i;
                        count-=bsl;
                        i=0;
                        return;
                    }else{
                        System.out.println(len+"   "+count+"   "+bsl+"  ");
                        System.out.println("获取文件夹名字："+(count-bsl)+"写入完成    剩余长度");
                        System.arraycopy(buf, i, bs, bsl, (int)count);
                        String name=new String(bs,0,bsl+(int)count);
                        File file=new File(savePath+File.separator+name);
                        bsl=0;
                        i+=count;
                        count=0;
                        if(!file.exists()){
                            file.mkdirs();
                        }
                        System.out.println("文件夹："+savePath+File.separator+name);
                        writeData(buf,i,len,savePath,bs);
                    }
                    break;
            }
        }

    }

    private static int bytesToInt(byte[] buf,int off){
        int i=0;
        i=i|((buf[off]&255)<<24);
        i=i|((buf[off+1]&255)<<16);
        i=i|((buf[off+2]&255)<<8);
        i=i|(buf[off+3]&255);
        return i;
    }

    private static long bytesToLong(byte[] buf,int off){
        long i=0;
        i=i|(((long)buf[off]&255)<<56)
                |(((long)buf[off+1]&255)<<48)
                |(((long)buf[off+2]&255)<<40)
                |(((long)buf[off+3]&255)<<32)
                |(((long)buf[off+4]&255)<<24)
                |(((long)buf[off+5]&255)<<16)
                |(((long)buf[off+6]&255)<<8)
                |((long)buf[off+7]&255);
        return i;
    }

}
