"""Draw the figures 2 to 7 used in ai-grading-test-report.md (figure 1 is a Mermaid diagram in the report).

Run: python make_charts.py   (needs matplotlib; PNG files are written next to this script)

Every number below is copied from the tables of ai-grading-test-report.md. If a table changes, update the constants
here and run the script again; the tables in the report remain the text alternative for each chart.
"""
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
from matplotlib.ticker import FuncFormatter  # noqa: E402

OUT = Path(__file__).resolve().parent
BLUE, TEAL, ORANGE, RED, GREY, INK = "#2F6DB5", "#2A9D8F", "#E9A23B", "#D1495B", "#9AA5B1", "#25303B"

plt.rcParams.update({
    "font.size": 10, "axes.edgecolor": GREY, "axes.labelcolor": INK, "text.color": INK,
    "xtick.color": INK, "ytick.color": INK, "axes.spines.top": False, "axes.spines.right": False,
    "axes.grid": True, "grid.color": "#E6EAEE", "grid.linewidth": 0.8, "axes.axisbelow": True,
    "figure.dpi": 100, "savefig.dpi": 150, "savefig.bbox": "tight", "savefig.facecolor": "white",
})


def comma(value, digits=None):
    """Vietnamese decimal comma, as in the report tables."""
    text = f"{value:.{digits}f}" if digits is not None else f"{value:g}"
    return text.replace(".", ",")


def comma_axis(axis, thousands=False):
    fmt = (lambda v, _: f"{int(v):,}".replace(",", ".")) if thousands else (lambda v, _: comma(v))
    axis.yaxis.set_major_formatter(FuncFormatter(fmt))


def save(figure, name):
    figure.savefig(OUT / name)
    plt.close(figure)
    print("wrote", name)


# 1. Three rounds: score match and judge passes -------------------------------------------------------------------
def chart_rounds():
    rounds = ["Lượt 1\nBaseline", "Lượt 2\nĐọc rubric và sơ đồ", "Lượt 3\nTrigger và điểm từng luồng"]
    luna = [15, 16, 15]
    sol = [15, 18, 18]
    opus = [13, 2, 15]  # round 2: only 3 cases were judged, 2 passed
    figure, axis = plt.subplots(figsize=(9, 4.6))
    width = 0.26
    positions = range(len(rounds))
    series = [("Điểm tổng khớp mốc nhóm (Luna)", luna, BLUE), ("Sol đạt cả 3 tiêu chí", sol, TEAL),
              ("Opus đạt cả 3 tiêu chí", opus, ORANGE)]
    for index, (label, values, color) in enumerate(series):
        bars = axis.bar([p + (index - 1) * width for p in positions], values, width, label=label, color=color)
        for bar, value in zip(bars, values):
            hatched = label.startswith("Opus") and value == 2
            if hatched:
                bar.set_hatch("///")
                bar.set_facecolor("white")
                bar.set_edgecolor(ORANGE)
            axis.text(bar.get_x() + bar.get_width() / 2, value + 0.35, "2/3*" if hatched else f"{value}/20",
                      ha="center", va="bottom", fontsize=9)
    axis.set_xticks(list(positions), rounds)
    axis.set_ylim(0, 22)
    axis.set_yticks(range(0, 21, 5))
    axis.set_ylabel("Số ca (trên 20 ca mỗi lượt)")
    axis.grid(axis="x", visible=False)
    axis.set_title("Kết quả ba lượt chấm", loc="left", fontweight="bold")
    axis.legend(loc="upper center", bbox_to_anchor=(0.5, -0.2), ncol=3, frameon=False)
    figure.text(0.01, -0.04, "* Lượt 2 chỉ có 3 ca được Opus đánh giá (2 ca đạt), nên chưa so sánh được với hai lượt còn lại.",
                fontsize=8.5, color=GREY)
    save(figure, "hinh-4-ket-qua-ba-luot.png")


# 2. Score deviation per round ------------------------------------------------------------------------------------
def chart_deviation():
    rounds = ["Lượt 1", "Lượt 2", "Lượt 3"]
    mae = [0.148, 0.065, 0.165]
    biggest = [2.00, 0.50, 1.50]
    figure, (left, right) = plt.subplots(1, 2, figsize=(9, 3.9))
    for axis, values, title, color, label in (
        (left, mae, "Sai số trung bình (điểm)", BLUE, "{:.3f}"),
        (right, biggest, "Sai lớn nhất của một ca (điểm)", RED, "{:.2f}"),
    ):
        bars = axis.bar(rounds, values, color=color, width=0.55)
        for bar, value in zip(bars, values):
            axis.text(bar.get_x() + bar.get_width() / 2, value + max(values) * 0.02, label.format(value).replace(".", ","),
                      ha="center", va="bottom", fontsize=10)
        axis.set_ylim(0, max(values) * 1.18)
        axis.set_title(title, loc="left", fontweight="bold", fontsize=10.5)
        axis.grid(axis="x", visible=False)
        comma_axis(axis)
    figure.suptitle("Độ lệch điểm của Luna so với mốc nhóm", x=0.01, ha="left", fontweight="bold", y=1.03)
    figure.text(0.01, -0.04, "Mỗi ca chỉ chấm một lần mỗi lượt. Chạy lặp cho thấy cùng một prompt có thể dao động tới 1,5 điểm ở một ca.",
                fontsize=8.5, color=GREY)
    save(figure, "hinh-5-do-lech-diem.png")


# 3. Result per assignment (round 3) ------------------------------------------------------------------------------
def chart_by_assignment():
    rows = [("swr302-room-scheduling", 2, 2), ("swr302-lab-equipment-relations", 1, 1), ("swr302-lab-equipment", 3, 3),
            ("swd392-internship-portal", 3, 3), ("swd392-workshop-registration", 3, 1), ("badminton-booking", 8, 5)]
    figure, axis = plt.subplots(figsize=(9, 3.9))
    names = [row[0] for row in rows]
    matched = [row[2] for row in rows]
    off = [row[1] - row[2] for row in rows]
    axis.barh(names, matched, color=BLUE, label="Điểm tổng khớp mốc nhóm", height=0.6)
    axis.barh(names, off, left=matched, color=RED, label="Lệch mốc nhóm", height=0.6)
    for index, (name, total, ok) in enumerate(rows):
        axis.text(total + 0.12, index, f"{ok}/{total}", va="center", fontsize=9.5)
    axis.set_xlim(0, 9.4)
    axis.set_xlabel("Số ca")
    axis.grid(axis="y", visible=False)
    axis.set_title("Kết quả lượt 3 theo từng đề", loc="left", fontweight="bold")
    axis.legend(loc="lower right", frameon=False)
    save(figure, "hinh-2-theo-de.png")


# 4. Item level accuracy and review flags -------------------------------------------------------------------------
def chart_items_and_flags():
    figure, (top, bottom) = plt.subplots(2, 1, figsize=(9, 4.8), gridspec_kw={"hspace": 1.6})
    top.barh([""], [225], color=BLUE, height=0.5, label="Khớp điểm tham chiếu")
    top.barh([""], [9], left=[225], color=RED, height=0.5, label="Luna thấp hơn tham chiếu (0 mục cao hơn)")
    top.text(112, 0, "225 mục (96,2%)", ha="center", va="center", color="white", fontweight="bold")
    top.text(230, 0, "9", ha="center", va="center", color="white", fontweight="bold", fontsize=9)
    top.set_xlim(0, 234)
    top.set_title("Từng mục rubric, lượt 3 (234 mục)", loc="left", fontweight="bold", fontsize=10.5)
    top.legend(loc="upper center", bbox_to_anchor=(0.5, -0.5), ncol=2, frameon=False, fontsize=9)
    bottom.barh([""], [2], color=ORANGE, height=0.5, label="Có cờ cần xem lại")
    bottom.barh([""], [7], left=[2], color=GREY, height=0.5, label="Không có cờ")
    bottom.text(1, 0, "2", ha="center", va="center", color="white", fontweight="bold")
    bottom.text(5.5, 0, "7 mục không có cờ", ha="center", va="center", color="white", fontweight="bold")
    bottom.set_xlim(0, 9)
    bottom.set_title("Cờ có chỉ ra 9 mục lệch không?", loc="left", fontweight="bold", fontsize=10.5)
    bottom.legend(loc="upper center", bbox_to_anchor=(0.5, -0.5), ncol=2, frameon=False, fontsize=9)
    for axis in (top, bottom):
        axis.grid(visible=False)
        axis.set_yticks([])
    save(figure, "hinh-3-muc-rubric-va-co.png")


# 5. Repeated runs -----------------------------------------------------------------------------------------------
def chart_repeats():
    # name, reference, {prompt: (original round value, [three new runs])}
    cases = [
        ("swd-workshop-missing-fk", 9.25, {"Lượt 2": (9.25, [7.75, 7.25, 9.25]), "Lượt 3": (7.75, [9.25, 7.00, 9.25])}),
        ("swd-workshop-complete", 10.00, {"Lượt 2": (10.00, [10.00, 8.50, 10.00]), "Lượt 3": (9.25, [10.00, 10.00, 8.50])}),
        ("03-one-normal-flow", 3.75, {"Lượt 2": (3.50, [3.50, 3.50, 3.50]), "Lượt 3": (3.50, [3.75, 3.75, 3.50])}),
        ("05-off-topic", 0.50, {"Lượt 2": (0.75, [0.50, 0.75, 0.50]), "Lượt 3": (0.50, [0.50, 0.50, 0.50])}),
    ]
    figure, axes = plt.subplots(2, 2, figsize=(9.5, 7.2))
    offsets = [-0.27, -0.09, 0.09, 0.27]
    for axis, (name, reference, prompts) in zip(axes.flat, cases):
        values = [reference]
        for position, (prompt, (original, runs)) in enumerate(prompts.items()):
            color = BLUE if prompt == "Lượt 2" else TEAL
            axis.scatter([position + offsets[0]], [original], s=70, facecolor="white", edgecolor=color, linewidth=2, zorder=3)
            axis.scatter([position + o for o in offsets[1:]], runs, s=70, color=color, zorder=3)
            hits = sum(abs(run - reference) < 1e-9 for run in runs)
            axis.text(position, -0.17, f"khớp mốc {hits}/3", transform=axis.get_xaxis_transform(), ha="center",
                      va="top", fontsize=9, color=INK)
            values += [original, *runs]
        axis.axhline(reference, color=RED, linestyle="--", linewidth=1.4, zorder=2)
        low, high = min(values), max(values)
        pad = max((high - low) * 0.25, 0.25)
        axis.set_ylim(low - pad, high + pad)
        axis.set_xlim(-0.55, 1.55)
        axis.set_xticks([0, 1], ["Prompt lượt 2", "Prompt lượt 3"])
        axis.tick_params(axis="x", pad=2)
        axis.set_title(f"{name} (mốc {comma(reference, 2)})", loc="left", fontweight="bold", fontsize=10.5)
        comma_axis(axis)
        axis.grid(axis="x", visible=False)
    figure.suptitle("Chạy lặp: cùng một prompt, cùng một bài, điểm khác nhau", x=0.01, ha="left", fontweight="bold", y=0.99)
    from matplotlib.lines import Line2D

    handles = [
        Line2D([], [], marker="o", linestyle="", markerfacecolor="white", markeredgecolor=INK, markeredgewidth=2,
               markersize=8, label="Lượt gốc"),
        Line2D([], [], marker="o", linestyle="", color=INK, markersize=8, label="Ba lần chạy lặp (2026-11-23)"),
        Line2D([], [], color=RED, linestyle="--", linewidth=1.4, label="Mốc nhóm"),
    ]
    figure.legend(handles=handles, loc="lower center", ncol=3, frameon=False, fontsize=9.5)
    figure.text(0.01, 0.945, "Luna gpt-6-luna, mức suy luận medium. Xanh dương: prompt lượt 2. Xanh lục: prompt lượt 3.",
                fontsize=9, color=GREY)
    figure.tight_layout(rect=(0, 0.06, 1, 0.93), h_pad=2.6)
    save(figure, "hinh-6-chay-lap.png")


# 6. Tokens and time per round -----------------------------------------------------------------------------------
def chart_cost():
    rounds = ["Lượt 1", "Lượt 2", "Lượt 3"]
    tokens = [27042, 25517, 27800]
    seconds = [54.7, 54.0, 55.9]
    figure, (left, right) = plt.subplots(1, 2, figsize=(9, 3.9))
    bars = left.bar(rounds, tokens, color=BLUE, width=0.55)
    for bar, value in zip(bars, tokens):
        left.text(bar.get_x() + bar.get_width() / 2, value + 400, f"{value:,}".replace(",", "."), ha="center", fontsize=10)
    left.set_ylim(0, 33000)
    left.set_title("Token mỗi ca hoàn tất", loc="left", fontweight="bold", fontsize=10.5)
    comma_axis(left, thousands=True)
    bars = right.bar(rounds, seconds, color=TEAL, width=0.55)
    for bar, value in zip(bars, seconds):
        right.text(bar.get_x() + bar.get_width() / 2, value + 1, f"{value:.1f}".replace(".", ",") + " giây", ha="center",
                   fontsize=10)
    right.set_ylim(0, 66)
    right.set_title("Thời gian một lần chấm (trung vị)", loc="left", fontweight="bold", fontsize=10.5)
    for axis in (left, right):
        axis.grid(axis="x", visible=False)
    figure.suptitle("Chi phí và thời gian gần như không đổi giữa các lượt", x=0.01, ha="left", fontweight="bold", y=1.03)
    save(figure, "hinh-7-token-va-thoi-gian.png")


if __name__ == "__main__":
    chart_rounds()
    chart_deviation()
    chart_by_assignment()
    chart_items_and_flags()
    chart_repeats()
    chart_cost()
