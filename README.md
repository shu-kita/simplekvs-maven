# SimpleKVS

SimpleKVSは、LSM Tree を実装したKey-Value Storeです。  
※学習目的で作成

## 使用方法

jarファイルが存在するディレクトリで実行することを想定しているため、適宜読み替えて実行する。
* サーバ
  ```
  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SimpleKVS <dataDir>

  or

  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SimpleKVS
  ```
* クライアント
  ```
  # java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.skvs <method> <key> <value>
  ```

## ファイル構成

memtableの要素数が1024を超えたら、SSTableにflashされる。
flash時にindexファイルも作成される。
```
data_dir
    ├─sstab_<unixtime>.dat # SSTable
    ├─sstab_<unixtime>.dat.index # indexファイル
    └─wal.dat # Write-Ahead Log
```

## TODO
* ログ出力の処理ない

## ライセンス

SimpleKVSはMITライセンスの下でリリースされています。[LICENSEファイル](./LICENSE)を参照してください。
