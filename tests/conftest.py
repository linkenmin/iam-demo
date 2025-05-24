import pytest
from utils.browser_utils import init_playwright, launch_browser

def pytest_addoption(parser):
    parser.addoption("--browsers", action="store", default="chromium,firefox,webkit",
                     help="Comma-separated list of browsers")
    parser.addoption("--headless", action="store_true", default=False,
                     help="Run in headless mode")

@pytest.fixture(scope="session")
def playwright():
    pw = init_playwright()
    yield pw
    pw.stop()

@pytest.fixture(scope="session")
def browser_instances(playwright, request):
    names = [b.strip() for b in request.config.getoption("--browsers").split(",") if b.strip()]
    instances = { name: launch_browser(playwright, name, headless=request.config.getoption("--headless"))
                  for name in names }
    yield instances
    for br in instances.values():
        br.close()

# Parameterize for "browser" parameter
def pytest_generate_tests(metafunc):
    if "browser" in metafunc.fixturenames:
        names = [b.strip() for b in metafunc.config.getoption("--browsers").split(",") if b.strip()]
        metafunc.parametrize("browser", names, indirect=True, ids=names)

# Select corresponding instance based on parameter
@pytest.fixture
def browser(browser_instances, request):
    return browser_instances[request.param]
