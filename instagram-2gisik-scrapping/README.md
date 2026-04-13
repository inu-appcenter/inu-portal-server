# Instagram Latest Post Scraper

This script opens Instagram in Chrome, reuses a persistent Chrome profile,
checks the most recent Instagram posts for a public account, and saves results as JSON.

## Current local environment

- Chrome version detected locally: `146.0.7680.178`
- Intended Python command: `python.exe`

## Files

- `insta_latest_post.py`: Selenium scraper
- `requirements.txt`: Python dependency list

## What it extracts

- recent post URLs
- caption text
- publish time
- like count
- main image URL
- image alt text

## Current behavior

- checks the most recent `2` posts by default
- writes the newest single post to `output/latest_post.json`
- appends only unseen posts to `output/scraped_posts.json`
- skips posts already stored in the history file based on `post_url`

## First-time setup

1. Install Python 3.11 or newer.
2. Install dependencies:

```powershell
python.exe -m pip install -r requirements.txt
```

3. Run the scraper:

```powershell
python.exe insta_latest_post.py
```

## Session persistence

The script stores a dedicated Chrome profile in:

```text
chrome-profile
```

On the first run, Instagram may redirect you to the login page. Log in manually
inside the opened Chrome window, then return to the terminal and press Enter.
Later runs will reuse the same profile directory and keep the session if
Instagram has not invalidated it.

## Output

By default the script writes:

```text
output/latest_post.json
```

and also maintains:

```text
output/scraped_posts.json
```

## Optional arguments

```powershell
python.exe insta_latest_post.py `
  --username incheondae_2gisik `
  --output output/latest_post.json `
  --history-output output/scraped_posts.json `
  --profile-dir chrome-profile `
  --recent-count 2 `
  --chromedriver-path C:\path\to\chromedriver.exe
```

## ChromeDriver note

The script first tries the default Selenium Chrome startup flow. If that fails,
install a ChromeDriver version that matches Chrome `146.0.7680.178` and either:

- put `chromedriver.exe` on `PATH`, or
- pass `--chromedriver-path`
