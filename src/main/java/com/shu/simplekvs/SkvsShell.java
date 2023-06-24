package com.shu.simplekvs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SkvsShell {
	private static final String HELP = "get, deleteの時\n\tget(or delete) \"<key>\"\nputの時\n\tput \"<key>\" \"<value>\"";
	private static final String EXIT = "exit";
	
	public static void main(String[] args) {
		// TODO
		//「get "key1"」と「get "key1" 」で動作が違う(末尾に半角スペースがある/ない)
		// 原因不明
		
		try(
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr);
		){
			String input = null;
			while (true){
				System.out.print("> ");
				input = br.readLine();
				
				if (SkvsShell.EXIT.equals(input)) {
					break;
				}
				
				String[] inputList = SkvsShell.parseInput(input);
				if (!SkvsShell.checkInput(inputList)) {
					System.out.println(String.format("\n[ERROR] 入力が間違っています\nUsage :\n%s", SkvsShell.HELP));
					continue;
				}

				byte[][] byteArgs = SkvsShell.convArgsToBytes(inputList);
				byte[] bytes = ArrayUtil.combineArray(byteArgs);
				SkvsShell.sendData(bytes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean checkInput(String inputs[]) {
		if (inputs.length == 0) {
			return false;
		}
		
		boolean result;
		switch (inputs[0]) {
		case "get":
			result = inputs.length == 2 ? true : false;
			break;
		case "delete":
			result = inputs.length == 2 ? true : false;
			break;
		case "put":
			result = inputs.length == 3 ? true : false;
			break;
		default:
			result = false;
			break;
		};
		return result;
	}
	
	private static byte[][] convArgsToBytes(String[] args) {
		byte [][] byteArgs = new byte[args.length*2-1][];
		int methodCode = SkvsShell.getMethodCode(args[0]);

		int index = 0;
		byteArgs[index] = ByteBuffer.allocate(4).putInt(methodCode).array();

		for (String strKV : ArrayUtil.slice(args, 1, args.length)) {
			byte[][] bytes = IOUtils.getByteStrAndLength(strKV);
	        byte[] byteStr = bytes[0];
	        byte[] byteLenStr = bytes[1];
			byteArgs[++index] = byteLenStr;
			byteArgs[++index] = byteStr;
		}
		return byteArgs;
	}
	
	private static int getMethodCode(String method) {
		int methodCode;
		switch(method) {
			case "get":
				methodCode = 0;
				break;
			case "put":
				methodCode = 1;
				break;
			case "delete":
				methodCode = 2;
				break;
			default:
				methodCode = 99;
		};
		return methodCode;
	}
	
	private static String[] parseInput(String input) {
		String[] parsed = input.split(" \"");
		for (int i=0; i < parsed.length; i++) {
			parsed[i] = parsed[i].replace("\"", "");
		}
		return parsed;
	}
	
	private static void sendData(byte[] bytes) throws IOException{
		try(
			Socket socket = new Socket("localhost", 10000);
			OutputStream os = socket.getOutputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
		){
			os.write(bytes);
			os.flush();
			System.out.println(reader.readLine());
		}
	}
}
