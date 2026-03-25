import sys
import os
import subprocess
import argparse

GEMINI_DIR = os.path.dirname(os.path.abspath(__file__))
CLASS_DB_FILE = os.path.join(GEMINI_DIR, 'class_db.txt')
CLASSPATH_FILE = os.path.join(GEMINI_DIR, 'classpath.txt')

def search_classes(query, exact=False):
    if not os.path.exists(CLASS_DB_FILE):
        print("Class DB not found. Run '.\\gradlew.bat geminiClassDb' first.")
        return

    query_lower = query.lower()
    matches = []
    with open(CLASS_DB_FILE, 'r', encoding='utf-8') as f:
        for line in f:
            c = line.strip()
            if exact:
                if c.split('.')[-1] == query or c == query:
                    matches.append(c)
            else:
                if query_lower in c.lower():
                    matches.append(c)

    print(f"Found {len(matches)} matches for '{query}':")
    for m in matches[:50]:
        print("  " + m)
    if len(matches) > 50:
        print(f"  ... and {len(matches) - 50} more. Refine your query.")

def inspect_class(class_name):
    if not os.path.exists(CLASSPATH_FILE):
        print("Classpath file not found. Run '.\\gradlew.bat geminiClassDb' first.")
        return

    with open(CLASSPATH_FILE, 'r', encoding='utf-8') as f:
        classpath = f.read().strip()

    # Determine if it's an exact class name or short name
    if '.' not in class_name:
        # try to resolve full class name
        with open(CLASS_DB_FILE, 'r', encoding='utf-8') as f:
            possible = [line.strip() for line in f if line.strip().endswith('.' + class_name)]
        if len(possible) == 1:
            class_name = possible[0]
            print(f"Resolved to {class_name}")
        elif len(possible) > 1:
            print(f"Ambiguous class name '{class_name}'. Found multiple:")
            for p in possible:
                print("  " + p)
            return
        else:
            print(f"Class '{class_name}' not found in DB.")
            # We don't return early here, as the user might want to inspect a class outside the DB
            # like a newly created class in the source directory

    cmd = ['javap', '-p', '-c', '-cp', classpath, class_name]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')
        print(result.stdout)
        if result.stderr:
            print("Errors:", result.stderr)
    except Exception as e:
        print("Error running javap:", e)

def main():
    parser = argparse.ArgumentParser(description="Minecraft/Fabric Class Lookup Tool for Gemini")
    parser.add_argument("action", choices=["search", "inspect", "update"], help="Action to perform: 'search' to find classes, 'inspect' to decompile/show signatures, 'update' to rebuild DB")
    parser.add_argument("query", nargs="?", help="Class name or search query")
    
    args = parser.parse_args()

    if args.action == "update":
        print("Running gradlew.bat geminiClassDb...")
        subprocess.run(["..\\gradlew.bat", "geminiClassDb"], cwd=GEMINI_DIR, shell=True)
    elif args.action == "search":
        if not args.query:
            print("Provide a query to search.")
            return
        search_classes(args.query)
    elif args.action == "inspect":
        if not args.query:
            print("Provide a class name to inspect.")
            return
        inspect_class(args.query)

if __name__ == "__main__":
    main()