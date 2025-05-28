import pytest
import re
from playwright.sync_api import expect

ROLES = [
    {
        "username": "admin1",
        "password": "P@ssw0rd!",
        "allowed_apis": ["/api/admin", "/api/user", "/api/guest"],
        "denied_apis": []
    },
    {
        "username": "user1",
        "password": "P@ssw0rd!",
        "allowed_apis": ["/api/user", "/api/guest"],
        "denied_apis": ["/api/admin"]
    },
    {
        "username": "guest1",
        "password": "P@ssw0rd!",
        "allowed_apis": ["/api/guest"],
        "denied_apis": ["/api/admin", "/api/user"]
    },
    {
        "username": "admin2",
        "password": "P@ssw0rd!",
        "allowed_apis": ["/api/admin", "/api/user", "/api/guest"],
        "denied_apis": []
    },
    {
        "username": "user2",
        "password": "P@ssw0rd!",
        "allowed_apis": ["/api/user", "/api/guest"],
        "denied_apis": ["/api/admin"]
    },
    {
        "username": "guest2",
        "password": "P@ssw0rd!",
        "allowed_apis": ["/api/guest"],
        "denied_apis": ["/api/admin", "/api/user"]
    }
]

def test_parallel_roles(browser_context):

    # For each role, open an isolated context+page
    # Note: browser_context.context.browser is the Browser instance
    contexts = [browser_context.context.browser.new_context() for _ in ROLES]
    pages = [ctx.new_page() for ctx in contexts]

    try:
        # Parallel login for each role/page
        for role, pg in zip(ROLES, pages):
            pg.goto("http://localhost:8081")
            pg.get_by_role("link", name="Login").click()
            pg.locator("#username").fill(role["username"])
            pg.locator("#password").fill(role["password"])
            pg.locator("#kc-login").click()
            expect(pg.get_by_text(f"Welcome, {role['username']}")).to_be_visible()

        # Parallel API permission checks
        for role, page in zip(ROLES, pages):
            result = page.locator("#testResult")
            # Allowed APIs
            for api in role["allowed_apis"]:
                page.get_by_text(f"Test {api.split('/')[-1].upper()} API").click()
                expect(result).to_have_value(re.compile(f"you can access {api}"))
            # Denied APIs
            for api in role["denied_apis"]:
                page.get_by_text(f"Test {api.split('/')[-1].upper()} API").click()
                expect(result).to_have_value(re.compile("Forbidden"))
    finally:
        # Close all contexts
        for ctx in contexts:
            ctx.close()
