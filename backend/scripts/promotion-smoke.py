"""Real local API/MySQL regression. Creates isolated records and cleans up only those IDs.
Run with DB_PASSWORD set and backend listening on API_BASE (default localhost:8080).
"""

import concurrent.futures, json, os, subprocess, uuid
from urllib.request import Request, urlopen

BASE = os.environ.get("API_BASE", "http://127.0.0.1:8080/api")
created = []


def sql(statement):
    env = dict(os.environ, MYSQL_PWD=os.environ.get("DB_PASSWORD", "123456"))
    return subprocess.check_output(
        ["mysql", "-u", "root", "-D", "campus_platform", "-N", "-B", "-e", statement],
        env=env,
        text=True,
    ).strip()


def api(path, body=None, token=None, method=None, ok=True):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request(
        BASE + path,
        data=None if body is None else json.dumps(body).encode(),
        headers=headers,
        method=method or ("POST" if body is not None else "GET"),
    )
    with urlopen(req) as response:
        r = json.load(response)
    if ok:
        assert r["code"] == 200, r
    else:
        assert r["code"] != 200, r
    return r.get("data")


def login(name, password):
    return api("/auth/login", {"username": name, "password": password})["token"]


def create_item(token, title):
    item = api(
        "/items",
        {
            "title": title,
            "description": "promotion regression",
            "type": "SECOND_HAND",
            "category": "promo-test",
            "location": "test",
            "contact": "test",
        },
        token,
    )
    created.append(item["id"])
    return item["id"]


def order(token, item, type="TOP_24H"):
    return api(
        "/promotions/orders",
        {"itemId": item, "promotionType": type, "amount": 0.01},
        token,
    )


try:
    admin = login("admin", "admin123")
    seller = login("demo1", "demo12345")
    other = login("demo2", "demo12345")
    baseline = api("/admin/promotions/stats", token=admin)
    item = create_item(seller, "promotion-smoke-" + uuid.uuid4().hex)
    api("/admin/promotions/stats", token=seller, ok=False)
    api(
        "/promotions/orders",
        {"itemId": item, "promotionType": "TOP_24H"},
        other,
        ok=False,
    )
    api(
        "/promotions/orders", {"itemId": item, "promotionType": "BAD"}, seller, ok=False
    )
    first = order(seller, item)
    assert float(first["amount"]) == 1.99
    assert order(seller, item)["id"] == first["id"]
    assert api("/admin/promotions/stats", token=admin)["revenue"] == baseline["revenue"]
    api("/promotions/orders/" + str(first["id"]) + "/pay", {}, other, ok=False)

    def pay():
        try:
            api("/promotions/orders/" + str(first["id"]) + "/pay", {}, seller)
            return True
        except AssertionError:
            return False

    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        assert sum(pool.map(lambda _: pay(), range(5))) == 1
    assert (
        int(sql("SELECT COUNT(*) FROM item_promotion WHERE item_id=" + str(item))) == 1
    )
    assert float(api("/admin/promotions/stats", token=admin)["revenue"]) == round(
        float(baseline["revenue"]) + 1.99, 2
    )
    assert api("/items?size=1")["records"][0]["id"] == item
    assert "TOP_24H" in api("/items/" + str(item))["promotions"]
    api(
        "/promotions/orders",
        {"itemId": item, "promotionType": "TOP_24H"},
        seller,
        ok=False,
    )
    api("/items/" + str(item) + "/withdraw", {}, seller, method="PUT")
    assert api("/items?keyword=promotion-smoke")["total"] == 0
    api(
        "/admin/items/" + str(item) + "/status",
        {"status": "APPROVED"},
        admin,
        method="PUT",
    )
    sql(
        "UPDATE item_promotion SET end_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id="
        + str(first["id"])
    )
    assert api("/items/" + str(item))["promotions"] == []
    assert (
        api("/promotions/orders?size=1", token=seller)["records"][0]["status"]
        == "EXPIRED"
    )
    renewal = order(seller, item)
    assert renewal["id"] != first["id"]
    api("/promotions/orders/" + str(renewal["id"]) + "/cancel", {}, seller)
    api("/promotions/orders/" + str(renewal["id"]) + "/pay", {}, seller, ok=False)
    for kind, price, scene in [
        ("FEATURED_3D", 4.99, "scene=home"),
        ("CATEGORY_7D", 9.90, "category=promo-test"),
    ]:
        o = order(seller, item, kind)
        assert float(o["amount"]) == price
        api("/promotions/orders/" + str(o["id"]) + "/pay", {}, seller)
        assert api("/items?size=1&" + scene)["records"][0]["id"] == item
    hidden = order(seller, item)
    api("/items/" + str(item) + "/withdraw", {}, seller, method="PUT")
    api("/promotions/orders/" + str(hidden["id"]) + "/pay", {}, seller, ok=False)
    for path, token in [
        ("/items", None),
        ("/items/mine", seller),
        ("/trade/orders?role=buy", seller),
        ("/admin/users", admin),
        ("/admin/items/pending", admin),
        ("/admin/trade/orders", admin),
        ("/promotions/orders", seller),
        ("/admin/promotions/orders", admin),
    ]:
        sep = "&" if "?" in path else "?"
        data = api(path + sep + "page=-1&size=0", token=token)
        assert data["page"] == 1 and data["size"] == 1
        data = api(path + sep + "page=2147483647&size=100", token=token)
        assert data["size"] == 50 and data["records"] == []
    filtered = api("/items?type=SECOND_HAND&size=50")
    assert filtered["total"] == int(
        sql("SELECT COUNT(*) FROM item WHERE status='APPROVED' AND type='SECOND_HAND'")
    )
    p1 = api("/items?size=10")["records"]
    p2 = api("/items?page=2&size=10")["records"]
    assert not {x["id"] for x in p1} & {x["id"] for x in p2}
    print(
        "PASS: permissions, server prices, pending reuse, concurrent pay, revenue, ranking, expiry, renewals, cancellation, hidden-item payment, filtered counts, pagination boundaries"
    )
finally:
    if created:
        ids = ",".join(map(str, created))
        sql(
            "DELETE FROM item_promotion WHERE item_id IN ("
            + ids
            + "); DELETE FROM item WHERE id IN ("
            + ids
            + ");"
        )
