-ignorewarnings                                     # 是否忽略警告
-optimizationpasses 7                               # 指定代码的压缩级别(在0~7之间，默认为5)
-dontusemixedcaseclassnames                         # 是否使用大小写混合(windows大小写不敏感，建议加入)
-dontskipnonpubliclibraryclasses                    # 是否混淆非公共的库的类
-dontskipnonpubliclibraryclassmembers               # 是否混淆非公共的库的类的成员
#-dontpreverify                                      # 混淆时是否做预校验(Android不需要预校验，去掉可以加快混淆速度)
-verbose                                            # 混淆时是否记录日志(混淆后会生成映射文件)
# 混淆时所采用的算法(谷歌推荐算法)
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
# 添加支持的jar(引入libs下的所有jar包)
#-libraryjars libs/httpmime-4.1.3.jar
# 将文件来源重命名为“SourceFile”字符串
-renamesourcefileattribute SourceFile
# 保持注解不被混淆
-keepattributes *Annotation*
-keep class * extends java.lang.annotation.Annotation {*;}
-keep interface * extends java.lang.annotation.Annotation {*;}#：保持注解不被混淆
-keepattributes Signature#保持泛型不被混淆
-keepattributes EnclosingMethod#保持反射不被混淆
-keepattributes Exceptions#保持异常不被混淆
-keepattributes InnerClasses#保持内部类不被混淆
-keepattributes SourceFile,LineNumberTable,Deprecated#抛出异常时保留代码行号
#---------------------------------默认保留区---------------------------------
# 保持基本组件不被混淆
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# 保持 Google 原生服务需要的类不被混淆
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService

# Support包规则
-dontwarn android.support.**
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.support.annotation.**

# androidX
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

# 保持测试相关的代码
-dontnote junit.framework.**
-dontnote junit.runner.**
-dontwarn android.test.**
-dontwarn androidx.test.**
-dontwarn org.junit.**

# 保持 native 方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留自定义控件(继承自View)不被混淆
-keep public class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <methods>;#保持该类下所有的共有方法不被混淆
    public *;#保持该类下所有的共有内容不被混淆
}

# 保留指定格式的构造方法不被混淆
-keepclasseswithmembers class * {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <methods>;#保持该类下所有的共有方法不被混淆
    public *;#保持该类下所有的共有内容不被混淆
}

# 保留在Activity中的方法参数是view的方法(避免布局文件里面onClick被影响)
-keepclassmembers class * extends android.app.Activity {
    public <methods>;#保持该类下所有的共有方法不被混淆
    public *;#保持该类下所有的共有内容不被混淆
}

# 保持枚举 enum 类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持R(资源)下的所有类及其方法不能被混淆
-keep class **.R$* { *; }

# 保持 Parcelable 序列化的类不被混淆(注：aidl文件不能去混淆)
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 需要序列化和反序列化的类不能被混淆(注：Java反射用到的类也不能被混淆)
-keepnames class * implements java.io.Serializable

# 保持 Serializable 序列化的类成员不被混淆
-keepclassmembers class * implements java.io.Serializable {
     static final long serialVersionUID;
     private static final java.io.ObjectStreamField[] serialPersistentFields;
     !static !transient <fields>;
     !private <fields>;
     !private <methods>;
     private void writeObject(java.io.ObjectOutputStream);
     private void readObject(java.io.ObjectInputStream);
     java.lang.Object writeReplace();
     java.lang.Object readResolve();
}

# 保持 BaseAdapter 类不被混淆
-keep public class * extends android.widget.BaseAdapter { *; }
# 保持 CusorAdapter 类不被混淆
-keep public class * extends android.widget.CusorAdapter{ *; }

#kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

#实体类
-keep class com.quanyou.mapt.entity.** { *; }
-keep class com.quanyou.mapt.common.Constant{ *; }
# 对于带有回调函数的onXXEvent、**On*Listener的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
    void *(**On*Callback);
    void on*Event(***);
}

# 方法名中含有“JNI”字符的，认定是Java Native Interface方法，自动排除
# 方法名中含有“JRI”字符的，认定是Java Reflection Interface方法，自动排除

-keepclasseswithmembers class * {
    ... *JNI*(...);
}

-keepclasseswithmembernames class * {
    ... *JRI*(...);
}

-keep class **JNI* {*;}

# --------------------------------------------webView区--------------------------------------------#
# WebView处理，项目中没有使用到webView忽略即可
# 保持Android与JavaScript进行交互的类不被混淆
-keep class **.AndroidJavaScript { *; }
-keepclassmembers class * extends android.webkit.WebViewClient {
     public void *(android.webkit.WebView,java.lang.String,android.graphics.Bitmap);
     public boolean *(android.webkit.WebView,java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
     public void *(android.webkit.WebView,java.lang.String);
}

# 网络请求相关
-keep public class android.net.http.SslError

# --------------------------------------------删除代码区--------------------------------------------#
# 删除代码中Log相关的代码
#-assumenosideeffects class android.util.Log {
#    public static boolean isLoggable(java.lang.String, int);
#    public static int v(...);
#    public static int i(...);
#    public static int w(...);
#    public static int d(...);
#    public static int e(...);
#}
