package com.shu.simplekvs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SimpleKVS {
    private static final int PORT = 10000;
    private Logger logger = Logger.getLogger("SampleLogging");

    private Path dataDir;
    private Map<String, String> memtable;
    private int memtableLimit;
    private List<SSTable> sstableList;
    private WAL wal;

    public SimpleKVS(String dataDir, int memtableLimit) {
    	try {
    		// 出力ファイルを指定
    		FileHandler fh = new FileHandler("test.log", true);
    		// フォーマット指定
    		fh.setFormatter(new SimpleFormatter());
    		// 何をしてる処理かわかっていない（ロガーにファイルを指定している？）
    		this.logger.addHandler(fh);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	this.logger.log(Level.INFO, "Launch SimpleKVS.");
    	

        this.dataDir = Paths.get(dataDir);
    	this.logger.log(Level.INFO, String.format("Set \"%s\" as the data directory.", this.dataDir));
        
        // ディレクトリが存在しない場合、作成する
        if (!Files.exists(this.dataDir)) {
        	this.logger.log(Level.INFO, String.format("\"%s\" isn't exist, so create a directory.", this.dataDir));
        	try {
        		Files.createDirectories(this.dataDir);
        	} catch (IOException e) {
        		this.logger.log(Level.WARNING, "The following exception occurred in the process of creating directory.", e);
        	}
        }
        
        this.memtable = new TreeMap<String, String>();
        this.memtableLimit = memtableLimit;
        this.logger.log(Level.INFO, String.format("Set \"%d\" as number of the limit of memtable.", this.memtableLimit));
        
        // SSTable読み込み処理
        this.sstableList = new ArrayList<SSTable>();
        this.loadSSTables(dataDir);
        
        // WAL読込み処理
        this.logger.log(Level.INFO, String.format("Load WAL."));
        this.wal = new WAL(dataDir);
        try {
        	this.memtable = this.wal.recovery();
        } catch (IOException e) {
        	this.logger.log(Level.WARNING, "The following exception occurred in the process loading WAL.", e);
        }
        this.logger.log(Level.INFO, String.format("Loading WAL is complete."));
        
        this.logger.log(Level.INFO, "Finish to launch SimpleKVS");
    }

    public SimpleKVS(String dataDir) {
        this(dataDir, 1024);
    }

    public SimpleKVS() {
        this(".", 1024);
    }
    
    public String get(String key) {
    	String value;
    	this.logger.log(Level.INFO, String.format("Operation is get. Key is \"%s\"", key));
    	if (this.memtable.containsKey(key)) {
            value = this.memtable.get(key);
        } else {
        	value = this.getFromSSTable(key);
        }
    	// 削除されているかチェックしてreturn
    	return this.isDeleted(value) ? null : value;
    }

    public void put(String key, String value) {
    	this.logger.log(Level.INFO, String.format("Operation is put. Key is \"%s\", Value is \"%s\"", key, value));

    	this.writeWAL(key, value);
    	
    	this.memtable.put(key, value);
    	this.logger.log(Level.INFO, String.format("Key \"%s\" and Value \"%s\" are written to Memtable.", key, value));

        if (this.memtable.size() >= this.memtableLimit) {
        	// flush処理
        	this.logger.log(Level.INFO, "Number of rows in memtable have reached the limit, so flush memtable to SSTable");
        	try {
        		SSTable sstable = new SSTable(this.dataDir.toString() , this.memtable);
        		this.sstableList.add(sstable);
                this.memtable = new TreeMap<String, String>();
        	} catch (IOException e){
        		this.logger.log(Level.WARNING, "The following exception occurred in the process flush memtable to SSTable.", e);
        	}
        }
    }

    public void delete(String key) {
    	this.logger.log(Level.INFO, String.format("Operation is delete. Key is \"%s\"", key));
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
    		this.wal.put(key, value);
        	this.logger.log(Level.INFO, String.format("Key \"%s\" and Value \"%s\" are written to WAL.", key, value));
    	} catch (IOException e) {
    		this.logger.log(Level.WARNING, "The following exception occurred in the process writing to WAL.", e);
    	}
    }
    
    private void loadSSTables(String dataDir) {
    	this.logger.log(Level.INFO, "Load SSTables.");
    	//ロードしたSSTableは時系列の古い順でリストに格納される
        File[] files = new File(dataDir).listFiles();
        for (File file : files) {
        	String path = file.getPath();
        	if (path.startsWith("sstab") && path.endsWith(".dat")) {
        		this.logger.log(Level.INFO, String.format("Load SSTable \"%s\"", path));
        		try {
        			this.sstableList.add(new SSTable(path));
        		} catch (IOException e) {
        			this.logger.log(Level.WARNING, String.format("The following exception occurred in the process loading SSTable \"%s\".", path), e);
        		}
        		this.logger.log(Level.INFO, String.format("Loading SSTable \"%s\" is complete.", path));
        	}
        }
        this.logger.log(Level.INFO, "Loading SSTables is complete.");
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
    		this.logger.log(Level.WARNING, "The following exception occurred in the process getting value in SSTable.", e);
    	}
    	return value;
    }
    
    
    // TODO
    // ここより下（Socket Server機能 + リクエストを処理している部分）のログ機能
    private void run() {
    	try (ServerSocket server = new ServerSocket(SimpleKVS.PORT)) {
    		while(true) {
    			Socket socket = server.accept();
    			InputStream is = socket.getInputStream();
    			byte [] buf = new byte[1024];
    			is.read(buf);

    			Map<String, String> req = this.getRequest(buf);
    			String execRes = this.execOperation(req);

    			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
    			writer.println(execRes);
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
    
    private Map<String, String> getRequest(byte[] buffer) {
    	Map<String, String> request = new HashMap<>();
 
    	byte[] byteCode = ArrayUtil.slice(buffer, 0, 4);
		int methodCode = ByteBuffer.wrap(byteCode).getInt();
		
		String method = this.getMethod(methodCode);
		request.put("method", method);
		
		String key;
		String value;
		switch(methodCode) {
			case 0:
				// getの時
				key = this.getStrFromBytes(buffer, 4);
				request.put("key", key);
				break;
			case 1:
				// putの時の処理
				key = this.getStrFromBytes(buffer, 4);
				request.put("key", key);
				value = this.getStrFromBytes(buffer, 8 + key.length()); // MethodCode, Key長のByte配列の長さの合計である4*2 = 8を足す 
				request.put("value", value);
				break;
			case 2:
				// deleteの時
				key = this.getStrFromBytes(buffer, 4);
				request.put("key", key);
				break;
			default:
				System.out.println("Invalid method");
				break;
				// TODO：変なメソッドが来た時の処理がない
		}
		return request;
    }

    private String getStrFromBytes(byte[] bytes, int startIndex) {
    	int endIndex = startIndex + 4; // Key、Value長を4byteの配列に変換しているため、+4としている
    	
    	byte[] lenBytes = ArrayUtil.slice(bytes, startIndex, endIndex);
    	int length = ByteBuffer.wrap(lenBytes).getInt();
    	
    	startIndex += 4; // StartIndexに取得した4byte分足す
    	endIndex = startIndex + length;
    	
    	byte[] strBytes = ArrayUtil.slice(bytes, startIndex, endIndex);
    	return new String(strBytes);
    }
    
    private String getMethod(int methodCode) {
    	// methodCode(int)からmethod(String)に変換する
    	String method;
    	switch (methodCode) {
    	case 0:
    		method = "get";
    		break;
    	case 1:
    		method = "put";
    		break;
    	case 2:
    		method = "delete";
    		break;
    	default:
    		method = null;
    		break;
    	}
    	return method;
    }
    
    private String execOperation(Map<String, String> request) {

    	String result;
    	switch(request.get("method")) {
    		case "get":
    			result = this.get(request.get("key"));
    			break;
			case "put":
			    this.put(request.get("key"),request.get("value"));
			    result = "success put";
			    break;
			case "delete":
				this.delete(request.get("key"));
				result = "success delete";
				break;
			default:
			    result =  "Invalid method";
			    break;
    	};
    	return result;
    }
    
    public static void main(String[] args) {
    	if (args.length > 1) {
    		System.out.println("Invalid argments.");
    		System.exit(1);
    	}

    	String dataDir = args.length == 1 ? args[0] : "data"; // dataを保存するディレクトリ(デフォルトはdata)
    	SimpleKVS kvs = new SimpleKVS(dataDir);
    	kvs.run();
    }
}
