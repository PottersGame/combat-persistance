import sys
import os
import subprocess
import argparse
import re

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

def get_class_name(class_name):
    if '.' not in class_name:
        if not os.path.exists(CLASS_DB_FILE):
            return class_name
        with open(CLASS_DB_FILE, 'r', encoding='utf-8') as f:
            possible = [line.strip() for line in f if line.strip().endswith('.' + class_name)]
        if len(possible) == 1:
            return possible[0]
        elif len(possible) > 1:
            print(f"Ambiguous class name '{class_name}'. Found multiple:")
            for p in possible:
                print("  " + p)
            return None
    return class_name

def inspect_class(class_name):
    if not os.path.exists(CLASSPATH_FILE):
        print("Classpath file not found. Run '.\\gradlew.bat geminiClassDb' first.")
        return

    with open(CLASSPATH_FILE, 'r', encoding='utf-8') as f:
        classpath = f.read().strip()

    resolved_class = get_class_name(class_name)
    if not resolved_class: return
    if resolved_class != class_name:
        print(f"Resolved to {resolved_class}")

    cmd = ['javap', '-p', '-c', '-cp', classpath, resolved_class]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')
        print(result.stdout)
        if result.stderr:
            print("Errors:", result.stderr)
    except Exception as e:
        print("Error running javap:", e)

def generate_mixin_stub(class_name, method_query):
    if not os.path.exists(CLASSPATH_FILE):
        print("Classpath file not found. Run '.\\gradlew.bat geminiClassDb' first.")
        return

    with open(CLASSPATH_FILE, 'r', encoding='utf-8') as f:
        classpath = f.read().strip()

    resolved_class = get_class_name(class_name)
    if not resolved_class: return

    cmd = ['javap', '-p', '-s', '-cp', classpath, resolved_class]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')
        lines = result.stdout.split('\n')
    except Exception as e:
        print("Error running javap:", e)
        return

    found_methods = []
    current_method = None
    
    for i, line in enumerate(lines):
        line = line.strip()
        if method_query in line and "(" in line and ")" in line and not line.startswith("descriptor:"):
            current_method = line
            # Look ahead for descriptor
            for j in range(i+1, min(i+5, len(lines))):
                if lines[j].strip().startswith("descriptor:"):
                    desc = lines[j].strip().replace("descriptor: ", "").strip()
                    found_methods.append((current_method, desc))
                    break

    if not found_methods:
        print(f"Method '{method_query}' not found in {resolved_class}.")
        return

    print(f"--- Mixin Stub for {resolved_class} ---")
    print(f"@Mixin({resolved_class.split('.')[-1]}.class)")
    print(f"public abstract class {resolved_class.split('.')[-1]}Mixin {{\n")
    
    for method, desc in found_methods:
        method_name_match = re.search(r'([a-zA-Z0-9_$]+)\s*\(', method)
        if not method_name_match: continue
        method_name = method_name_match.group(1)
        
        return_type = method.split(method_name)[0].strip().split(" ")[-1]
        
        print(f"    @Inject(method = \"{method_name}{desc}\", at = @At(\"HEAD\"))")
        
        # Parse params from descriptor for callback info
        is_cancelable = "cancellable = true" if return_type != "void" else ""
        cancel_str = f", {is_cancelable}" if is_cancelable else ""
        
        cb_type = "CallbackInfoReturnable<" + return_type + ">" if return_type != "void" else "CallbackInfo"
        
        print(f"    private void on{method_name.capitalize()}({cb_type} ci) {{")
        print(f"        // TODO: Implement {method_name} mixin logic")
        print(f"    }}\n")
    
    print("}")

def main():
    parser = argparse.ArgumentParser(description="Minecraft/Fabric Class Lookup Tool for Gemini")
    parser.add_argument("action", choices=["search", "inspect", "update", "mixin"], help="Action to perform")
    parser.add_argument("query", nargs="?", help="Class name or search query")
    parser.add_argument("method", nargs="?", help="Method name (only for mixin action)")
    
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
    elif args.action == "mixin":
        if not args.query or not args.method:
            print("Provide a class name and method name. Example: python mc-lookup.py mixin PlayerEntity tick")
            return
        generate_mixin_stub(args.query, args.method)

if __name__ == "__main__":
    main()