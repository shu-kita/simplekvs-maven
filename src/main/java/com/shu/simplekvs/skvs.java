package com.shu.simplekvs;

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
		
		byte[] numbyte = skvs.slice(bytes, 0, 4);
		int num = ByteBuffer.wrap(numbyte).getInt();
		System.out.println(num);
		
		// クライアントソケットを生成
		/*
		try (Socket socket = new Socket("localhost", 10000);
			 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
		) {
			String message = String.join(" ", args);
			writer.println(message);
			System.out.println(reader.readLine());
		} catch (IOException e){
			e.printStackTrace();
		}
		*/
	}
	
	private static boolean checkArgs(String args[]) {
		if (args.length == 0) {
			return false;
		}
		
		boolean result = switch (args[0]) {
		case "get", "delete"-> args.length == 2 ? true : false;
		case "put"-> args.length == 3 ? true : false;
		default -> false;
		};
		return result;
	}
	
	private static byte[][] convArgsToBytes(String[] args) {
		byte [][] byteArgs = new byte[args.length*2][];
		
		for (int i=0; i < args.length ; i++) {
			byte[][] bytes = IOUtils.getByteStrAndLength(args[i]);
	        byte[] byteStr = bytes[0];
	        byte[] byteLenStr = bytes[1];
			int byteArgsIndex = i*2;
			byteArgs[byteArgsIndex] = byteLenStr;
			byteArgs[byteArgsIndex+1] = byteStr;
		}
		return byteArgs;
	}
	
	public static byte[] slice(byte[] arr, int stIndx, int enIndx) {
        byte[] sclicedArr = Arrays.copyOfRange(arr, stIndx, enIndx);
        return sclicedArr;
    }
}
