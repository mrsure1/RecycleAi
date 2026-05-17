import sqlite3
import sys

def check_db():
    db_path = "wasteguide_dictionary.sqlite3"
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Get table names
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        print("Tables in DB:", [t[0] for t in tables])
        
        for t in tables:
            table_name = t[0]
            cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
            count = cursor.fetchone()[0]
            print(f"Table '{table_name}' has {count} rows.")
            
            # Print sample row to see schema
            cursor.execute(f"SELECT * FROM {table_name} LIMIT 1")
            sample = cursor.fetchone()
            columns = [desc[0] for desc in cursor.description]
            print(f"Columns for '{table_name}': {columns}")
            if sample:
                print(f"Sample data: {sample}")
            print("-" * 40)
            
        conn.close()
    except Exception as e:
        print("Error:", e)

if __name__ == "__main__":
    check_db()
