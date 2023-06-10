package com.shu.simplekvs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class skvs {
	private static final String help = """
			get, deleteの時
			    java skvs <get or delete> <key>
			putの時
			    java skvs put <key> <value>
			""";
	
	public static void main(String[] args) {
		if (!skvs.checkArgs(args)) {
			System.out.println(String.format("""
					[ERROR] 引数が間違っています
					Usage :					
					%s
					""", skvs.help));
			return;
		}
		
		byte[][] byteArgs = skvs.convArgsToBytes(args);
		
		byte[] bytes = byteArgs[0];
		for(int i = 1; i < byteArgs.length; i++) {
			bytes = IOUtils.combineBytes(bytes, byteArgs[i]);
		}

		// クライアントソケットを生成
		try (Socket socket = new Socket("localhost", 10000);
			 OutputStream os = socket.getOutputStream();
			 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
		) {
			os.write(bytes);
			os.flush();
			System.out.println(reader.readLine());
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private static boolean checkArgs(String args[]) {
		if (args.length == 0) {
			return false;
		}
		
		boolean result = switch (args[0]) {
		case "get", "delete" -> args.length == 2 ? true : false;
		case "put" -> args.length == 3 ? true : false;
		default -> false;
		};
		return result;
	}
	
	private static byte[][] convArgsToBytes(String[] args) {
		byte [][] byteArgs = new byte[args.length*2-1][];
		int methodCode = skvs.getMethodCode(args[0]);

		int index = 0;
		byteArgs[index] = ByteBuffer.allocate(4).putInt(methodCode).array();

		for (String strKV : IOUtils.slice(args, 1, args.length)) {
			byte[][] bytes = IOUtils.getByteStrAndLength(strKV);
	        byte[] byteStr = bytes[0];
	        byte[] byteLenStr = bytes[1];
			byteArgs[++index] = byteLenStr;
			byteArgs[++index] = byteStr;
		}
		return byteArgs;
	}
	
	private static int getMethodCode(String method) {
		int methodCode = switch(method) {
			case "get" -> 0;
			case "put" -> 1;
			case "delete" -> 2;
			default -> 99;
		};
		return methodCode;
	}
	
	public static byte[] slice(byte[] arr, int stIndx, int enIndx) {
        byte[] sclicedArr = Arrays.copyOfRange(arr, stIndx, enIndx);
        return sclicedArr;
    }
}
