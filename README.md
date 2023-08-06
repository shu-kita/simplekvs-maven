# SimpleKVS

SimpleKVSは、LSM Tree を実装したKey-Value Storeです。  
※学習目的で作成

## 使用方法

jarファイルが存在するディレクトリで実行することを想定しているため、適宜読み替えて実行する。
* サーバ
  ```
  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SimpleKVS <dataDir>　<logDir> <memtableLimit>

  or

  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SimpleKVS <dataDir>　<logDir>
  
  or
  
  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SimpleKVS <dataDir>

  or

  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SimpleKVS
  ```
  引数の規定値は以下
  * dataDir -> data
  * logDir -> log
  * memtableLimit -> 1024
* クライアント  
  SkvsShellを実行すると、対話的に実行できる。  
  get/put/deleteの実行は以下のように行う。
  ```
  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SkvsShell
  > get "<key>"
  > put "<key>", "<value>"
  > delete "<key>"
  ```

## ファイル構成

* データを格納するディレクトリ(dataDir)  
  memtableの要素数が1024を超えたら、SSTableにflashされる。(既定値の場合)    
  flashすると、拡張子が「.dat」、「.index」の2つのファイルが作成される。  
  ```
  dataDir
      ├─sstab_<unixtime>.dat # SSTable
      ├─sstab_<unixtime>.dat.index # indexファイル
      └─wal.dat # Write-Ahead Log
  ```
* ログファイルを格納するディレクトリ(logDir)  
  ```
  logDir
      ├─SimpleKVS.log # logファイル
      └─SimpleKVS.log.lck # lockファイル(2重起動防止用)
  ```

## TODO

* とりあえず、最低限の実装はできた。
  まだ真剣に考えていないが、とりあえず上げると以下。
  * ソースコードを綺麗にする
  * テストコードを書く
    →これから取り組んでいる(08/06時点)
  * MinorCompactionの実装
  * MajorCompactionの処理速度改善
  * 改善案(下記)に取り組む

## 改善案

* 現在、実行時の引数で受け取っているもの(dataDir, logDir, MemtableLimit)を設定ファイルに書くようにする

## ライセンス

SimpleKVSはMITライセンスの下でリリースされています。[LICENSEファイル](./LICENSE)を参照してください。
