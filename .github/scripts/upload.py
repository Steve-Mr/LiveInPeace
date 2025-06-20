import json
import os
import requests

apiAddress = "http://127.0.0.1:8081/"
urlPrefix = apiAddress + "bot" + os.getenv("TELEGRAM_TOKEN")


def sendMediaGroup(user_id, paths):
    url = urlPrefix + "/sendMediaGroup"
    files = {}
    media = []

    for i, path in enumerate(paths):
        file_key = f'file{i}'
        files[file_key] = open(path, 'rb')
        media_item = {
            'type': 'document',
            'media': f'attach://{file_key}'
        }
        media.append(media_item)

    data = {
        'chat_id': user_id,
        'media': json.dumps(media)
    }

    response = requests.post(url, files=files, data=data)
    print("MediaGroup Response:", response.json())

    for f in files.values():
        f.close()


def sendTextMessage(user_id, message):
    url = urlPrefix + "/sendMessage"
    data = {
        'chat_id': user_id,
        'text': message,
        'parse_mode': 'Markdown'
    }
    response = requests.post(url, data=data)
    print("Text Message Response:", response.json())



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

    sendMediaGroup(user_id="@maaryIsTyping", paths=apk_paths)
    sendTextMessage(user_id="@maaryIsTyping", message=message)
