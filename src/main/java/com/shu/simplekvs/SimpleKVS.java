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
 
    	byte[] byteCode = IOUtils.slice(buffer, 0, 4);
		int methodCode = ByteBuffer.wrap(byteCode).getInt();
		
		String method = this.getMethod(methodCode);
		request.put("method", method);
		
		switch(methodCode) {
			case 0,2 -> {
				// get, deleteの時
				String key = this.getStrFromBytes(buffer, 4);
				request.put("key", key);
			}
			case 1 -> {
				// putの時の処理
				String key = this.getStrFromBytes(buffer, 4);
				request.put("key", key);
				String value = this.getStrFromBytes(buffer, 8 + key.length()); // MethodCode, Key長のByte配列の長さの合計である4*2 = 8を足す 
				request.put("value", value);
			}
			default -> {
				System.out.println("Invalid method");
				// TODO：変なメソッドが来た時の処理がない
			}
		}
		return request;
    }

    private String getStrFromBytes(byte[] bytes, int startIndex) {
    	int endIndex = startIndex + 4; // Key、Value長を4byteの配列に変換しているため、+4としている
    	
    	byte[] lenBytes = IOUtils.slice(bytes, startIndex, endIndex);
    	int length = ByteBuffer.wrap(lenBytes).getInt();
    	
    	startIndex += 4; // StartIndexに取得した4byte分足す
    	endIndex = startIndex + length;
    	
    	byte[] strBytes = IOUtils.slice(bytes, startIndex, endIndex);
    	return new String(strBytes);
    }
    
    private String getMethod(int methodCode) {
    	// methodCode(int)からmethod(String)に変換する
    	String method = switch(methodCode) {
	    	case 0 -> "get";
	    	case 1 -> "put";
	    	case 2 -> "delete";
	    	default -> null;
    	};
    	return method;
    }
    
    private String execOperation(Map<String, String> request) {

    	String result = switch(request.get("method")) {
    					case "get":
    						yield this.get(request.get("key"));
			    		case "put":
			    			this.put(request.get("key"),request.get("value"));
			    			yield "success put";
			    		case "delete":
			    			this.delete(request.get("key"));
			    			yield "success delete";
			    		default:
			    			yield "Invalid method";
    					};
    	return result;
    }
    
    public static void main(String[] args) {
    	if (args.length > 1) {
    		System.out.println("Invalid argments.");
    		System.exit(1);
    	}

    	String dataDir = args.length == 1 ? args[0] : "data"; // dataを保存するディレクトリ(デフォルトはdata)
    	System.out.println("data directory : " + dataDir);
    	SimpleKVS kvs = new SimpleKVS(dataDir);
    	kvs.run();
    }
}
