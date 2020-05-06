# AutoTranslateMod
这是一个自动翻译Localizer Mod导出的原始文本的程序，同时能够支持跳过原有中文翻译。
首先，先到http://api.fanyi.baidu.com/ 注册百度开发者账号（需要高级版）得到Appid和密钥（当然其他平台也行，但未支持，比如Dedpl..但是Dedpl Pro需要欧盟注册的信用卡）。

方法1 ------填入Main.kt的APPID和KEY中，然后将导出后的文本放入srcFiles目录中
===================================================================================
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/1.PNG" height="330" width="190" >
</div>
随后run ,会自动翻译到targetFiles目录下，目前LdstrFile.json大部分对话已经能自动翻译，若有没有翻译的提Issue，我再补充

方法2 ------ (需至少安装JRE)将根目录AutoTranslateMod-1.0-SNAPSHOT.jar文件移到任意地方(不移都行)，并在jar的当前文件夹创建srcFiles和targetFiles,任何打开命令行到当前目录执行java -jar AutoTranslateMod-1.0-SNAPSHOT.jar即可
========================================================
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/2.PNG" >
</div>
日志输出如下，{原文} ----- {机翻后文本}，之后该文件翻译完成后输出日志如下
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/3.PNG" >
</div>
效果图
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/4.png" >
</div>
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/5.png" >
</div>
