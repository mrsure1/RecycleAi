"""PowerPoint 슬라이드 전환·도형 등장 애니메이션 (OOXML)."""

from __future__ import annotations

from pptx.oxml import parse_xml
from pptx.oxml.ns import nsdecls

# HTML .slide transition 0.7s
SLIDE_FADE_MS = 700

# HTML delay-N → 초
DELAY_STEP_MS = 100

# HTML animation 매핑
EFFECT_FADE_UP = "fade_up"      # fadeInUp
EFFECT_SLIDE_LEFT = "slide_left"  # slideInLeft
EFFECT_SCALE_IN = "scale_in"    # scaleIn


def delay_from_class(class_str: str) -> int:
    for part in (class_str or "").split():
        if part.startswith("delay-"):
            try:
                n = int(part.split("-", 1)[1])
                return n * DELAY_STEP_MS
            except ValueError:
                pass
    return 0


def set_slide_fade_transition(slide, duration_ms: int = SLIDE_FADE_MS) -> None:
    """슬라이드 전환: Fade (HTML opacity 0.7s 전환에 대응)."""
    xml = (
        f'<p:transition spd="med" advClick="1" xmlns:p="http://schemas.openformats.org/'
        f'presentationml/2006/main"><p:fade/></p:transition>'
    )
    el = slide._element
    old = el.findall("{http://schemas.openformats.org/presentationml/2006/main}transition")
    for t in old:
        el.remove(t)
    el.insert(-1, parse_xml(xml))


def _next_ctn_id(slide) -> int:
    ids = slide._element.xpath("//p:cTn/@id")
    if not ids:
        return 2
    return max(int(i) for i in ids) + 1


def add_entrance_animation(
    slide,
    shape_id: int,
    *,
    effect: str = EFFECT_FADE_UP,
    delay_ms: int = 0,
    duration_ms: int = 500,
    trigger: str = "afterPrevious",
) -> None:
    """도형 등장 애니메이션 (entr). 립싱크·시네마틱과 무관, bullet stagger용."""
    sld = slide._element
    child = sld.get_or_add_childTnLst()

    if effect == EFFECT_SLIDE_LEFT:
        preset_id, preset_subtype, filter_attr = "2", "0", "fade"
        # fly from left
    elif effect == EFFECT_SCALE_IN:
        preset_id, preset_subtype, filter_attr = "22", "0", "fade"
    else:
        preset_id, preset_subtype, filter_attr = "10", "0", "fade"

    dur = str(duration_ms)
    delay = str(delay_ms)
    cond = "indefinite" if trigger == "onClick" else "0"
    after_prev = "1" if trigger == "afterPrevious" else "0"

    c1, c2, c3, c4 = (_next_ctn_id(slide) + i for i in range(4))

    par_xml = f"""<p:par {nsdecls("p")}>
  <p:cTn id="{c1}" fill="hold">
    <p:stCondLst>
      <p:cond delay="{delay}"/>
    </p:stCondLst>
    <p:childTnLst>
      <p:par>
        <p:cTn id="{c2}" presetID="{preset_id}" presetClass="entr" presetSubtype="{preset_subtype}"
          fill="hold" nodeType="afterEffect">
          <p:stCondLst>
            <p:cond delay="{cond}" evt="onPrev"/>
          </p:stCondLst>
          <p:childTnLst>
            <p:par>
              <p:cTn id="{c3}" fill="hold">
                <p:stCondLst>
                  <p:cond delay="0"/>
                </p:stCondLst>
                <p:childTnLst>
                  <p:animEffect transition="in" filter="{filter_attr}">
                    <p:cBhvr>
                      <p:cTn id="{c4}" dur="{dur}" fill="hold"/>
                      <p:tgtEl>
                        <p:spTgt spid="{shape_id}"/>
                      </p:tgtEl>
                    </p:cBhvr>
                  </p:animEffect>
                </p:childTnLst>
              </p:cTn>
            </p:par>
          </p:childTnLst>
        </p:cTn>
      </p:par>
    </p:childTnLst>
  </p:cTn>
</p:par>"""
    child.append(parse_xml(par_xml))


def apply_dark_slide_bg(slide) -> None:
    """HTML --dark-bg 배경."""
    slide.background.fill.solid()
    slide.background.fill.fore_color.rgb = __import__(
        "pptx.dml.color", fromlist=["RGBColor"]
    ).RGBColor(0x0F, 0x17, 0x2A)
