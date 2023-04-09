from pyrogram import Client
from pyrogram.types import InlineKeyboardButton, InlineKeyboardMarkup, InputMediaDocument
from requests import get
from tgconfig import TG_API_HASH, TG_API_ID, TG_TOKEN

req = get("https://api.github.com/repos/Lucchetto/SuperImage/releases/latest").json()
tag = req.get("tag_name")

def get_changelog():
    data = get("https://api.github.com/repos/Lucchetto/SuperImage/contents/fastlane/metadata/android/en-US/changelogs").json()
    last_log = max([int(file["name"].replace(".txt", "")) for file in data])
    log = get(
        f"https://github.com/Lucchetto/SuperImage/raw/master/fastlane/metadata/android/en-US/changelogs/{last_log}.txt"
    ).text
    return log

buttons = InlineKeyboardMarkup(
    [
        [
            InlineKeyboardButton("Download", url=req.get("html_url")),
        ]
    ]
)

caption = f"**SuperImage {tag}**\n\n<u>What's New:</u>\n```{get_changelog()}```"

with Client("SuperImage", TG_API_ID, TG_API_HASH, bot_token=TG_TOKEN, in_memory=True) as app:
    app.send_photo(-1001583643831, open("banner.png", "rb"), caption, reply_markup=buttons)
