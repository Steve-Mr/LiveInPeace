from fontTools.ttLib import TTFont
from io import BytesIO
import cairo
from cairosvg import svg2png

# 加载自定义字体
font_path = "/home/maary/Documents/code/shell/ndot_45.ttf"  # 替换为你的字体路径
ttfont = TTFont(font_path)
glyph_set = ttfont.getGlyphSet()

# 设置输出 SVG 的尺寸
width, height = 400, 400

for num in range(0, 101):
    # 创建 Cairo SVG 表面
    svg_surface = cairo.SVGSurface(f"num_{num}.svg", width, height)
    context = cairo.Context(svg_surface)

    # 设置背景为白色
    # context.set_source_rgb(1, 1, 1)  
    # context.paint()

    # 从 TTFont 提取字体数据并转换为字符串
    font_data = BytesIO()
    ttfont.save(font_data)
    font_data_str = font_data.getvalue()

    # 设置字体
    context.set_font_face(cairo.ToyFontFace("NDOT 45 (inspired by NOTHING)")) 
    # context.select_font_face("NDOT 45 (inspired by NOTHING)", cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD) 
    context.set_font_size(250)
    context.set_source_rgb(0, 0, 0)

    if (num == 100):
    	num = "!!"
    # 获取文本范围
    text = str(num)
    x_bearing, y_bearing, text_width, text_height, x_advance, y_advance = context.text_extents(text)

    # 计算文本位置，使其居中
    x = (width - text_width) / 2 - x_bearing
    y = (height - text_height) / 2 - y_bearing

    # 绘制文本
    context.move_to(x, y)
    context.show_text(text)

    # 完成 SVG 并关闭
    svg_surface.finish()

    # 可选：将 SVG 转换为 PNG
    # svg2png(url=f"output_{num}.svg", write_to=f"output_{num}.png")

