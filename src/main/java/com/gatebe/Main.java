package com.gatebe;
public class Main {

    public static void main(String[] args) {
        GateRPC rpc = new GateRPC("e9bc7a7e-23d3-46fc-98aa-cbe484d6f661","127.0.0.1:6062");
        System.out.println(rpc.GetQrcode().ToJson());
        //System.out.println(rpc.CheckQrcode().ToJson());
        //System.out.println(rpc.HeartBeat().ToJson());
    }


}
