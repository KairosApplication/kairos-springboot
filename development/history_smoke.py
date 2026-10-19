"""Exercise the principal API against the real kairos-data service and Redis."""
import argparse
import json
import urllib.error
import urllib.request

from smoke import API, Client


def write(client, path, method, body=None, expected=200):
    headers = {client.protection["headerName"]: client.protection["token"]}
    data = b"" if body is None else json.dumps(body).encode()
    if body is not None:
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(API + path, method=method, data=data, headers=headers)
    try:
        response = client.http.open(request, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        payload = response.read()
        if response.status != expected:
            raise RuntimeError(f"{method} {path}: expected {expected}, got {response.status}")
        return json.loads(payload) if payload else None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--offline", action="store_true", help="Run while the history microservice is stopped")
    parser.add_argument("--keep", action="store_true", help="Retain the fictitious record to verify later delivery")
    args = parser.parse_args()
    manager, stocker = Client(), Client()
    manager.login("manager@kairos.local")
    stocker.login("stocker@kairos.local")
    employee = next(row for row in manager.call("/employees/list") if row["position"].lower() == "stocker")
    inventory = manager.call("/inventories/list")[0]
    shelf = manager.call("/shelves/list")[0]
    product = manager.call("/products/list")[0]
    saved = write(stocker, "/restockings/registration", "POST", {
        "inventoryId": inventory["id"], "employeeId": employee["id"],
        "shelfId": shelf["id"], "productId": product["id"],
        "restockedQuantity": 10, "damagedQuantity": 1,
    }, 201)
    item_id = saved["id"]
    try:
        history = stocker.call("/restockings/week")
        assert any(row["restocking_id"] == item_id for row in history["items"])
        assert history["companyId"] > 0 and history["employeeId"] == employee["id"]
        manager.call(f"/restockings/week?employeeId={employee['id']}")
        customer = Client()
        customer.login("customer@kairos.local")
        customer.call("/restockings/week", expected=403)
        write(customer, "/auth/logout", "POST", expected=204)
        print("PASS: committed restocking and authenticated weekly history", flush=True)
        if args.offline:
            print("PASS: history API outage preserved the PostgreSQL record and weekly response", flush=True)
            return
        write(stocker, f"/restockings/update/{item_id}", "PATCH", {"restockedQuantity": 20})
        updated = stocker.call("/restockings/week")
        assert next(row for row in updated["items"] if row["restocking_id"] == item_id)["restocked_quantity"] == 20
        print("PASS: mutable history uses PostgreSQL instead of stale append-only cache", flush=True)
    finally:
        if not args.keep:
            write(stocker, f"/restockings/delete/{item_id}", "DELETE", expected=204)
            assert not any(row["restocking_id"] == item_id for row in stocker.call("/restockings/week")["items"])
        else:
            print(f"RETAINED_RESTOCKING_ID={item_id}", flush=True)
        write(manager, "/auth/logout", "POST", expected=204)
        write(stocker, "/auth/logout", "POST", expected=204)
    if not args.keep:
        print("PASS: deletion removed the event from public history", flush=True)


if __name__ == "__main__":
    main()
