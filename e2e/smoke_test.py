"""End-to-end smoke test against a running stack (docker compose up, or start-all.ps1).

Expects a fresh database with the seed data. Uses only the standard library.

    BASE_URL=http://localhost:3000 MAILPIT_URL=http://localhost:8025 python e2e/smoke_test.py
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
MAILPIT = os.environ.get("MAILPIT_URL", "http://localhost:8025").rstrip("/")
ADMIN = {"email": os.environ.get("ADMIN_EMAIL", "admin@library.local"),
         "password": os.environ.get("ADMIN_PASSWORD", "admin123")}

token = None
checks = 0


def call(method, path, body=None, expect=None, auth=True, headers=None, base=BASE):
    global checks
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if auth and token:
        h["Authorization"] = "Bearer " + token
    h.update(headers or {})
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            status, text = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        status, text = e.code, e.read().decode()
    if expect is not None:
        checks += 1
        if status != expect:
            fail(f"{method} {path}: expected {expect}, got {status}: {text[:300]}")
        print(f"  ok  {method} {path} -> {status}")
    return json.loads(text) if text else None


def check(condition, message):
    global checks
    checks += 1
    if not condition:
        fail(message)
    print(f"  ok  {message}")


def fail(message):
    print(f"FAIL {message}")
    sys.exit(1)


def wait_until_ready(timeout=240):
    """Services start at different speeds; wait until login works and every service has seeded its data."""
    global token
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        try:
            token = call("POST", "/api/auth/login", ADMIN, auth=False)["token"]
            members = call("GET", "/api/members")
            books = call("GET", "/api/books")
            call("GET", "/api/loans/pending")
            call("GET", "/api/notifications")
            if isinstance(members, list) and len(members) >= 3 and isinstance(books, list) and len(books) >= 8:
                print(f"stack ready ({BASE})")
                return
            last = "seed data not loaded yet"
        except Exception as e:  # connection refused, 502/503 from the proxy, ...
            last = repr(e)
        time.sleep(3)
    fail(f"stack not ready after {timeout}s: {last}")


def main():
    global token
    wait_until_ready()

    print("security")
    saved, token = token, None
    call("GET", "/api/members", expect=401)
    call("POST", "/api/auth/login", {"email": ADMIN["email"], "password": "wrong"}, expect=401)
    token = saved
    call("POST", "/api/books/any-id/checkout", expect=403)
    call("POST", "/api/loans/any-id/reminded", expect=403)

    print("catalog")
    members = call("GET", "/api/members", expect=200)
    ayse = next(m for m in members if m["email"] == "ayse.yilmaz@example.com")
    books = call("GET", "/api/books", expect=200)
    kar = next(b for b in books if b["title"] == "Kar")
    donusum = next(b for b in books if b["title"] == "Dönüşüm")
    author = call("GET", f"/api/authors/{kar['author']['id']}", expect=200)
    check(author["author"]["name"] == "Orhan Pamuk" and len(author["books"]) == 3,
          "author page lists the author's 3 books")
    check(len(call("GET", "/api/books?q=pamuk", expect=200)) == 3, "search by author name finds 3 books")

    print("lending")
    loan = call("POST", "/api/loans", {"memberId": ayse["id"], "bookId": kar["id"], "days": 14}, expect=201)
    check(loan["status"] == "PENDING", "new loan waits for approval")
    call("POST", "/api/loans", {"memberId": ayse["id"], "bookId": kar["id"]}, expect=409)
    loan = call("POST", f"/api/loans/{loan['id']}/approve", expect=200)
    check(loan["status"] == "ACTIVE" and loan["approvedBy"]["name"], "approved loan is active and records the staff member")
    check(call("GET", f"/api/books/{kar['id']}", expect=200)["availableCopies"] == kar["availableCopies"] - 1,
          "approval takes one copy")
    holders = call("GET", f"/api/loans/books/{kar['id']}/active", expect=200)
    check([h["memberId"] for h in holders] == [ayse["id"]], "book shows who holds it")

    print("return")
    loan2 = call("POST", "/api/loans", {"memberId": ayse["id"], "bookId": donusum["id"], "days": 7}, expect=201)
    call("POST", f"/api/loans/{loan2['id']}/approve", expect=200)
    call("POST", f"/api/loans/{loan2['id']}/return-request", expect=200)
    loan2 = call("POST", f"/api/loans/{loan2['id']}/return-approve", expect=200)
    check(loan2["status"] == "RETURNED", "return approval closes the loan")
    check(call("GET", f"/api/books/{donusum['id']}", expect=200)["availableCopies"] == donusum["availableCopies"],
          "return puts the copy back")

    print("reading profile")
    stats = call("GET", f"/api/loans/members/{ayse['id']}/stats", expect=200)
    check(stats["totalBorrowed"] == 2 and stats["currentlyBorrowed"] == 1, "counts borrowed and current books")
    check(stats["totalPages"] == kar["pageCount"] + donusum["pageCount"], "sums the pages")
    check(stats["byCategory"][0] == {"key": "Roman", "name": "Roman", "count": 2,
                                     "pages": kar["pageCount"] + donusum["pageCount"]}, "groups by category")

    print("notifications")
    log = call("POST", f"/api/notifications/loans/{loan['id']}/reminder", expect=200)
    check(log["success"], "reminder e-mail was sent")
    check(call("GET", f"/api/loans/{loan['id']}", expect=200)["reminderCount"] == 1, "loan records the reminder")
    mails = call("GET", "/api/v1/messages", base=MAILPIT, auth=False)["messages"]
    check(any(m["To"][0]["Address"] == ayse["email"] and kar["title"] in m["Subject"] for m in mails),
          "e-mail arrived in Mailpit")

    print("staff roles")
    call("POST", "/api/auth/users", {"fullName": "Test Personel", "email": "staff@library.local",
                                     "password": "secret1", "role": "STAFF"}, expect=201)
    token = call("POST", "/api/auth/login", {"email": "staff@library.local", "password": "secret1"},
                 expect=200, auth=False)["token"]
    new_user = {"fullName": "X", "email": "x@library.local", "password": "secret1", "role": "STAFF"}
    call("POST", "/api/auth/users", new_user, expect=403)
    call("POST", "/api/auth/users", new_user, expect=403, headers={"X-User-Role": "ADMIN"})

    print(f"PASSED: {checks} checks")


if __name__ == "__main__":
    main()
