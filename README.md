# AutoTranslateMod
这是一个自动翻译Localizer Mod导出的原始文本的程序
首先，先到http://api.fanyi.baidu.com/ 注册百度开发者账号得到Appid和密钥（当然其他平台也行，但未支持，比如Depl..但是Depl Pro需要欧盟注册的信用卡），填入Main.kt的APPID和KEY中，然后将导出后的文本放入srcFiles目录中
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/1.PNG" height="330" width="190" >
</div>
随后run ,会自动翻译到targetFiles目录下，目前LdstrFile.json未翻译，但是不是很影响体验，最近在忙毕设，等有空吧。
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/2.PNG" >
</div>
日志输出如下，文件名------表示当前文件，result16正常翻译，之后该文件翻译完成后输出日志如下
<div align="center">
<img src="https://github.com/cllh1999/AutoTranslateMod/blob/master/images/3.PNG" >
</div>
