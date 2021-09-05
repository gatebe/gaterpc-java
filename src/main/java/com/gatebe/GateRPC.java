package com.gatebe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hprose.client.HproseClient;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class GateRPC {
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024*4];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
    public static byte[] MmtlsPost(String url, byte[] param) {
        byte[] array = new byte[]{};
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("User-Agent", "MicroMessenger Client");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Upgrade", "mmtls");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream os = conn.getOutputStream();
            os.write(param);
            array = toByteArray(conn.getInputStream());
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        return array;
    }
    interface  LoginInterface{
        PackRequest CheckQrcode(String guid,byte[] response);
    }
    interface  StableInterface{
        PackRequest GetQrcode(String guid);
        PackResponse GetQrcode(String guid,byte[] response);
        PackRequest CheckQrcode(String guid);
        PackResponse CheckQrcode(String guid,byte[] response);
        PackResponse ManualAuth(String guid,byte[] response);
        PackRequest LogOut(String guid);
        PackResponse LogOut(String guid,byte[] response);
        PackRequest HeartBeat(String guid);
        PackResponse HeartBeat(String guid,byte[] response);
        PackRequest Awake(String guid);
        PackResponse Awake(String guid,byte[] response);
        PackRequest GetProfile(String guid);
        PackResponse GetProfile(String guid,byte[] response);
        PackRequest SendText(String guid,String userName,String content);
        PackResponse SendText(String guid,byte[] response);
        PackRequest SendEmoji(String guid,String userName,String md5,int gameType,String content);
        PackResponse SendEmoji(String guid,byte[] response);
    }


    class HyBridEcdhData{
        public byte[] HashFinal;
        public byte[] EncryptKeyData;
        public byte[] EncryptKeyExtendData;
        public byte[] EncryptData;
    }

    class Package{
        public byte[] hash;
        public byte[] data;
        public byte[] pubkey;
        public byte[] prikey;
        public HyBridEcdhData brid;
        public byte[] ToBytes(){
            try {
                return new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(this);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return new byte[]{};
        }
    }

    class PackRequest{
        public String remark;
        public String message;
        public int code;
        public String url;
        public int flag;
        public Package response;
        public PackRequest Mmtls(){
            this.response.data = MmtlsPost(this.url,this.response.data);
            return this;
        }

        public PackResponse Empty(){
            PackResponse response = new PackResponse();
            response.message = this.message;
            response.code = this.code;
            response.remark = this.remark;
            response.url = this.url;
            response.flag = this.flag;
            return  response;
        }
    }

    class PackResponse{
        public String remark;
        public String message;
        public int code;
        public String url;
        public int flag;
        public Object response;
        public  String ToJson() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return "{}";
        }
    }


    public String endPoint;
    public String guid;
    private HproseClient client;
    private StableInterface instance;
    private LoginInterface login;
    public GateRPC(String guid,String endPoint){
        this.endPoint = endPoint;
        this.guid = guid;
        try {
            this.client = HproseClient.create("http://"+endPoint);
            this.instance = this.client.useService(StableInterface.class);
            this.login = this.client.useService(LoginInterface.class);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public PackResponse GetQrcode() {
        PackRequest request = this.instance.GetQrcode(this.guid);
        return request.code<0 ?  request.Empty() : this.instance.GetQrcode(this.guid,request.Mmtls().response.ToBytes());
    }

    public PackResponse CheckQrcode(){
        PackRequest request = this.instance.CheckQrcode(this.guid);
        if(request.code<0 || request.code ==1) return request.Empty();
        PackResponse response = this.instance.CheckQrcode(this.guid,request.Mmtls().response.ToBytes());
        if(response.flag !=2) return response;
        request = this.login.CheckQrcode(this.guid,request.response.ToBytes());
        return this.instance.ManualAuth(this.guid,request.Mmtls().response.ToBytes());
    }

    public PackResponse HeartBeat(){
        PackRequest request = this.instance.HeartBeat(this.guid);
        return request.code<0 ?  request.Empty() : this.instance.HeartBeat(this.guid,request.Mmtls().response.ToBytes());
    }

    public PackResponse LogOut(){
        PackRequest request = this.instance.LogOut(this.guid);
        return request.code<0 ?  request.Empty() : this.instance.LogOut(this.guid,request.Mmtls().response.ToBytes());
    }

    public PackResponse Awake(){
        PackRequest request = this.instance.Awake(this.guid);
        return request.code<0 ?  request.Empty() : this.instance.Awake(this.guid,request.Mmtls().response.ToBytes());
    }

    public PackResponse GetProfile(){
        PackRequest request = this.instance.GetProfile(this.guid);
        return request.code<0 ?  request.Empty() : this.instance.GetProfile(this.guid,request.Mmtls().response.ToBytes());
    }

    public PackResponse SendText(String userName,String content){
        PackRequest request = this.instance.SendText(this.guid,userName,content);
        return request.code<0 ?  request.Empty() : this.instance.SendText(this.guid,request.Mmtls().response.ToBytes());
    }

    public PackResponse SendEmoji(String userName,String md5,int gameType,String content){
        PackRequest request = this.instance.SendEmoji(this.guid,userName,md5,gameType,content);
        return request.code<0 ?  request.Empty() : this.instance.SendEmoji(this.guid,request.Mmtls().response.ToBytes());
    }
}
