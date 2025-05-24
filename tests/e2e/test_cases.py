import pytest
import re
from playwright.sync_api import Page, Browser, expect

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
    }
]

def test_parallel_roles(browser: Browser):
    # For each role, open an isolated context+page
    contexts = [ browser.new_context() for _ in ROLES ]
    pages = [ ctx.new_page() for ctx in contexts ]

    try:
        # Log in to different pages in parallel
        for role, page in zip(ROLES, pages):
            page.goto("http://localhost:8081")
            page.get_by_role("link", name="Login").click()
            page.locator("#username").fill(role["username"])
            page.locator("#password").fill(role["password"])
            page.locator("#kc-login").click()
            # Verify successful login
            expect(page.get_by_text(f"Welcome, {role['username']}")).to_be_visible()

        # Test API permissions in parallel
        for role, page in zip(ROLES, pages):
            result = page.locator("#testResult")
            # Allowed APIs
            for api in role["allowed_apis"]:
                page.get_by_text(f"Test {api.split('/')[-1].upper()} API").click()
                expect(result).to_have_value(re.compile(f"access {api}"))
            # Denied APIs
            for api in role["denied_apis"]:
                page.get_by_text(f"Test {api.split('/')[-1].upper()} API").click()
                expect(result).to_have_value(re.compile("access-denied"))
    finally:
        # Close contexts
        for ctx in contexts:
            ctx.close()
