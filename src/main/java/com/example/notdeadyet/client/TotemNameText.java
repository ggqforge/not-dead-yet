package com.example.notdeadyet.client;

import com.example.notdeadyet.TotemNameDisplay;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 名字解析：<b>显示文本</b> + <b>字号倍率</b> + <b>颜色 / 渐变 / 方向</b>。
 *
 * <h2>命名规则：末尾一个方括号设置块</h2>
 *
 * <pre>
 * 应急[颜色:红色;尺寸:2]
 * 应急[c:红,蓝]              c 是「颜色」的短键，逗号分隔即为渐变
 * 应急[c:红,蓝;s:1.5]        s 是「尺寸」的短键
 * 应急[c:y红,蓝]             方向字母：x 横向（默认）、y 纵向、t 斜向
 * 应急[c:t红,黄,绿]          斜向多段渐变
 * 凤凰[c:#FF8800]
 * 备用[颜色:金][尺寸:1.5]     也可以拆成多个方括号连写
 * </pre>
 *
 * <h2>只认方括号这一种写法</h2>
 *
 * <p>本模组<b>不</b>解析 {@code §} 代码，也不沿用 {@code custom_name} 组件自带的样式。
 * 原因是原版铁砧根本用不了 {@code §}：{@code AnvilScreenHandler.setNewItemName} 的第一条
 * 指令就是 {@code sanitize(...)}，而它调用 {@code StringHelper.stripInvalidChars}；
 * 后者的判据 {@code isValidChar} 对 {@code 167}（{@code §}）返回 false，对
 * {@code [ ] : ; ,} 和中文全部放行（均已用字节码确认）。</p>
 *
 * <p>所以方括号设置块是唯一「能在铁砧里直接打字」的写法，也就没有理由再维护第二条通路。
 * 命令想设颜色也一样：把设置块写进名字字符串即可，例如
 * {@code /give @s totem_of_undying[minecraft:custom_name='应急[颜色:红色]']}。</p>
 *
 * <h2>方向字母的歧义</h2>
 *
 * <p>{@code y} 既是方向字母，也是颜色名 {@code yellow} 的首字母（{@code t} 与
 * {@code teal} 同理）。所以解析时<b>先试「方向字母 + 颜色」，失败再退回把整串当颜色</b>：
 * {@code yellow} 会被切成方向 {@code y} + {@code ellow}，而 {@code ellow} 不是合法颜色，
 * 于是回退，{@code yellow} 正常解析。两不误。</p>
 */
public final class TotemNameText {

    /** 名字里没写尺寸时使用的倍率。 */
    public static final float DEFAULT_SIZE_MULTIPLIER = 1.0F;

    /**
     * 没指定颜色时使用的预设字色 —— 不死图腾贴图上的金属亮面金 {@code #EADB84}。
     *
     * <p>动画与物品栏 tooltip 共用它，保证两处颜色一致。</p>
     */
    public static final int DEFAULT_COLOR = 0xEADB84;

    private static final float MIN_SIZE_MULTIPLIER = 0.25F;
    private static final float MAX_SIZE_MULTIPLIER = 6.0F;

    /** 结尾的设置块，例如 {@code [颜色:红色;尺寸:2]}。 */
    private static final Pattern TRAILING_BLOCK = Pattern.compile("\\[([^\\[\\]]*)]\\s*$");
    /** 键与值的分隔：英文/中文冒号。 */
    private static final Pattern KEY_VALUE = Pattern.compile("[:：]");
    /** 键值对之间的分隔：英文/中文分号。 */
    private static final Pattern PAIR_SEPARATOR = Pattern.compile("[;；]");
    /** 渐变色标之间的分隔：英文/中文逗号。 */
    private static final Pattern STOP_SEPARATOR = Pattern.compile("[,，]");

    private static final Set<String> COLOR_KEYS =
            Set.of("颜色", "色", "c", "color", "colour", "渐变", "gradient");
    private static final Set<String> SIZE_KEYS = Set.of("尺寸", "大小", "size", "s");

    /** 彩蛋关键词：动态彩虹。致敬原版 {@code _jeb} 绵羊的循环变色羊毛。 */
    private static final String EASTER_EGG_RAINBOW = "_jeb";

    private static final Map<String, Integer> COLOR_NAMES = buildColorNames();

    private static final TotemNameText EMPTY = new TotemNameText(
            OrderedText.EMPTY, "", DEFAULT_SIZE_MULTIPLIER, null, null,
            TotemGradient.Direction.HORIZONTAL, true);

    // 解析结果缓存：名字在整段动画里是固定的，没必要每帧重跑正则。
    private static String cachedRaw;
    private static TotemNameText cachedResult;
    /** 只用来抑制重复日志：同一个名字只打一次。 */
    private static String lastLoggedRaw;

    private final OrderedText text;
    /** 剥掉设置块后的纯文本。tooltip 那种只能吃字符串的地方用它。 */
    private final String plainText;
    private final float sizeMultiplier;
    /** 纯色；null 表示没指定。与 {@link #gradient} 互斥。 */
    private final Integer color;
    /** 渐变或彩虹；null 表示没指定。 */
    private final TotemGradient gradient;
    /** 渐变方向。 */
    private final TotemGradient.Direction direction;
    private final boolean empty;

    private TotemNameText(OrderedText text, String plainText, float sizeMultiplier, Integer color,
                          TotemGradient gradient, TotemGradient.Direction direction, boolean empty) {
        this.text = text;
        this.plainText = plainText;
        this.sizeMultiplier = sizeMultiplier;
        this.color = color;
        this.gradient = gradient;
        this.direction = direction;
        this.empty = empty;
    }

    /** 已剥离设置块的显示文本。 */
    public OrderedText text() {
        return text;
    }

    /** 已剥离设置块的纯文本。 */
    public String plainText() {
        return plainText;
    }

    /** 实际该用的字色：指定了就用指定的，否则用预设金。 */
    public int effectiveColor() {
        return color != null ? color : DEFAULT_COLOR;
    }

    /** 命名时指定的字号倍率。 */
    public float sizeMultiplier() {
        return sizeMultiplier;
    }

    /** 命名时指定的纯色；{@code null} 表示没指定。 */
    public Integer color() {
        return color;
    }

    /** 命名时指定的渐变或彩虹；{@code null} 表示没指定。 */
    public TotemGradient gradient() {
        return gradient;
    }

    /** 渐变方向；没有渐变时无意义，默认横向。 */
    public TotemGradient.Direction direction() {
        return direction;
    }

    /** 剥离设置块之后没有可显示的内容。 */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * 解析名字字符串。结果按原始字符串缓存，动画期间每帧调用也只解析一次。
     *
     * @param raw {@code custom_name} 的纯文本内容；null 或空串返回 {@link #EMPTY}
     */
    public static TotemNameText parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return EMPTY;
        }
        if (raw.equals(cachedRaw)) {
            return cachedResult;
        }

        TotemNameText result = parseUncached(raw);
        cachedRaw = raw;
        cachedResult = result;
        return result;
    }

    private static TotemNameText parseUncached(String raw) {
        float multiplier = DEFAULT_SIZE_MULTIPLIER;
        Integer color = null;
        TotemGradient gradient = null;
        TotemGradient.Direction direction = TotemGradient.Direction.HORIZONTAL;
        List<String> problems = new ArrayList<>();

        // 反复剥离结尾的设置块，兼容 [颜色:红色;尺寸:2] 与 [颜色:红色][尺寸:2]
        String visible = raw;
        while (!visible.isEmpty()) {
            Matcher matcher = TRAILING_BLOCK.matcher(visible);
            if (!matcher.find()) {
                break;
            }
            Settings settings = parseSettings(matcher.group(1), problems);
            if (!settings.applied) {
                break;   // 结尾这个方括号不是设置块，原样保留
            }
            multiplier = settings.size != null ? settings.size : multiplier;
            if (settings.color != null) {
                color = settings.color;
                gradient = null;
            }
            if (settings.gradient != null) {
                gradient = settings.gradient;
                color = null;
            }
            if (settings.direction != null) {
                direction = settings.direction;
            }
            visible = visible.substring(0, matcher.start()).trim();
        }

        logOnce(raw, visible, multiplier, color, gradient, direction, problems);

        if (visible.isEmpty()) {
            return EMPTY;
        }
        return new TotemNameText(Text.literal(visible).asOrderedText(), visible, multiplier,
                color, gradient, direction, false);
    }

    // ---- 设置块解析 ----

    /** 一个设置块的解析结果。 */
    private static final class Settings {
        Float size;
        Integer color;
        TotemGradient gradient;
        TotemGradient.Direction direction;
        /** 是否至少有一个键值对解析成功 —— 决定这个方括号要不要被吃掉。 */
        boolean applied;
    }

    private static Settings parseSettings(String body, List<String> problems) {
        Settings result = new Settings();

        for (String pair : PAIR_SEPARATOR.split(body)) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] keyValue = KEY_VALUE.split(trimmed, 2);
            if (keyValue.length != 2) {
                // 省略键名的简写：[yred,blue;2]
                applyKeyless(result, trimmed, problems);
                continue;
            }

            String key = keyValue[0].trim().toLowerCase(Locale.ROOT);
            String value = keyValue[1].trim();

            if (COLOR_KEYS.contains(key)) {
                applyColor(result, value, problems, trimmed);
            } else if (SIZE_KEYS.contains(key)) {
                Float parsed = parseSize(value);
                if (parsed != null) {
                    result.size = parsed;
                    result.applied = true;
                } else {
                    problems.add(trimmed + "（字号要写数字，例如 2 或 1.5）");
                }
            } else {
                problems.add(trimmed + "（认不出这个键）");
            }
        }
        return result;
    }

    /**
     * 解析带键的颜色值。先试「方向字母 + 颜色」，失败再退回把整串当颜色
     * （见类注释里的歧义说明）。
     */
    private static void applyColor(Settings result, String value, List<String> problems, String pairText) {
        if (value.trim().isEmpty()) {
            problems.add(pairText + "（颜色是空的）");
            return;
        }
        if (tryColor(result, value)) {
            result.applied = true;
            return;
        }
        problems.add(pairText + "（认不出这个颜色）");
    }

    /**
     * 省略键名的简写：{@code [yred,blue;2]} 等价于 {@code [c:yred,blue;s:2]}。
     *
     * <p>规则是<b>先试字号、再试颜色</b>：能当数字读的（含 {@code 2x} 这种写法）算字号，
     * 其余一律当颜色值。</p>
     *
     * <p><b>为什么字号优先</b>：原版单字符颜色代码里 {@code 0}~{@code 9} 都有效，
     * 所以 {@code [2]} 天然有歧义（字号 2 还是颜色 {@code §2} 深绿）。定成「数字优先」
     * 之后规则是确定的、可预期的；要写数字颜色代码就显式带上键，例如 {@code [c:2]}。</p>
     */
    private static void applyKeyless(Settings result, String text, List<String> problems) {
        Float size = parseSize(text);
        if (size != null) {
            result.size = size;
            result.applied = true;
            return;
        }
        if (tryColor(result, text)) {
            result.applied = true;
            return;
        }
        problems.add(text + "（既不是字号也不是颜色。省略键名时数字当字号、其余当颜色）");
    }

    /**
     * 把整串当颜色值试一次（允许带方向字母）。成功返回 {@code true} 并写入 {@code result}；
     * 失败时不留任何副作用、也不记日志。
     */
    private static boolean tryColor(Settings result, String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        TotemGradient.Direction letter = TotemGradient.Direction.fromLetter(trimmed.charAt(0));
        if (letter != null && tryApplyColors(result, trimmed.substring(1).trim(), letter)) {
            return true;
        }
        return tryApplyColors(result, trimmed, TotemGradient.Direction.HORIZONTAL);
    }

    /**
     * 尝试把 {@code value} 解析成颜色 / 渐变 / 彩虹。
     *
     * <p>失败时<b>不产生任何副作用</b>、也不记日志 —— 因为 {@link #applyColor} 会先拿它
     * 当探测用。真正失败时的报错由调用方统一给出。</p>
     */
    private static boolean tryApplyColors(Settings result, String value, TotemGradient.Direction direction) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        if (trimmed.equalsIgnoreCase(EASTER_EGG_RAINBOW)) {
            result.gradient = TotemGradient.rainbow();
            result.color = null;
            result.direction = direction;
            return true;
        }

        String[] parts = STOP_SEPARATOR.split(trimmed);
        if (parts.length > 1) {
            List<Integer> stops = new ArrayList<>(parts.length);
            for (String part : parts) {
                Integer stop = parseColor(part);
                if (stop == null) {
                    return false;   // 任意一段坏掉就整体失败，不静默丢弃
                }
                stops.add(stop);
            }
            result.gradient = TotemGradient.ofStops(stops);
            result.color = null;
            result.direction = direction;
            return true;
        }

        Integer solid = parseColor(trimmed);
        if (solid == null) {
            return false;
        }
        result.color = solid;
        result.gradient = null;
        result.direction = direction;
        return true;
    }

    /** 颜色：{@code #RRGGBB} / 单个代码字符 / 中英文颜色名。失败返回 null。 */
    private static Integer parseColor(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) {
            return null;
        }

        // 十六进制。#RGB 会展开成 #RRGGBB
        if (v.charAt(0) == '#') {
            String hex = v.substring(1);
            if (hex.length() == 3) {
                StringBuilder expanded = new StringBuilder(6);
                for (int i = 0; i < 3; i++) {
                    expanded.append(hex.charAt(i)).append(hex.charAt(i));
                }
                hex = expanded.toString();
            }
            if (hex.length() == 6 && hex.matches("[0-9a-f]{6}")) {
                return Integer.parseInt(hex, 16);
            }
            return null;
        }

        // 单个代码字符，例如 [颜色:c]
        if (v.length() == 1) {
            Formatting formatting = Formatting.byCode(v.charAt(0));
            if (formatting != null && formatting.isColor()) {
                return formatting.getColorValue();
            }
        }

        return COLOR_NAMES.get(v);
    }

    /** 字号：数字，允许尾巴带个 x。夹在允许范围内。 */
    private static Float parseSize(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.endsWith("x")) {
            v = v.substring(0, v.length() - 1).trim();
        }
        try {
            return MathHelper.clamp(Float.parseFloat(v),
                    MIN_SIZE_MULTIPLIER, MAX_SIZE_MULTIPLIER);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 颜色名表。中文名按直觉给，注意几个「原版没有对应色」的近似映射：
     * 橙/橙色 → GOLD，粉/粉色 → LIGHT_PURPLE，天蓝 → AQUA。
     */
    private static Map<String, Integer> buildColorNames() {
        Map<String, Integer> map = new HashMap<>();
        register(map, Formatting.BLACK, "黑", "黑色", "black");
        register(map, Formatting.DARK_BLUE, "深蓝", "暗蓝", "dark_blue", "darkblue", "navy");
        register(map, Formatting.DARK_GREEN, "深绿", "暗绿", "dark_green", "darkgreen");
        register(map, Formatting.DARK_AQUA, "深青", "暗青", "dark_aqua", "darkaqua", "teal");
        register(map, Formatting.DARK_RED, "深红", "暗红", "dark_red", "darkred", "maroon");
        register(map, Formatting.DARK_PURPLE, "深紫", "暗紫", "dark_purple", "darkpurple");
        register(map, Formatting.GOLD, "金", "金色", "橙", "橙色", "gold", "orange");
        register(map, Formatting.GRAY, "灰", "灰色", "gray", "grey", "silver");
        register(map, Formatting.DARK_GRAY, "深灰", "暗灰", "dark_gray", "dark_grey",
                "darkgray", "darkgrey");
        register(map, Formatting.BLUE, "蓝", "蓝色", "blue");
        register(map, Formatting.GREEN, "绿", "绿色", "green", "lime");
        register(map, Formatting.AQUA, "青", "青色", "天蓝", "浅蓝", "aqua", "cyan");
        register(map, Formatting.RED, "红", "红色", "red");
        register(map, Formatting.LIGHT_PURPLE, "淡紫", "亮紫", "浅紫", "粉", "粉色",
                "light_purple", "lightpurple", "pink", "magenta");
        register(map, Formatting.YELLOW, "黄", "黄色", "yellow");
        register(map, Formatting.WHITE, "白", "白色", "white");
        return Map.copyOf(map);
    }

    private static void register(Map<String, Integer> map, Formatting formatting, String... names) {
        Integer rgb = formatting.getColorValue();
        if (rgb == null) {
            return;
        }
        for (String name : names) {
            map.put(name, rgb);
        }
    }

    /** 同一个名字只打一次日志，避免每帧刷屏。 */
    private static void logOnce(String raw, String visible, float multiplier, Integer color,
                                TotemGradient gradient, TotemGradient.Direction direction,
                                List<String> problems) {
        if (raw.equals(lastLoggedRaw)) {
            return;
        }
        lastLoggedRaw = raw;

        String colorText;
        if (gradient != null) {
            colorText = "渐变(" + direction + ")";
        } else if (color != null) {
            colorText = String.format("#%06X", color);
        } else {
            colorText = "预设";
        }

        TotemNameDisplay.LOGGER.info("[命名] 「{}」→ 显示「{}」字号 x{} 颜色 {}",
                raw, visible, String.format("%.2f", multiplier), colorText);
        for (String problem : problems) {
            TotemNameDisplay.LOGGER.warn("[命名] 已忽略：{}", problem);
        }
    }
}
