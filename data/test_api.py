import requests
import json
import urllib.parse

def test_api():
    # Read key from local.properties
    api_key = ""
    with open("../local.properties", "r", encoding="utf-8") as f:
        for line in f:
            if line.startswith("WASTE_OPEN_API_KEY"):
                api_key = line.split("=")[1].strip().strip('"')
                break

    if not api_key:
        print("Error: API Key not found in local.properties")
        return

    # Base URL for the API
    url = "https://apis.data.go.kr/1741000/household_waste_info/info"
    
    # query params
    params = {
        "ServiceKey": api_key,
        "pageNo": "1",
        "numOfRows": "10",
        "type": "JSON",
        # Let's try with a sample sigungu_code if required, or just omit to see if we get a response
    }

    try:
        print(f"Testing connection with ServiceKey: {api_key[:10]}...")
        # Since requests URL-encodes params, and Korean open API keys are often already encoded,
        # it might cause an INVALID_KEY error. We construct the URL manually to be safe.
        query_string = f"?ServiceKey={api_key}&pageNo=1&numOfRows=10&type=JSON"
        full_url = url + query_string
        
        response = requests.get(full_url, timeout=10)
        print(f"HTTP Status Code: {response.status_code}")
        
        if response.status_code == 200:
            try:
                data = response.json()
                items = data.get("response", {}).get("body", {}).get("items", {}).get("item", [])
                if items:
                    first_item = items[0]
                    print("--- API Field List ---")
                    for key, value in first_item.items():
                        print(f"{key}: {value}")
                else:
                    print("No items found.")
            except Exception as e:
                print("Received response, but it's not valid JSON:", e)
        else:
            print("Failed with status code:", response.status_code)
            
    except Exception as e:
        print("Network Error:", str(e))

if __name__ == "__main__":
    test_api()
