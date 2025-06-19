import json
import os
import requests

apiAddress = "http://127.0.0.1:8081/"
urlPrefix = apiAddress + "bot" + os.getenv("TELEGRAM_TOKEN")


def sendMediaGroup(user_id, paths, message=""):
    """
    使用 Telegram Bot API 的 sendMediaGroup 方法发送一组文件。
    """
    url = urlPrefix + "/sendMediaGroup"
    files = {}
    media = []

    # 将消息文本（caption）附加到第一个文件上
    # API规定，媒体组的标题由第一个媒体项的 caption 决定
    for i, path in enumerate(paths):
        file_key = f'file{i}'
        files[file_key] = open(path, 'rb')
        media_item = {
            'type': 'document',
            'media': f'attach://{file_key}'
        }
        # 只在第一个文件上添加 caption
        if i == 0:
            media_item['caption'] = message
            media_item['parse_mode'] = 'Markdown'
        media.append(media_item)

    # 准备请求数据
    data = {
        'chat_id': user_id,
        'media': json.dumps(media)
    }

    # 发送请求
    response = requests.post(url, files=files, data=data)
    print(response.json())

    # 关闭所有已打开的文件
    for f in files.values():
        f.close()


if __name__ == '__main__':
    # 从环境变量中获取两个 APK 文件的路径
    apk_path1 = os.getenv("APK_FILE_UPLOAD1")
    apk_path2 = os.getenv("APK_FILE_UPLOAD2")

    # 检查路径是否存在
    if not apk_path1 or not apk_path2:
        print("错误：未能在环境变量中找到 APK 文件路径。")
        exit(1)

    # 将两个 APK 路径放入一个列表
    apk_paths = [apk_path1, apk_path2]

    # 从环境变量中获取版本信息和提交信息来构建消息内容
    version_name = os.getenv("VERSION_NAME", "N/A")
    commit_message = os.getenv("COMMIT_MESSAGE", "无提交信息。")

    message = (
        f"#app #apk\n"
        f"**版本:** `{version_name}`\n\n"
        f"**更新内容:**\n{commit_message}\n\n"
        f"https://github.com/Steve-Mr/LiveInPeace"
    )

    # 调用函数，将两个 APK 文件发送到指定用户
    sendMediaGroup(
        user_id="@maaryIsTyping",
        paths=apk_paths,
        message=message
    )