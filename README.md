# SimpleKVS

SimpleKVSは、LSM Tree を実装したKey-Value Storeです。  
※学習目的で作成

## 使用方法

jarファイルが存在するディレクトリで実行することを想定しているため、適宜読み替えて実行する。
* サーバ
  ```
  java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.SimpleKVS
  ```
* クライアント
  ```
  java -cp simplekvs-maven-0.0.1-SNAPSHOT.jar com.shu.simplekvs.skvs <メソッド> <key> <value>
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

## TODO
* データ(*.dat)を置くディレクトリが「test」で固定になっている  
  → 引数などで指定できるようにする
* skvsで実行する際に、key, valueに空白のみとかが指定できる。  
  → 指定できるのがいいのかを考慮する必要があるとは思うが、指定できる必要性を感じない  
  　→ 指定できないようにしたい

## ライセンス

SimpleKVSはMITライセンスの下でリリースされています。[LICENSEファイル](./LICENSE)を参照してください。
