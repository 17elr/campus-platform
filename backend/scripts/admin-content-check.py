"""Read-only regression for live admin category filters; does not change item records."""

import json
import os
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

BASE = os.environ.get("API_BASE", "http://127.0.0.1:8088/api")


def api(path, token=None, body=None, success=True):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = Request(
        BASE + path,
        headers=headers,
        data=None if body is None else json.dumps(body).encode(),
    )
    try:
        with urlopen(request, timeout=15) as response:
            result = json.load(response)
    except HTTPError:
        if not success:
            return
        raise
    assert (result["code"] == 200) == success, result
    return result.get("data")


def query(token, **params):
    return api("/admin/items?" + urlencode(params), token)


admin = api(
    "/auth/login",
    body={
        "username": os.environ.get("ADMIN_USER", "admin"),
        "password": os.environ.get("ADMIN_PASSWORD", "admin123"),
    },
)["token"]
student = api(
    "/auth/login",
    body={
        "username": os.environ.get("STUDENT_USER", "demo1"),
        "password": os.environ.get("STUDENT_PASSWORD", "demo12345"),
    },
)["token"]
api("/admin/items?type=LOST", success=False)
api("/admin/items?type=LOST", student, success=False)
for invalid in [
    "type=INVALID",
    "status=INVALID",
    "keyword=" + "x" * 101,
    "location=" + "x" * 101,
]:
    api("/admin/items?" + invalid, admin, success=False)

for kind in ["LOST", "FOUND", "SECOND_HAND"]:
    first = query(admin, type=kind, size=50)
    assert first["counts"]["total"] == first["total"]
    records = list(first["records"])
    for page in range(2, (first["total"] + 49) // 50 + 1):
        records.extend(query(admin, type=kind, size=50, page=page)["records"])
    assert len(records) == first["total"]
    assert len({item["id"] for item in records}) == len(records)
    assert all(item["type"] == kind for item in records)
    for state in ["PENDING", "APPROVED", "HIDDEN", "WITHDRAWN", "DELETED"]:
        result = query(admin, type=kind, status=state, size=50)
        assert result["total"] == first["counts"][state]
        assert result["counts"] == first["counts"]
        assert all(
            item["type"] == kind and item["status"] == state
            for item in result["records"]
        )
    if records:
        item = records[0]
        result = query(
            admin,
            type=kind,
            keyword=item["title"],
            location=item["location"],
            status=item["status"],
        )
        assert any(row["id"] == item["id"] for row in result["records"])
    bounded = query(admin, type=kind, page=-1, size=0)
    assert bounded["page"] == 1 and bounded["size"] == 1
    empty = query(admin, type=kind, page=2147483647, size=100)
    assert empty["size"] == 50 and empty["records"] == []
    print(
        f"PASS {kind}: type/status/search/location/counts/pagination, {len(records)} records"
    )

print(
    "PASS authentication, role authorization and filter validation; no records modified"
)
