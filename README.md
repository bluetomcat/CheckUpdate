[ ![Download](https://api.bintray.com/packages/tomcat/maven/checkupdate/images/download.svg) ](https://bintray.com/tomcat/maven/checkupdate/_latestVersion)
[ ![Download](https://jitpack.io/v/bluetomcat/CheckUpdate.svg)](https://jitpack.io/#bluetomcat/CheckUpdate)
___
# CheckUpdate
分离UI和下载，仅包含更新判断和安装逻辑；结合Rxjava链式编程
###  JitPack引用
1、工程`build.gradle`文件
  ```  
  allprojects {
      repositories {
        ...
        maven { url 'https://jitpack.io' }//添加
      }
    }
  ```
2、Module`build.gradle`文件
  ```
  dependencies {
            ...
            implementation 'com.github.bluetomcat:CheckUpdate:1.0.1-bate'//添加
    }
  ```
### jcenter引用
   ```
    dependencies {
             ...
             implementation 'com.github.bluetomcat:checkupdate:1.0.1-bate'//添加
    }
   ```
### 使用
1、初始化对象
```
CheckUpdate<VersionEntity.DataBean> checkUpdate = new CheckUpdate.Builder<>()
                    .setUpdateUiListener((c, d) -> {
                        //提示用户是否更新的UI 使用c.postUpdate(isUpdate)通知下一步  可省 默认上一步结果判断是否更新
                        messageDialog.setMessage(d.getAppDesc())
                                .setTitle("更新提示")
                                .setOnBtnClickListener((v, i, isCancel) -> {
                                    c.postNext(!isCancel);
                                    return false;
                                })
                                .show();
                    })
                    .setNetworkUiListener((c, d) -> {
                        //提示是否非WIFI环境下载文件的UI 使用c.postDownload(isDownload)通知下一步  可省 默认上一步结果+是否已下载文件+网络是否可用判断是否下载
                        messageDialog.setMessage("当前非WIFI环境是否继续下载?")
                                .setTitle("提示")
                                .setOnBtnClickListener((v, i, isCancel) -> {
                                    c.postNext(!isCancel);
                                    return false;
                                })
                                .show();
                    })
                    .setOnDownloadListener((file, url, downLoadResult, date) -> {
                        downLoadHelper.StartDownload(file, url, downLoadResult);//自行依赖下载工具  将下载结果通过downLoadResult回传
                    })
                    .setCacheDir(getExternalCacheDir())//设置缓存文件夹 可省
                    .build();
```
2、使用
```
       mRetrofit.create(Api.class)
                .getVersionInfo(5)//请求网络版本
                .compose(checkUpdate.checkUpdate(this))//检查逻辑
                .subscribe(new SampleObserver<NoteEvent>() {

                    @Override
                    public void onNext(NoteEvent noteEvent) {
                        super.onNext(noteEvent);
                        BaseUtils.makeText(noteEvent.getMsg());
                    }
                    
                    @Override
                    public void onError(@NotNull Throwable e) {
                        super.onError(e);
                        BaseUtils.makeText("检查更新失败");
                    }
                });
```

### 鸣谢
  感谢tbruyelle的[RxPermissions](https://github.com/tbruyelle/RxPermissions)提供的思路
### 后记
  感谢您的耐心阅读，如果觉得赞请点击右上角的star按钮，如果觉得烂请留下您的宝贵意见。
  
