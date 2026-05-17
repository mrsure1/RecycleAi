import pandas as pd
import glob

def inspect():
    files = glob.glob("*.xlsx")
    if not files:
        print("No xlsx files found.")
        return
    
    file_path = files[0]
    print(f"Reading file: {file_path}")
    
    try:
        df = pd.read_excel(file_path, header=0)
        # 컬럼 이름 변경 (임의로)
        out = {
            "columns": list(df.columns),
            "data": df.head(50).to_dict(orient="records")
        }
        import json
        with open("inspect_output.json", "w", encoding="utf-8") as f:
            json.dump(out, f, ensure_ascii=False, indent=2)
        print("Written to inspect_output.json")
    except Exception as e:
        print("Error reading excel:", e)

if __name__ == "__main__":
    inspect()
