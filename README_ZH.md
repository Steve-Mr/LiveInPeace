# LiveInPeace

>I felt a great disturbance in the Force... as if millions of voices suddenly cried out in terror and were suddenly silenced.

[![Android CI](https://github.com/Steve-Mr/LiveInPeace/actions/workflows/android.yml/badge.svg)](https://github.com/Steve-Mr/LiveInPeace/actions/workflows/android.yml)


在通知栏显示当前音量。  

这名字怎么想的：  

安静——祝我安静——祝我安息——安息——RIP——live in peace  

## 目前的问题 

1. ~~需要手动启用通知权限(必需)和附近的设备权限(非必需，仅当需要监测蓝牙耳机的连接和断开导致的音量变化)~~
2. 通知渠道和通知优先级可能需要手动调整   
3. 通知图标可能不够醒目  
4. 使用数字式通知图标时~~可能有性能问题，且~~音量满时会显示`!!`而非 `100`    
5. 设置中的「禁用提醒」和「启用提醒」指是否启用耳机连接时间过长的提醒，受限于通知按钮的宽度限制没有清楚表述，计划将来使用 Activity 实现设置界面，但并不能保证且可能需要很长的时间。
6. 随便什么 bug 都有可能  

## 致谢 Credits

本项目中使用了名为 NDOT 45 的字体来动态生成通知图标。该字体从 FontStruct 网站获取的，感谢字体设计师 Interactivate 的创作。在[这里](https://fontstruct.com/fontstructions/show/1947061/ndot-45-inspired-by-nothing)可以找到此字体。  
