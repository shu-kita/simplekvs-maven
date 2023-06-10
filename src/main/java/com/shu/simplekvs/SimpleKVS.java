package com.shu.simplekvs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SimpleKVS {
    private Path dataDir;
    private Map<String, String> memtable;
    private int memtableLimit;
    private List<SSTable> sstableList;
    private WAL wal;

    public SimpleKVS(String dataDir, int memtableLimit) {
        this.dataDir = Paths.get(dataDir);
        
        // ディレクトリが存在しない場合、作成する
        if (!Files.exists(this.dataDir)) {
        	try {
        		Files.createDirectories(this.dataDir);
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
        
        this.memtable = new TreeMap<String, String>();
        this.memtableLimit = memtableLimit;
        
        // SSTable読み込み処理
        this.sstableList = new ArrayList<SSTable>();
        this.loadSSTables(dataDir);

        // WAL読込み処理
        this.wal = new WAL(dataDir);
        try {
        	this.memtable = this.wal.recovery();
        } catch (IOException e) {
        	// TODO
        	//   * 強制終了させる処理
        	//   * log出力処理
        	e.printStackTrace();
        }
    }

    public SimpleKVS(String dataDir) {
        this(dataDir, 1024);
    }

    public SimpleKVS() {
        this(".", 1024);
    }
    
    public String get(String key) {
    	/*
    	 * keyがmemtable, SSTableのどちらにも存在しない場合、
    	 * valueには空文字列が入るようになっている。
    	 * とりあえず動作はするため、この処理にしている。
    	 */
    	String value;
    	if (this.memtable.containsKey(key)) {
            value = this.memtable.get(key);
        } else {
        	value = this.getFromSSTable(key);
        }
    	// 削除されているかチェックしてreturn
    	return this.isDeleted(value) ? null : value;
    }

    public void put(String key, String value) {
    	this.writeWAL(key, value);
        this.memtable.put(key, value);
        if (this.memtable.size() >= this.memtableLimit) {
        	// flush処理
        	try {
        		SSTable sstable = new SSTable(this.dataDir.toString() , this.memtable);
        		this.sstableList.add(sstable);
                this.memtable = new TreeMap<String, String>();
        	} catch (IOException e){
        		e.printStackTrace();
        	}
        }
    }

    public void delete(String key) {
        this.writeWAL(key, "__tombstone__");
        this.memtable.put(key, "__tombstone__");
    }

    /*
     以下、privateのメソッド
     */ 
    private boolean isDeleted(String value) {
        return value.equals("__tombstone__");
    }
    
    private void writeWAL(String key, String value) {
    	try {
    		System.out.println(key + " : " + value);
    		this.wal.put(key, value);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
    
    private void loadSSTables(String dataDir) {
    	//ロードしたSSTableは時系列の古い順でリストに格納される
        File[] files = new File(dataDir).listFiles();
        for (File file : files) {
        	String path = file.getPath();
        	if (path.startsWith("sstab") && path.endsWith(".dat")) {
        		try {
        			this.sstableList.add(new SSTable(path));
        		} catch (IOException e) {
        			// TODO : SSTableが読み込めなかった時の処理
        			e.printStackTrace();
        		}
        	}
        }
    }
    
    private String getFromSSTable(String key) {
    	String value = "";
    	try {
    		//ロード時点で時系列順で入るので、ソートはしていない
    		for (SSTable sstable : this.sstableList) {
            	if (sstable.containsKey(key)) {
            		value = sstable.get(key);
            		break;
            	}
        	}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return value;
    }
    
    private void run() {
    	final int PORT = 10000;

    	try (ServerSocket server = new ServerSocket(PORT)) {
    		System.out.println("start");
    		while(true) {
        		Socket socket = server.accept();
        		InputStreamReader isr = new InputStreamReader(socket.getInputStream());
        		BufferedReader br = new BufferedReader(isr);
        		String message = br.readLine();
        		System.out.println("クライアントからのメッセージ＝" + message);
        		
        		/* messageを以下のようなMapにしたかったんだが、
        		 * get, deleteメソッドにはvalueがないため、難しい
        		 * {method : get, key: key1}
        		 * {method : put, key: key1, value : value1}
        		 * 
        		 * 現状は配列として、インデックスの
        		 * 	0がmethod
        		 * 	1がkey
        		 * 	2がvalue(methodがputの時のみ)
        		 */
        		String[] messageList = message.split(" ");
        		
        		String execRes = this.execOperation(messageList);
        		
    			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
    			writer.println(execRes); 
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
    
    private String execOperation(String[] mList) {
    	/* TODO:
    	 * mListは長さが 2 or 3の配列
    	 * →「put key "v v"」の指定をしたときに、配列のサイズは4になる
    	 * 1や4以上の時の処理が書けていない
    	 * (クライアント側のチェックは書いたので、後回しにしている)
    	 */
    	for (String s : mList) {
    		System.out.println(s);
    	}
    	
    	String method = mList[0];
    	String key = mList[1];
    	String value = mList.length == 3 ? mList[2] : null;
    	System.out.println(method + " : " + key + " : " + value);

    	String result = switch(method) {
    					case "get":
    						yield this.get(key);
			    		case "put":
			    			this.put(key,value);
			    			yield "success put";
			    		case "delete":
			    			this.delete(key);
			    			yield "success delete";
			    		default:
			    			yield "Invalid method";
    					};
    	return result;
    }

    public static void main(String[] args) {
    	SimpleKVS kvs = new SimpleKVS("test");
    	kvs.run();
    }
}
