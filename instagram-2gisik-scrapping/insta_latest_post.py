from __future__ import annotations

import argparse
import html
import json
import os
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from selenium import webdriver
from selenium.common.exceptions import TimeoutException, WebDriverException
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.remote.webelement import WebElement
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

BASE_URL = "https://www.instagram.com"
DEFAULT_USERNAME = "incheondae_2gisik"
DEFAULT_OUTPUT = Path("output/latest_post.json")
DEFAULT_HISTORY_OUTPUT = Path("output/scraped_posts.json")
DEFAULT_PROFILE_DIR = Path("chrome-profile")
DEFAULT_RECENT_COUNT = 2
LIKE_TEXT = "\uc88b\uc544\uc694"
COUNT_SUFFIX = "\uac1c"
PROFILE_PHOTO_TEXT = "\ud504\ub85c\ud544 \uc0ac\uc9c4"
TEN_THOUSAND_SUFFIX = "\ub9cc"
THOUSAND_SUFFIX = "\ucc9c"
CLOSE_TEXTS = [
    "\ub2eb\uae30",
    "Close",
    "\ub098\uc911\uc5d0 \ud558\uae30",
    "Not now",
]
NOISE_LINE_PATTERNS = [
    r"^\u2022$",
    r"^\ub85c\uadf8\uc778$",
    r"^\uac00\uc785\ud558\uae30$",
    r"^\d+\s*(?:\ucd08|\ubd84|\uc2dc\uac04|\uc77c|\uc8fc|\uac1c\uc6d4|\ub144)(?:\s*\uc804)?$",
    r"^\d{4}\ub144.*$",
    r"^" + LIKE_TEXT + r".*$",
    r"^\ub313\uae00.*$",
    r"^\uac8c\uc2dc.*$",
    r"^\uc800\uc7a5$",
    r"^\uc635\uc158.*$",
    r"^\uba54\uc2dc\uc9c0 \ubcf4\ub0b4\uae30$",
    r"^\ud314\ub85c\uc789$",
    r"^\ud314\ub85c\uc6b0$",
    r"^\uc544\uc9c1 \ub313\uae00\uc774 \uc5c6\uc2b5\ub2c8\ub2e4\.$",
    r"^\ub313\uae00\uc744 \ub0a8\uaca8\ubcf4\uc138\uc694\.$",
    r"^" + LIKE_TEXT + r" \ub610\ub294 \ub313\uae00\uc744 \ub0a8\uae30\ub824\uba74 \ub85c\uadf8\uc778\..*$",
    r"^.+\ub2d8\uc758 \uac8c\uc2dc\ubb3c \ub354 \ubcf4\uae30$",
    r"^Meta$",
]


def remove_chrome_lock(profile_dir):
    lock_file = Path(profile_dir) / "SingletonLock"
    if lock_file.exists():
        try:
            os.remove(lock_file)
            print(f"Removed existing lock file: {lock_file}")
        except Exception as e:
            print(f"Failed to remove lock file: {e}")

def should_run_headless(explicit_headless: Optional[bool]) -> bool:
    # explicit_headless가 지정되지 않았다면 도커(리눅스) 환경에서는 무조건 True
    if explicit_headless is not None:
        return explicit_headless
    return True # 도커 환경 배포를 위해 기본값을 True로 강제


def build_driver(
    profile_dir: Path,
    chromedriver_path: Optional[str],
    headless: bool,
) -> webdriver.Chrome:
    options = Options()
    options.add_argument(f"--user-data-dir={profile_dir.resolve()}")
    options.add_argument("--profile-directory=Default")
    options.add_argument("--lang=ko-KR")
    options.add_argument("--remote-allow-origins=*")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-gpu")
    options.add_argument("--window-size=1440,1600")
    options.add_argument("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")

    if headless:
        options.add_argument("--headless=new")
    else:
        options.add_argument("--start-maximized")

    service = Service(executable_path=chromedriver_path) if chromedriver_path else Service()

    try:
        return webdriver.Chrome(service=service, options=options)
    except WebDriverException as exc:
        message = [
            "Could not start Chrome WebDriver.",
            f"Original error: {exc}",
            "Check if Google Chrome and ChromeDriver versions match.",
            "Make sure system dependencies (libnss3, etc.) are installed."
        ]
        raise SystemExit("\n".join(message)) from exc


def wait_for_page_ready(driver: webdriver.Chrome, timeout: int = 20) -> None:
    WebDriverWait(driver, timeout).until(
        lambda current_driver: current_driver.execute_script("return document.readyState") == "complete"
    )


def try_click(driver: webdriver.Chrome, element: WebElement) -> bool:
    try:
        element.click()
        return True
    except Exception:
        try:
            driver.execute_script("arguments[0].click();", element)
            return True
        except Exception:
            return False


def dismiss_login_modal_if_present(driver: webdriver.Chrome) -> bool:
    xpath_variants = [
        f"//button[.//*[name()='svg' and @aria-label='{CLOSE_TEXTS[0]}']]",
        f"//div[@role='button' and .//*[name()='svg' and @aria-label='{CLOSE_TEXTS[0]}']]",
        f"//button[normalize-space()='{CLOSE_TEXTS[0]}']",
        f"//*[@role='button' and normalize-space()='{CLOSE_TEXTS[0]}']",
        f"//button[normalize-space()='{CLOSE_TEXTS[1]}']",
        f"//*[@role='button' and normalize-space()='{CLOSE_TEXTS[1]}']",
        f"//button[normalize-space()='{CLOSE_TEXTS[2]}']",
        f"//*[@role='button' and normalize-space()='{CLOSE_TEXTS[2]}']",
        f"//button[normalize-space()='{CLOSE_TEXTS[3]}']",
        f"//*[@role='button' and normalize-space()='{CLOSE_TEXTS[3]}']",
    ]

    for xpath in xpath_variants:
        for element in driver.find_elements(By.XPATH, xpath):
            if element.is_displayed() and try_click(driver, element):
                time.sleep(0.5)
                return True

    return False


def is_login_page(driver: webdriver.Chrome) -> bool:
    current_url = driver.current_url.lower()
    return "/accounts/login" in current_url or "/challenge/" in current_url


def extract_latest_post_url_from_html(page_source: str, username: str) -> Optional[str]:
    urls = extract_recent_post_urls_from_html(page_source, username, limit=1)
    return urls[0] if urls else None


def extract_recent_post_urls_from_html(page_source: str, username: str, limit: Optional[int] = None) -> list[str]:
    decoded = html.unescape(page_source)
    pattern = re.compile(
        rf'href="(?P<href>(?:https://www\.instagram\.com)?/{re.escape(username)}/p/[^"/?#]+/?(?:\?[^"]*)?)"',
        re.IGNORECASE,
    )

    urls: list[str] = []
    seen: set[str] = set()
    for match in pattern.finditer(decoded):
        href = match.group("href")
        url = href if href.startswith("http") else f"{BASE_URL}{href}"
        if url in seen:
            continue
        seen.add(url)
        urls.append(url)
        if limit is not None and len(urls) >= limit:
            break

    return urls


def ensure_profile_page(driver: webdriver.Chrome, username: str, wait: WebDriverWait) -> None:
    profile_url = f"{BASE_URL}/{username}/"
    driver.get(profile_url)
    wait_for_page_ready(driver)
    time.sleep(2)

    for _ in range(3):
        if not dismiss_login_modal_if_present(driver):
            break

    if is_login_page(driver):
        print(
            "Instagram redirected to a login or challenge page.\n"
            "Log in manually in the opened Chrome window.\n"
            "When the profile page is visible, press Enter here to continue."
        )
        input()
        driver.get(profile_url)
        wait_for_page_ready(driver)

    def profile_has_posts(current_driver: webdriver.Chrome) -> bool:
        dismiss_login_modal_if_present(current_driver)
        return (
            len(extract_recent_post_urls_from_html(current_driver.page_source, username, limit=1)) > 0
            or len(current_driver.find_elements(By.XPATH, f"//a[contains(@href, '/{username}/p/')]")) > 0
        )

    wait.until(profile_has_posts)


def extract_recent_post_urls(driver: webdriver.Chrome, username: str, limit: int) -> list[str]:
    anchors = driver.find_elements(By.XPATH, f"//a[contains(@href, '/{username}/p/')]")
    urls: list[str] = []
    seen: set[str] = set()

    for anchor in anchors:
        href = anchor.get_attribute("href")
        if href and href not in seen:
            seen.add(href)
            urls.append(href)
            if len(urls) >= limit:
                return urls

    for fallback_url in extract_recent_post_urls_from_html(driver.page_source, username, limit=limit):
        if fallback_url not in seen:
            seen.add(fallback_url)
            urls.append(fallback_url)
            if len(urls) >= limit:
                break

    if urls:
        return urls

    raise RuntimeError("Could not find recent post links on the profile page.")


def extract_latest_post_url(driver: webdriver.Chrome, username: str) -> str:
    return extract_recent_post_urls(driver, username, limit=1)[0]


def first_non_empty_text(elements: list[WebElement]) -> Optional[str]:
    for element in elements:
        text = element.text.strip()
        if text:
            return text
    return None


def parse_compact_number(raw_value: str) -> Optional[int]:
    value = raw_value.replace(",", "").strip()
    if not value:
        return None

    multipliers = {TEN_THOUSAND_SUFFIX: 10_000, THOUSAND_SUFFIX: 1_000}
    for suffix, multiplier in multipliers.items():
        if value.endswith(suffix):
            return int(float(value[:-1]) * multiplier)

    try:
        return int(float(value))
    except ValueError:
        return None


def extract_like_count(driver: webdriver.Chrome) -> Optional[int]:
    candidates = driver.find_elements(
        By.XPATH,
        f"//*[contains(normalize-space(.), '{LIKE_TEXT}') and contains(normalize-space(.), '{COUNT_SUFFIX}')]",
    )

    for candidate in candidates:
        text = candidate.text.strip()
        pattern = rf"{LIKE_TEXT}\s*([0-9.,]+|[0-9.]+[{TEN_THOUSAND_SUFFIX}{THOUSAND_SUFFIX}])\s*{COUNT_SUFFIX}"
        match = re.search(pattern, text)
        if match:
            return parse_compact_number(match.group(1))

    return None


def extract_main_image(article: WebElement) -> tuple[Optional[str], Optional[str]]:
    images = article.find_elements(
        By.XPATH,
        f".//img[not(contains(@alt, '{PROFILE_PHOTO_TEXT}'))]",
    )

    for image in images:
        src = image.get_attribute("src")
        alt = image.get_attribute("alt")
        if src:
            return src, alt

    return None, None


def extract_attr_from_tag(tag: str, attr_name: str) -> Optional[str]:
    match = re.search(rf'{attr_name}="([^"]*)"', tag, re.IGNORECASE)
    if match:
        return html.unescape(match.group(1))
    return None


def compact_whitespace(value: Optional[str]) -> Optional[str]:
    if value is None:
        return None

    normalized = html.unescape(value)
    normalized = normalized.replace("\r", "")
    normalized = re.sub(r"[ \t\f\v]+", " ", normalized)
    normalized = re.sub(r" *\n *", "\n", normalized)
    normalized = normalized.strip()
    return normalized or None


def extract_text_from_element(element: WebElement) -> Optional[str]:
    candidates = [
        element.text,
        element.get_attribute("innerText"),
        element.get_attribute("textContent"),
    ]

    inner_html = element.get_attribute("innerHTML")
    if inner_html:
        candidates.append(strip_tags(inner_html))

    for candidate in candidates:
        normalized = compact_whitespace(candidate)
        if normalized:
            return normalized

    return None


def extract_caption_from_dom(driver: webdriver.Chrome, article: WebElement, username: str) -> Optional[str]:
    elements = article.find_elements(By.CSS_SELECTOR, "h1")
    if not elements:
        elements = driver.find_elements(By.CSS_SELECTOR, "article h1, main h1, h1._ap3a")

    candidates: list[str] = []
    for element in elements:
        text = extract_text_from_element(element)
        if not text or text == username:
            continue
        candidates.append(text)

    if not candidates:
        return None

    return max(candidates, key=len)


def is_noise_caption_line(line: str, username: str) -> bool:
    if not line:
        return True

    if line == username:
        return True

    for pattern in NOISE_LINE_PATTERNS:
        if re.match(pattern, line):
            return True

    return False


def clean_caption_candidate(raw_text: Optional[str], username: str) -> Optional[str]:
    normalized = compact_whitespace(raw_text)
    if not normalized:
        return None

    lines = [line.strip() for line in normalized.split("\n")]
    lines = [line for line in lines if line]
    if not lines:
        return None

    kept: list[str] = []
    started = False

    for line in lines:
        if is_noise_caption_line(line, username):
            if started:
                break
            continue

        started = True
        kept.append(line)

    if not kept:
        return None

    cleaned = compact_whitespace("\n".join(kept))
    if cleaned == username:
        return None

    return cleaned


def extract_caption_from_container_js(driver: webdriver.Chrome, article: WebElement, username: str) -> Optional[str]:
    script = """
const root = arguments[0];
const selectors = [
  '._a9zr',
  'ul._a9z6 li',
  'li._a9zj',
  'h1._ap3a',
  'h1'
];

const results = [];
for (const selector of selectors) {
  for (const el of root.querySelectorAll(selector)) {
    results.push({
      selector,
      text: el.innerText || '',
      content: el.textContent || '',
      html: el.innerHTML || ''
    });
  }
}
return results;
"""

    try:
        raw_candidates = driver.execute_script(script, article) or []
    except Exception:
        return None

    candidates: list[str] = []
    for item in raw_candidates:
        if not isinstance(item, dict):
            continue

        for key in ("text", "content", "html"):
            value = item.get(key)
            if key == "html" and value:
                value = strip_tags(value)

            cleaned = clean_caption_candidate(value, username)
            if cleaned:
                candidates.append(cleaned)

    if not candidates:
        return None

    return max(candidates, key=len)


def extract_caption_via_js(driver: webdriver.Chrome, username: str) -> Optional[str]:
    script = """
const selectors = [
  'article h1[dir="auto"]',
  'main h1[dir="auto"]',
  'article h1',
  'main h1',
  'h1._ap3a',
  'h1'
];

const nodes = [];
for (const selector of selectors) {
  for (const el of document.querySelectorAll(selector)) {
    nodes.push({
      text: el.innerText || '',
      content: el.textContent || '',
      html: el.innerHTML || '',
      className: el.className || ''
    });
  }
}
return nodes;
"""

    try:
        raw_candidates = driver.execute_script(script) or []
    except Exception:
        return None

    candidates: list[str] = []
    for item in raw_candidates:
        if not isinstance(item, dict):
            continue

        for key in ("text", "content", "html"):
            value = item.get(key)
            if key == "html" and value:
                value = strip_tags(value)

            normalized = clean_caption_candidate(value, username) if key != "html" else clean_caption_candidate(strip_tags(value), username)
            if not normalized or normalized == username:
                continue

            candidates.append(normalized)

    if not candidates:
        return None

    return max(candidates, key=len)


def extract_caption_from_body_text_js(driver: webdriver.Chrome, username: str) -> Optional[str]:
    try:
        body_text = driver.execute_script("return document.body ? document.body.innerText : '';")
    except Exception:
        return None

    normalized = compact_whitespace(body_text)
    if not normalized:
        return None

    lines = [line.strip() for line in normalized.split("\n")]
    lines = [line for line in lines if line]
    if not lines:
        return None

    candidates: list[str] = []
    for index, line in enumerate(lines):
        if line != username:
            continue

        window = "\n".join(lines[index + 1 : index + 20])
        cleaned = clean_caption_candidate(window, username)
        if cleaned:
            candidates.append(cleaned)

    if not candidates:
        cleaned = clean_caption_candidate(normalized, username)
        if cleaned:
            candidates.append(cleaned)

    if not candidates:
        return None

    return max(candidates, key=len)


def apply_body_text_caption_fallback(driver: webdriver.Chrome, details: dict[str, object], username: str) -> dict[str, object]:
    caption = extract_caption_from_body_text_js(driver, username)
    if caption:
        details["caption"] = caption
    return details


def strip_tags(fragment: str) -> str:
    normalized = re.sub(r"<br\s*/?>", "\n", fragment, flags=re.IGNORECASE)
    normalized = re.sub(r"</?(div|p|li|ul|ol|span|h1|h2|time)[^>]*>", " ", normalized, flags=re.IGNORECASE)
    normalized = re.sub(r"<[^>]+>", "", normalized)
    normalized = html.unescape(normalized)
    normalized = re.sub(r"[ \t\r\f\v]+", " ", normalized)
    normalized = re.sub(r"\n\s*", "\n", normalized)
    return normalized.strip()


def extract_meta_tag_content(page_source: str, key: str) -> Optional[str]:
    decoded = html.unescape(page_source)
    for tag_match in re.finditer(r"<meta\b[^>]*>", decoded, re.IGNORECASE):
        tag = tag_match.group(0)
        tag_key = extract_attr_from_tag(tag, "property") or extract_attr_from_tag(tag, "name")
        content = extract_attr_from_tag(tag, "content")
        if tag_key == key and content:
            return compact_whitespace(content)
    return None


def extract_caption_from_meta(page_source: str) -> Optional[str]:
    for key in ("og:description", "description"):
        content = extract_meta_tag_content(page_source, key)
        if not content:
            continue

        quoted_match = re.search(r':\s*["\u201c](.+?)["\u201d]\s*$', content)
        if quoted_match:
            return compact_whitespace(quoted_match.group(1))

    return None


def extract_like_count_from_meta(page_source: str) -> Optional[int]:
    meta_text = extract_meta_tag_content(page_source, "og:description") or extract_meta_tag_content(page_source, "description")
    if not meta_text:
        return None

    english_match = re.search(r"([0-9][0-9,\.]*)\s+likes?\b", meta_text, re.IGNORECASE)
    if english_match:
        return parse_compact_number(english_match.group(1))

    korean_match = re.search(
        rf"{LIKE_TEXT}\s*([0-9.,]+|[0-9.]+[{TEN_THOUSAND_SUFFIX}{THOUSAND_SUFFIX}])\s*{COUNT_SUFFIX}",
        meta_text,
    )
    if korean_match:
        return parse_compact_number(korean_match.group(1))

    return None


def extract_json_ld_objects(page_source: str) -> list[object]:
    decoded = html.unescape(page_source)
    objects: list[object] = []

    for match in re.finditer(
        r'<script[^>]*type="application/ld\+json"[^>]*>(.*?)</script>',
        decoded,
        re.IGNORECASE | re.DOTALL,
    ):
        raw_json = match.group(1).strip()
        if not raw_json:
            continue

        try:
            objects.append(json.loads(raw_json))
        except json.JSONDecodeError:
            continue

    return objects


def find_first_json_value(node: object, keys: tuple[str, ...]) -> Optional[object]:
    if isinstance(node, dict):
        for key in keys:
            value = node.get(key)
            if value not in (None, "", [], {}):
                return value

        for value in node.values():
            nested = find_first_json_value(value, keys)
            if nested not in (None, "", [], {}):
                return nested

    if isinstance(node, list):
        for item in node:
            nested = find_first_json_value(item, keys)
            if nested not in (None, "", [], {}):
                return nested

    return None


def extract_caption_from_json_ld(page_source: str) -> Optional[str]:
    for obj in extract_json_ld_objects(page_source):
        value = find_first_json_value(obj, ("caption", "articleBody", "description"))
        if isinstance(value, str):
            return compact_whitespace(value)
    return None


def extract_like_count_from_json_ld(page_source: str) -> Optional[int]:
    for obj in extract_json_ld_objects(page_source):
        value = find_first_json_value(obj, ("userInteractionCount",))
        if isinstance(value, (int, float)):
            return int(value)
        if isinstance(value, str):
            parsed = parse_compact_number(value)
            if parsed is not None:
                return parsed
    return None


def extract_post_details_from_html(page_source: str, username: str, post_url: str) -> dict[str, object]:
    decoded = html.unescape(page_source)

    caption = None
    caption_match = re.search(r"<h1[^>]*>(.*?)</h1>", decoded, re.IGNORECASE | re.DOTALL)
    if caption_match:
        caption = compact_whitespace(strip_tags(caption_match.group(1)))

    if caption is None:
        caption = extract_caption_from_meta(page_source)

    if caption is None:
        caption = extract_caption_from_json_ld(page_source)

    published_at_iso = None
    published_at_display = None
    time_match = re.search(
        r'<time[^>]*datetime="([^"]+)"[^>]*>(.*?)</time>',
        decoded,
        re.IGNORECASE | re.DOTALL,
    )
    if time_match:
        published_at_iso = time_match.group(1)
        published_at_display = strip_tags(time_match.group(2))

    like_count = None
    like_match = re.search(
        rf"{LIKE_TEXT}\s*([0-9.,]+|[0-9.]+[{TEN_THOUSAND_SUFFIX}{THOUSAND_SUFFIX}])\s*{COUNT_SUFFIX}",
        strip_tags(decoded),
    )
    if like_match:
        like_count = parse_compact_number(like_match.group(1))

    if like_count is None:
        like_count = extract_like_count_from_meta(page_source)

    if like_count is None:
        like_count = extract_like_count_from_json_ld(page_source)

    image_url = None
    image_alt = None
    for tag_match in re.finditer(r"<img\b[^>]*>", decoded, re.IGNORECASE):
        tag = tag_match.group(0)
        src = extract_attr_from_tag(tag, "src")
        alt = extract_attr_from_tag(tag, "alt")
        if src and PROFILE_PHOTO_TEXT not in (alt or ""):
            image_url = src
            image_alt = alt
            break

    return {
        "username": username,
        "post_url": post_url,
        "caption": caption,
        "published_at_iso": published_at_iso,
        "published_at_display": published_at_display,
        "like_count": like_count,
        "image_url": image_url,
        "image_alt": image_alt,
        "scraped_at_utc": datetime.now(timezone.utc).isoformat(),
    }


def extract_post_details(
    driver: webdriver.Chrome,
    username: str,
    post_url: str,
    wait: WebDriverWait,
) -> dict[str, object]:
    driver.get(post_url)
    wait_for_page_ready(driver)
    time.sleep(2)

    for _ in range(3):
        if not dismiss_login_modal_if_present(driver):
            break

    try:
        article = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.TAG_NAME, "article")))
    except TimeoutException:
        return apply_body_text_caption_fallback(
            driver,
            extract_post_details_from_html(driver.page_source, username, post_url),
            username,
        )

    html_fallback = extract_post_details_from_html(driver.page_source, username, post_url)

    caption = extract_caption_from_dom(driver, article, username)
    if caption is None:
        caption = extract_caption_from_container_js(driver, article, username)
    if caption is None:
        caption = extract_caption_via_js(driver, username)
    if caption is None:
        caption = extract_caption_from_body_text_js(driver, username)
    caption = compact_whitespace(caption) or html_fallback["caption"]

    time_elements = article.find_elements(By.XPATH, ".//time[@datetime]")
    published_at_iso = None
    published_at_display = None
    if time_elements:
        published_at_iso = time_elements[0].get_attribute("datetime")
        published_at_display = time_elements[0].text.strip() or time_elements[0].get_attribute("title")
    published_at_iso = published_at_iso or html_fallback["published_at_iso"]
    published_at_display = compact_whitespace(published_at_display) or html_fallback["published_at_display"]

    image_url, image_alt = extract_main_image(article)
    like_count = extract_like_count(driver)
    image_url = image_url or html_fallback["image_url"]
    image_alt = compact_whitespace(image_alt) or html_fallback["image_alt"]
    like_count = like_count if like_count is not None else html_fallback["like_count"]

    return {
        "username": username,
        "post_url": post_url,
        "caption": caption,
        "published_at_iso": published_at_iso,
        "published_at_display": published_at_display,
        "like_count": like_count,
        "image_url": image_url,
        "image_alt": image_alt,
        "scraped_at_utc": datetime.now(timezone.utc).isoformat(),
    }


def write_debug_snapshot(driver: webdriver.Chrome, username: str, output_path: Path) -> None:
    try:
        body_text = driver.execute_script("return document.body ? document.body.innerText : '';")
    except Exception:
        body_text = None

    try:
        article_html = driver.execute_script(
            "const article = document.querySelector('article'); return article ? article.outerHTML : null;"
        )
    except Exception:
        article_html = None

    debug_payload = {
        "username": username,
        "url": driver.current_url,
        "body_text": body_text,
        "article_html": article_html,
    }

    debug_path = output_path.parent / "debug_caption.json"
    debug_path.write_text(json.dumps(debug_payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Caption debug written to: {debug_path.resolve()}")


def read_post_history(history_path: Path) -> list[dict[str, object]]:
    if not history_path.exists():
        return []

    try:
        raw_value = json.loads(history_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return []

    if not isinstance(raw_value, list):
        return []

    posts: list[dict[str, object]] = []
    for item in raw_value:
        if isinstance(item, dict):
            posts.append(item)
    return posts


def merge_new_posts(
    existing_posts: list[dict[str, object]],
    recent_posts: list[dict[str, object]],
) -> tuple[list[dict[str, object]], list[dict[str, object]]]:
    existing_urls = {
        post.get("post_url")
        for post in existing_posts
        if isinstance(post.get("post_url"), str)
    }

    new_posts: list[dict[str, object]] = []
    for post in recent_posts:
        post_url = post.get("post_url")
        if not isinstance(post_url, str) or post_url in existing_urls:
            continue
        existing_urls.add(post_url)
        new_posts.append(post)

    merged_posts = new_posts + existing_posts
    return merged_posts, new_posts


def write_output(output_path: Path, payload: dict[str, object]) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def write_history_output(output_path: Path, payload: list[dict[str, object]]) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scrape the latest Instagram post from a public profile using Selenium.",
    )
    parser.add_argument("--username", default=DEFAULT_USERNAME, help="Instagram username to scrape.")
    parser.add_argument(
        "--output",
        default=str(DEFAULT_OUTPUT),
        help="Path to the single latest-post JSON output file.",
    )
    parser.add_argument(
        "--history-output",
        default=str(DEFAULT_HISTORY_OUTPUT),
        help="Path to the cumulative scraped-post history JSON file.",
    )
    parser.add_argument(
        "--profile-dir",
        default=str(DEFAULT_PROFILE_DIR),
        help="Chrome user data directory used to keep the login session.",
    )
    parser.add_argument(
        "--chromedriver-path",
        default=None,
        help="Optional path to a local chromedriver executable.",
    )
    parser.add_argument(
        "--pause-after-login",
        type=float,
        default=2.0,
        help="Extra seconds to wait after loading a page.",
    )
    parser.add_argument(
        "--recent-count",
        type=int,
        default=DEFAULT_RECENT_COUNT,
        help="How many of the most recent posts to check on each run.",
    )
    parser.add_argument(
        "--headless",
        action=argparse.BooleanOptionalAction,
        default=None,
        help="Run Chrome in headless mode. Defaults to true on non-Windows environments.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    output_path = Path(args.output)
    history_output_path = Path(args.history_output)
    profile_dir = Path(args.profile_dir)
    recent_count = max(args.recent_count, 1)
    headless = should_run_headless(args.headless)

    remove_chrome_lock(profile_dir)

    print(
        json.dumps(
            {
                "headless": headless,
                "profile_dir": str(profile_dir.resolve()),
                "history_output": str(history_output_path.resolve()),
            },
            ensure_ascii=False,
        )
    )

    driver = build_driver(
        profile_dir=profile_dir,
        chromedriver_path=args.chromedriver_path,
        headless=headless,
    )
    wait = WebDriverWait(driver, 25)

    try:
        ensure_profile_page(driver, args.username, wait)
        time.sleep(max(args.pause_after_login, 0))

        recent_post_urls = extract_recent_post_urls(driver, args.username, limit=recent_count)
        recent_posts: list[dict[str, object]] = []

        for post_url in recent_post_urls:
            post_details = extract_post_details(driver, args.username, post_url, wait)
            if post_details.get("caption") is None:
                write_debug_snapshot(driver, args.username, output_path)
            recent_posts.append(post_details)

        latest_post = recent_posts[0]
        existing_posts = read_post_history(history_output_path)
        merged_posts, new_posts = merge_new_posts(existing_posts, recent_posts)

        write_output(output_path, latest_post)
        write_history_output(history_output_path, merged_posts)

        summary = {
            "username": args.username,
            "checked_count": len(recent_posts),
            "new_count": len(new_posts),
            "latest_post_output": str(output_path.resolve()),
            "history_output": str(history_output_path.resolve()),
            "checked_posts": recent_posts,
            "new_posts": new_posts,
        }

        print(json.dumps(summary, ensure_ascii=False, indent=2))
        print(f"\nSaved latest post JSON to: {output_path.resolve()}")
        print(f"Saved cumulative history JSON to: {history_output_path.resolve()}")
        return 0
    except TimeoutException as exc:
        print("Timed out while waiting for Instagram page elements.", file=sys.stderr)
        print(str(exc), file=sys.stderr)
        return 1
    except RuntimeError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    finally:
        driver.quit()


if __name__ == "__main__":
    raise SystemExit(main())
