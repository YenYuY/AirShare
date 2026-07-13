from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parent
RAW = ROOT / "Raw" / "airshare-home-iphone17pro.png"
HERO_BG = ROOT / "Marketing" / "airshare-hero-background.png"
OUT = ROOT / "Marketing"
OUT.mkdir(parents=True, exist_ok=True)

BLUE = (46, 123, 246)
INK = (18, 26, 42)
MUTED = (86, 99, 119)
WHITE = (255, 255, 255)


def font(size: int, bold: bool = False):
    names = [
        "/System/Library/Fonts/STHeiti Medium.ttc",
        "/System/Library/Fonts/Hiragino Sans GB.ttc",
        "/System/Library/Fonts/SFNS.ttf",
    ]
    for name in names:
        try:
            return ImageFont.truetype(name, size=size, index=0)
        except OSError:
            pass
    return ImageFont.load_default()


def cover(image: Image.Image, size):
    ratio = max(size[0] / image.width, size[1] / image.height)
    resized = image.resize((round(image.width * ratio), round(image.height * ratio)), Image.Resampling.LANCZOS)
    left = (resized.width - size[0]) // 2
    top = (resized.height - size[1]) // 2
    return resized.crop((left, top, left + size[0], top + size[1]))


def rounded_paste(canvas, image, box, radius=64, shadow=True):
    image = cover(image, (box[2] - box[0], box[3] - box[1]))
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, image.width, image.height), radius=radius, fill=255)
    if shadow:
        shadow_layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
        shadow_mask = Image.new("L", canvas.size, 0)
        ImageDraw.Draw(shadow_mask).rounded_rectangle(box, radius=radius, fill=90)
        shadow_mask = shadow_mask.filter(ImageFilter.GaussianBlur(28))
        shadow_layer.paste((26, 63, 118, 90), (0, 24), shadow_mask)
        canvas.alpha_composite(shadow_layer)
    canvas.paste(image.convert("RGBA"), (box[0], box[1]), mask)


def background(size=(1260, 2736), tint=(238, 246, 255)):
    canvas = Image.new("RGBA", size, tint + (255,))
    glow = Image.new("RGBA", size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((size[0] * .45, -180, size[0] * 1.25, 900), fill=(46, 123, 246, 58))
    gd.ellipse((-450, size[1] * .55, 650, size[1] * 1.05), fill=(61, 206, 239, 38))
    return Image.alpha_composite(canvas, glow.filter(ImageFilter.GaussianBlur(95)))


def centered(draw, text, y, fnt, fill=INK, width=1120):
    box = draw.textbbox((0, 0), text, font=fnt)
    x = (1260 - (box[2] - box[0])) // 2
    draw.text((x, y), text, font=fnt, fill=fill)


def appstore_one(screen):
    canvas = background()
    d = ImageDraw.Draw(canvas)
    centered(d, "照片，不必先上雲端", 150, font(72, True))
    centered(d, "同一個 Wi-Fi，打開瀏覽器就能下載", 250, font(34), MUTED)
    rounded_paste(canvas, screen, (214, 410, 1046, 2218), 92)
    d.rounded_rectangle((140, 2360, 1120, 2574), radius=52, fill=(255, 255, 255, 205))
    d.text((210, 2412), "AirShare", font=font(48, True), fill=INK)
    d.text((210, 2480), "快速、直覺的區網相片傳輸", font=font(31), fill=MUTED)
    d.ellipse((968, 2426, 1038, 2496), fill=BLUE)
    canvas.convert("RGB").save(OUT / "appstore-01-local-transfer-1260x2736.jpg", quality=95)


def appstore_two(screen):
    canvas = background(tint=(246, 249, 255))
    d = ImageDraw.Draw(canvas)
    d.text((88, 140), "三步完成傳輸", font=font(76, True), fill=INK)
    d.text((92, 250), "不用帳號、不用連接線", font=font(36), fill=MUTED)
    rounded_paste(canvas, screen, (590, 410, 1134, 1592), 62)
    steps = [
        ("1", "允許相簿存取", "讀取你授權的照片與影片"),
        ("2", "啟動 AirShare", "手機與電腦保持同一個 Wi-Fi"),
        ("3", "在電腦打開網址", "瀏覽並下載原始照片與影片"),
    ]
    y = 550
    for number, title, body in steps:
        d.ellipse((88, y, 174, y + 86), fill=BLUE)
        d.text((116, y + 12), number, font=font(40, True), fill=WHITE)
        d.text((206, y - 2), title, font=font(39, True), fill=INK)
        d.multiline_text((206, y + 57), body, font=font(27), fill=MUTED, spacing=8)
        y += 300
    d.rounded_rectangle((86, 1915, 1174, 2490), radius=52, fill=(255, 255, 255, 225))
    d.text((150, 1990), "從手機，到你的電腦", font=font(49, True), fill=INK)
    d.text((150, 2075), "照片留在你的區域網路中傳送。", font=font(31), fill=MUTED)
    for x, icon in [(205, "iPhone"), (630, "Wi-Fi"), (950, "電腦")]:
        d.ellipse((x - 64, 2220, x + 64, 2348), fill=(230, 240, 255))
        w = d.textbbox((0, 0), icon, font=font(28, True))[2]
        d.text((x - w // 2, 2385), icon, font=font(28, True), fill=INK)
    d.line((270, 2284, 565, 2284), fill=BLUE, width=8)
    d.line((695, 2284, 885, 2284), fill=BLUE, width=8)
    canvas.convert("RGB").save(OUT / "appstore-02-three-steps-1260x2736.jpg", quality=95)


def appstore_three(screen):
    canvas = Image.new("RGBA", (1260, 2736), (20, 39, 73, 255))
    d = ImageDraw.Draw(canvas)
    d.ellipse((700, -250, 1500, 550), fill=(46, 123, 246, 80))
    d.text((88, 150), "你的媒體，\n留在你的網路", font=font(76, True), fill=WHITE, spacing=16)
    d.text((92, 380), "沒有雲端上傳步驟", font=font(36), fill=(188, 205, 232))
    rounded_paste(canvas, screen, (524, 575, 1110, 1850), 66)
    bullets = ["透過區域 Wi-Fi 傳輸", "原始檔案直接下載", "支援照片與影片", "繁中、英文、日文介面"]
    y = 700
    for item in bullets:
        d.ellipse((96, y + 6, 134, y + 44), fill=(76, 226, 159))
        d.line((106, y + 25, 117, y + 35), fill=(20, 39, 73), width=5)
        d.line((117, y + 35, 128, y + 16), fill=(20, 39, 73), width=5)
        d.text((160, y), item, font=font(32, True), fill=WHITE)
        y += 170
    d.rounded_rectangle((86, 2070, 1174, 2492), radius=54, fill=(255, 255, 255, 24), outline=(255, 255, 255, 55), width=2)
    d.text((150, 2140), "沒有繁複設定", font=font(50, True), fill=WHITE)
    d.multiline_text((150, 2235), "開啟 App、啟動分享，\n在電腦瀏覽器輸入顯示的網址。", font=font(34), fill=(198, 214, 238), spacing=18)
    canvas.convert("RGB").save(OUT / "appstore-03-private-lan-1260x2736.jpg", quality=95)


def website_hero(screen):
    base = cover(Image.open(HERO_BG).convert("RGB"), (1920, 1080)).convert("RGBA")
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.rectangle((0, 0, 930, 1080), fill=(248, 252, 255, 238))
    overlay = overlay.filter(ImageFilter.GaussianBlur(10))
    canvas = Image.alpha_composite(base, overlay)
    d = ImageDraw.Draw(canvas)
    d.text((120, 190), "AirShare", font=font(76, True), fill=INK)
    d.multiline_text((120, 310), "照片從 iPhone\n直接到你的電腦", font=font(70, True), fill=INK, spacing=12)
    d.multiline_text((124, 535), "同一個 Wi-Fi，不用帳號、不經雲端。\n在瀏覽器中查看並下載原始照片與影片。", font=font(32), fill=MUTED, spacing=16)
    d.rounded_rectangle((120, 710, 512, 796), radius=43, fill=BLUE)
    d.text((180, 729), "開始快速分享", font=font(31, True), fill=WHITE)
    rounded_paste(canvas, screen, (1320, 105, 1724, 983), 50)
    canvas.convert("RGB").save(OUT / "website-hero-1920x1080.jpg", quality=95)


if __name__ == "__main__":
    screenshot = Image.open(RAW).convert("RGB")
    appstore_one(screenshot)
    appstore_two(screenshot)
    appstore_three(screenshot)
    website_hero(screenshot)
