package com.shu.simplekvs;


import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class SimpleKVSTest extends SimpleKVS {
	private static final String TESTDATA_DIR = "./src/test/tmp/data";
	private static final String TESTLOG_DIR = "./src/test/tmp/log";
	private static final int MEMTABLE_LIMIT = 5;

	@Test
	void testGet() {
		String testKey = "testKey";
		String testValue = this.generateRandomString(10);
		SimpleKVS kvs = initKVS();
		kvs.put(testKey, testValue);
		assertEquals(kvs.get(testKey), testValue);
	}
	
	@Test
	void testGetFromSSTable() {
		SimpleKVS kvs = initKVS();
		for (int i=0; i<MEMTABLE_LIMIT; i++) {
			kvs.put(Integer.valueOf(i).toString(), String.format("value%d", i));
		}
		
		for (int j=0; j<MEMTABLE_LIMIT; j++) {
			assertEquals(kvs.get(Integer.valueOf(j).toString()), String.format("value%d", j));
		}
	}

	private String generateRandomString(int length) {
		String theAlphaNumericS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; 
		//create the StringBuffer
		StringBuilder builder = new StringBuilder(length); 
		for (int i = 0; i < length; i++) {
			// generate numeric
			int index = (int)(theAlphaNumericS.length() * Math.random()); 
			// add the characters
			builder.append(theAlphaNumericS.charAt(index)); 
		}
		return builder.toString(); 
	}

	private SimpleKVS initKVS() {
		// 空の状態のSimpleKVSインスタンスを生成する
		
		// ./src/test/tmp配下のディレクトリの削除
		File tmp = new File("./src/test/tmp");
		File[] fileObjects = tmp.listFiles();
		for (File dir : fileObjects) {
			for (File file: dir.listFiles())
			file.delete();
		}
		
		// インスタンス生成, return
		return new SimpleKVS(TESTDATA_DIR, TESTLOG_DIR, MEMTABLE_LIMIT);
	}
}
