# LiveInPeace

>I felt a great disturbance in the Force... as if millions of voices suddenly cried out in terror and were suddenly silenced.

在通知栏显示当前音量。  
A simple Android app to display your volume level in the notification bar.  

这名字怎么想的：  
Why this name:  
  
安静——祝我安静——祝我安息——安息——RIP——live in peace  
Quiet - Wishing for peace - Wishing for rest - Rest - RIP - Live in peace  

## 目前的问题 Current Problems

1. 需要手动启用通知权限(必需)和附近的设备权限(非必需，仅当需要监测蓝牙耳机的连接和断开导致的音量变化)  
   The notification permission (required) and nearby device permission (optional, only needed when monitoring volume changes caused by Bluetooth headset connection/disconnection) need to be manually enabled.  
2. 通知渠道和通知优先级可能需要手动调整   
   Notification channels and priority may need to be adjusted manually.
3. 通知图标可能不够醒目  
   Notification icon may not be prominent enough.
4. 使用数字式通知图标时可能有性能问题，且音量满时会显示`!!`而非 `100`    
   There may be performance issues when using numeric notification icons, and when the volume is at maximum, it will display `!!` instead of `100`.   
5. 随便什么 bug 都有可能  
   Any kind of bug is possible

## 致谢 Credits

本项目中使用了名为 NDOT 45 的字体来动态生成通知图标。该字体从 FontStruct 网站获取的，感谢字体设计师 Interactivate 的创作。在[这里](https://fontstruct.com/fontstructions/show/1947061/ndot-45-inspired-by-nothing)可以找到此字体。  
This project is using a special font called NDOT 45 to generate dynamic notification icons. This font was obtained from the website FontStruct, and I want to express our gratitude to the font designer, Interactivate. You can find this font [here](https://fontstruct.com/fontstructions/show/1947061/ndot-45-inspired-by-nothing).  
