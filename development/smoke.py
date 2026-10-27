"""HTTP checks against the isolated development API; uses only Python's standard library."""
import base64
import http.cookiejar
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

API = os.environ.get("TEST_API_URL", "http://127.0.0.1:8081/api/v1").rstrip("/")
PASSWORD = "Kairos-Dev-Only!123"  # Public local fixture, never a real credential.


class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.protection = None

    def call(self, path, method="GET", form=None, headers=None, expected=200, csrf=False):
        headers = dict(headers or {})
        if csrf:
            headers[self.protection["headerName"]] = self.protection["token"]
        data = urllib.parse.urlencode(form).encode() if form is not None else (b"" if method == "POST" else None)
        request = urllib.request.Request(API + path, data=data, headers=headers, method=method)
        try:
            response = self.http.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read()
            if response.status != expected:
                raise RuntimeError(f"{method} {path}: expected {expected}, got {response.status}")
            return json.loads(body) if body else None

    def login(self, email):
        self.protection = self.call("/auth/login")
        body = self.call("/auth/login", "POST", {"email": email, "password": PASSWORD}, csrf=True)
        self.protection = self.call("/auth/login")
        return body["accessToken"]

    def cookie(self, name):
        return next(cookie for cookie in self.jar if cookie.name == name)


def main():
    customer, other = Client(), Client()
    original = customer.login("customer@kairos.local")
    other.login("customer@kairos.local")
    assert customer.call("/auth/me")["roles"] == ["CUSTOMER"]
    customer.call("/employees/list", expected=403)
    customer.call("/auth/logout", "POST", expected=403)
    print("PASS: login, permissions and CSRF", flush=True)

    refresh = customer.cookie("KAIROS_REFRESH").value
    claims = json.loads(base64.urlsafe_b64decode(original.split(".")[1] + "=="))
    remaining = claims["exp"] - time.time() + 1
    if remaining > 60:
        raise RuntimeError("For the expiry check, set DEV_AUTH_ACCESS_TTL to 15s (maximum 59s)")
    time.sleep(max(0, remaining))
    assert customer.call("/auth/me")["email"] == "customer@kairos.local"
    renewed = customer.cookie("KAIROS_ACCESS").value
    assert renewed != original
    assert customer.cookie("KAIROS_REFRESH").value != refresh
    print("PASS: expired JWT renewed automatically by API on the same HTTP request", flush=True)

    customer.call("/auth/logout", "POST", csrf=True, expected=204)
    assert not any(cookie.name in ("KAIROS_ACCESS", "KAIROS_REFRESH") for cookie in customer.jar)
    Client().call("/auth/me", headers={"Authorization": "Bearer " + renewed}, expected=401)
    customer.call("/auth/me", expected=401)
    assert other.call("/auth/me")["email"] == "customer@kairos.local"
    print("PASS: immediate logout, cookies removed, other device preserved", flush=True)

    manager = Client()
    manager.login("manager@kairos.local")
    manager.call("/employees/list")
    manager.call("/auth/logout", "POST", csrf=True, expected=204)
    other.call("/auth/logout", "POST", csrf=True, expected=204)
    print("PASS: manager permissions; all development HTTP checks passed", flush=True)


if __name__ == "__main__":
    main()
