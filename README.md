# SimpleKVS

SimpleKVSは、LSM Tree を実装したKey-Value Storeです。  
※学習目的で作成

## 使用方法

SimpleKVSクラスをインスタンス化して使う
* サーバ
  ```
  java -cp . SimpleKVS
  ```
* クライアント
  ```
  java -cp . skvs <メソッド> <key> <value>
  ```

## ファイル構成

memtableの要素数が1024を超えたら、SSTableにflashされる。
flash時にindexファイルも作成される。
```
data_dir
    ├─sstab_<unixtime>.dat # SSTable
    ├─sstab_<unixtime>.dat.index # indexファイル
    └─wal.dat # Write-Ahead Log
log_dir
    └─SimpleKVS.log # ApplicationのLogファイル
```

## ライセンス

SimpleKVSはMITライセンスの下でリリースされています。[LICENSEファイル](./LICENSE)を参照してください。