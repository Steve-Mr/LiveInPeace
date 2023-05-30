# LiveInPeace

>I felt a great disturbance in the Force... as if millions of voices suddenly cried out in terror and were suddenly silenced.

[![Android CI](https://github.com/Steve-Mr/LiveInPeace/actions/workflows/android.yml/badge.svg)](https://github.com/Steve-Mr/LiveInPeace/actions/workflows/android.yml)

[中文版](README_ZH.md)

A simple Android app to display your volume level in the notification bar.  
 
Why this name:  

Quiet - Wishing for peace - Wishing for rest - Rest - RIP - Live in peace  

## Current Problems

1. ~~The notification permission (required) and nearby device permission (optional, only needed when monitoring volume changes caused by Bluetooth headset connection/disconnection) need to be manually enabled.~~  
2. Notification channels and priority may need to be adjusted manually.
3. Notification icon may not be prominent enough.
4. There may be performance issues when using numeric notification icons, and when the volume is at maximum, it will display `!!` instead of `100`.   
5. "Disable Alert" and "Enable Alert" in the settings refer to the option of disabling or enabling the notification for prolonged headphone connection time. Due to the width limitation of the notification button, it may not clearly convey the meaning. There is a plan to implement a settings interface using an Activity in the future, but it's not guaranteed and may take a considerable amount of time.
6. Any kind of bug is possible

## Credits

This project is using a special font called NDOT 45 to generate dynamic notification icons. This font was obtained from the website FontStruct, and I want to express our gratitude to the font designer, Interactivate. You can find this font [here](https://fontstruct.com/fontstructions/show/1947061/ndot-45-inspired-by-nothing).  
