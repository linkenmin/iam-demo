from playwright.sync_api import sync_playwright

def init_playwright():
    return sync_playwright().start()

def launch_browser(playwright, browser_name: str, headless: bool = True):
    browser_map = {
        "chromium": playwright.chromium,
        "firefox": playwright.firefox,
        "webkit": playwright.webkit
    }
    if browser_name not in browser_map:
        raise ValueError(f"Unsupported browser: {browser_name}")
    browser = browser_map[browser_name].launch(headless=headless)
    return browser