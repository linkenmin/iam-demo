import pytest
from utils.browser_utils import init_playwright, launch_browser

_playwright = None
browsers = {}

def pytest_addoption(parser):
    parser.addoption(
        "--browsers",
        action="store",
        default="chromium,firefox,webkit",
        help="Comma-separated list of browsers to test"
    )
    parser.addoption(
        "--headless",
        action="store_true",
        default=False,
        help="Run browsers in headless mode"
    )

def pytest_sessionstart(session):
    global _playwright, browsers
    _playwright = init_playwright()
    raw = session.config.getoption("--browsers")
    headless = session.config.getoption("--headless")
    for name in [b.strip() for b in raw.split(",") if b.strip()]:
        print(f"[SETUP] Launching browser {name}")
        browsers[name] = launch_browser(_playwright, name, headless=headless)

def pytest_sessionfinish(session, exitstatus):
    print('')
    global _playwright, browsers
    for name, br in browsers.items():
        print(f"[TEARDOWN] Quitting browser {name}")
        br.close()
    if _playwright:
        _playwright.stop()

def pytest_generate_tests(metafunc):
    if "browser_context" in metafunc.fixturenames:
        raw = metafunc.config.getoption("--browsers")
        names = [b.strip() for b in raw.split(",") if b.strip()]
        metafunc.parametrize(
            "browser_context",
            names,
            indirect=True,
            ids=names
        )

@pytest.fixture(scope="function")
def browser_context(request):
    browser = browsers[request.param]
    context = browser.new_context(viewport={"width": 1920, "height": 1080})
    page = context.new_page()
    yield page
    context.close()
